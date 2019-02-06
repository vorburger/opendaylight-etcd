/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.watch.WatchEvent;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.etcd.ds.impl.EtcdYangKV.EtcdTxn;
import org.opendaylight.etcd.ds.inmemory.copypaste.InMemoryDOMDataStore;
import org.opendaylight.etcd.utils.ByteSequences;
import org.opendaylight.etcd.utils.KeyValues;
import org.opendaylight.infrautils.utils.function.CheckedConsumer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ODL DOM Data Store implementation based on etcd.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
public class EtcdDataStore extends InMemoryDOMDataStore implements CheckedConsumer<List<WatchEvent>, EtcdException> {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdDataStore.class);

    public static final ByteSequence BASE_PREFIX = ByteSequences.fromBytes(); // empty (currently; could change)
    public static final ByteSequence CONFIGURATION_PREFIX = ByteSequences.append(BASE_PREFIX, (byte) 'C'); // 67
    public static final ByteSequence OPERATIONAL_PREFIX   = ByteSequences.append(BASE_PREFIX, (byte) 'O'); // 79

    // This flag could later be dynamic instead of fixed hard-coded, to optionally
    // support very fast reads with eventual instead of strong consistency.  We could do this either
    // globally and have different data stores (and, ultimately DataBroker), or per transaction.
    private final boolean isStronglyConsistent = true;

    private final EtcdYangKV kv;
    private final KV kvClient;
    private final RevAwaiter revAwaiter;

    private boolean hasSchemaContext = false;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    public EtcdDataStore(String name, LogicalDatastoreType type, ExecutorService dataChangeListenerExecutor,
            int maxDataChangeListenerQueueSize, Client client, boolean debugTransactions, RevAwaiter revAwaiter) {
        // TODO InMemoryDOMDataStore creates the DataTree with a hard-coded DataTreeConfiguration, instead of by type
        super(name + "-" + prefixChar(type), dataChangeListenerExecutor, maxDataChangeListenerQueueSize,
                debugTransactions);

        this.revAwaiter = revAwaiter;
        this.kvClient = client.getKVClient();

        kv = new EtcdYangKV(getIdentifier(), client, prefix(type));
    }

    @Override
    @SuppressWarnings("checkstyle:MissingSwitchDefault") // conflicts with http://errorprone.info/bugpattern/UnnecessaryDefaultInEnumSwitch
    public void accept(List<WatchEvent> events) throws EtcdException {
        isInitialized();
        apply(mod -> {
            for (WatchEvent watchEvent : events) {
                switch (watchEvent.getEventType()) {
                    case PUT:
                        KeyValue keyValue = watchEvent.getKeyValue();
                        kv.applyPut(mod, keyValue.getKey(), keyValue.getValue());
                        break;

                    case DELETE:
                        kv.applyDelete(mod, watchEvent.getKeyValue().getKey());
                        break;

                    case UNRECOGNIZED:
                        LOG.warn("{} UNRECOGNIZED watch event: {}", getIdentifier(),
                                KeyValues.toStringable(watchEvent.getKeyValue()));
                        break;

                    // no default, as error-prone has error checking for non-exhaustive switches
                }
            }
        });
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        isInitialized();
        await();
        return super.newReadOnlyTransaction();
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        isInitialized();
        await();
        return super.newReadWriteTransaction();
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        isInitialized();
        // TODO discuss with others if this makes sense, or could indeed be optimized?
        // It could be tempting to NOT await() for a newWriteOnlyTransaction(),
        // but the data validation on commit needs up-to-date data, so we do.
        await();
        return super.newWriteOnlyTransaction();
    }

    private void await() {
        if (isStronglyConsistent) {
            long expectedRev;
            try {
                expectedRev = EtcdServerUtils.getServerRevision(kvClient);
            } catch (EtcdException e) {
                throw new EtcdRuntimeException(getIdentifier() + " await getServerRevision() failed", e);
            }

            try {
                // TODO remove the *10 here again?  It was because of a doubt on early testing.
                revAwaiter.await(expectedRev, Duration.ofMillis(EtcdYangKV.TIMEOUT_MS * 10));
            } catch (TimeoutException | InterruptedException e) {
                throw new EtcdRuntimeException(getIdentifier() + " await revision failed: " + expectedRev, e);
            }
        }
    }

    @Override
    public synchronized void onGlobalContextUpdated(SchemaContext ctx) {
        super.onGlobalContextUpdated(ctx);
        this.hasSchemaContext = true;
    }

    public void init(long rev) throws Exception {
        if (!hasSchemaContext) {
            throw new IllegalStateException("onGlobalContextUpdated() not yet called");
        }
        initialLoad(rev);
        this.isInitialized.set(true);
    }

    @Override
    public void close() {
        kv.close();
    }

    private static char prefixChar(LogicalDatastoreType type) {
        return (char) prefix(type).getBytes()[0];
    }

    private static ByteSequence prefix(LogicalDatastoreType type) {
        return type.equals(LogicalDatastoreType.CONFIGURATION) ? CONFIGURATION_PREFIX : OPERATIONAL_PREFIX;
    }

    /**
     * On start-up, read back current persistent state from etcd as initial DataTree content.
     * @param rev the etcd Revision number to load
     * @throws EtcdException if loading failed
     */
    private void initialLoad(long rev) throws EtcdException {
        apply(mod -> kv.readAllInto(rev, mod));
    }

    private void apply(CheckedConsumer<DataTreeModification, EtcdException> function) throws EtcdException {
        // TODO requires https://git.opendaylight.org/gerrit/#/c/73482/ which makes dataTree protected instead of private
        DataTreeModification mod = dataTree.takeSnapshot().newModification();
        function.accept(mod);
        mod.ready();

        try {
            dataTree.validate(mod);
        } catch (DataValidationFailedException e) {
            throw new EtcdException("Applying changes watched from etcd to DS caused DataValidationFailedException", e);
        }
        DataTreeCandidate candidate = dataTree.prepare(mod);
        dataTree.commit(candidate);

        // also requires https://git.opendaylight.org/gerrit/#/c/73217/ which adds a protected notifyListeners to InMemoryDOMDataStore
        notifyListeners(candidate);

        LOG.info("{} applied DataTreeModification={}, DataTreeCandidate={}", getIdentifier(), mod, candidate);
    }

    @Override
    // requires https://git.opendaylight.org/gerrit/#/c/73208/ :-( or figure out if we can hook into InMemoryDOMDataStore via a commit cohort?!
    protected synchronized void commit(DataTreeCandidate candidate) {
        isInitialized();
        if (!candidate.getRootPath().equals(YangInstanceIdentifier.EMPTY)) {
            LOG.error("DataTreeCandidate: YangInstanceIdentifier path={}", candidate.getRootPath());
            throw new IllegalArgumentException("I've not learnt how to deal with DataTreeCandidate where "
                    + "root path != YangInstanceIdentifier.EMPTY yet - will you teach me? ;)");
        }

        LOG.info("{} commit: DataTreeCandidate={}", getIdentifier(), candidate);
        print("", candidate.getRootNode());

        // TODO make InMemoryDOMDataStore.commit(DataTreeCandidate) return ListenableFuture<Void> instead of void,
        // and then InMemoryDOMStoreThreePhaseCommitCohort.commit() return store.commit(candidate) instead of SUCCESS,
        // and then do this:
//        sendToEtcd(candidate.getRootNode()).thenRun(() -> super.commit(candidate)).exceptionally(throwable -> {
//            LOG.error("sendToEtcd failed", throwable);
//            return null;
//        });
        // but for now let's throw the entire nice async-ity over board and just do:
        try {
            EtcdTxn kvTx = kv.newTransaction();
            sendToEtcd(kvTx, candidate, candidate.getRootPath(), candidate.getRootNode());
            kvTx.commit().toCompletableFuture().get();
        } catch (EtcdException | IllegalArgumentException | InterruptedException | ExecutionException e) {
            // TODO This is ugly, wrong, and just temporary.. but see above, how to better return problems here?
            throw new RuntimeException(e);
        }

        // We do *NOT* super.commit(candidate), because we don't want to immediately/directly apply changes,
        // because we let the watcher listener do this - for ourselves here where we initiated the change, as well as
        // on all other remote nodes which listen to changes.  It seems tempting to optimize and for our own
        // node just apply ourselves, instead of going through the listener, but this causes
        // IllegalStateException: "Store tree ... and candidate base ... differ.", because we would apply
        // everything twice, because the watcher sends us back our own operations;
        // see also https://github.com/coreos/jetcd/issues/343.
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault") // http://errorprone.info/bugpattern/UnnecessaryDefaultInEnumSwitch
    private void sendToEtcd(EtcdTxn kvTx, DataTreeCandidate candidate, YangInstanceIdentifier base,
            DataTreeCandidateNode node) throws IllegalArgumentException, EtcdException {
        YangInstanceIdentifier newBase = candidate.getRootNode().equals(node) ? base : base.node(node.getIdentifier());

        ModificationType modificationType = node.getModificationType();
        switch (modificationType) {
            case WRITE:
            case APPEARED: // TODO is it right to treat APPEARED like WRITE here?
                kvTx.put(newBase,
                        node.getDataAfter().orElseThrow(() -> new IllegalArgumentException("No dataAfter: " + node)));
                break;

            case DELETE:
            case DISAPPEARED: // TODO is it right to treat DISAPPEARED like DELETE here?
                kvTx.delete(newBase);
                break;

            case UNMODIFIED:
            case SUBTREE_MODIFIED:
                // ignore
                break;

            // no default, as error-prone protects us, see http://errorprone.info/bugpattern/UnnecessaryDefaultInEnumSwitch
        }

        for (DataTreeCandidateNode childNode : node.getChildNodes()) {
            sendToEtcd(kvTx, candidate, newBase, childNode);
        }
    }

    private void print(String indent, DataTreeCandidateNode node) {
        if (LOG.isInfoEnabled()) {
            LOG.info("{}{} DataTreeCandidateNode: modificationType={}, PathArgument identifier={}",
                    indent, getIdentifier(), node.getModificationType(), getIdentifierAsString(node));
            // LOG.info("{}  dataBefore= {}", indent, node.getDataBefore());
            LOG.info("{}{}   dataAfter = {}", indent, getIdentifier(), node.getDataAfter());

            for (DataTreeCandidateNode childNode : node.getChildNodes()) {
                print(indent + "    ", childNode);
            }
        }
    }

    private static String getIdentifierAsString(DataTreeCandidateNode node) {
        try {
            return node.getIdentifier().toString();
        } catch (IllegalStateException e) {
            // just debugging code; not intended for production
            return "-ROOT-";
        }
    }

    private void isInitialized() {
        if (!isInitialized.get()) {
            throw new IllegalStateException("init() not yet called");
        }
    }
}
