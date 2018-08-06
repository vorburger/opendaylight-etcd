/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.watch.WatchEvent;
import java.util.concurrent.ExecutorService;
import javax.annotation.PostConstruct;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.etcd.ds.impl.EtcdKV.EtcdTxn;
import org.opendaylight.etcd.utils.KeyValues;
import org.opendaylight.infrautils.utils.function.CheckedConsumer;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
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
public class EtcdDataStore extends InMemoryDOMDataStore {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdDataStore.class);

    public static final byte CONFIGURATION_PREFIX = 67; // 'C'
    public static final byte OPERATIONAL_PREFIX   = 79; // 'O'

    private final EtcdKV kv;
    private final EtcdWatcher watcher;

    private boolean hasSchemaContext = false;
    private boolean isInitialized = false;

    @SuppressWarnings("checkstyle:MissingSwitchDefault") // conflicts with http://errorprone.info/bugpattern/UnnecessaryDefaultInEnumSwitch
    public EtcdDataStore(String name, LogicalDatastoreType type, ExecutorService dataChangeListenerExecutor,
            int maxDataChangeListenerQueueSize, Client client, boolean debugTransactions) {
        super(name + "-" + prefixChar(type), dataChangeListenerExecutor, maxDataChangeListenerQueueSize,
                debugTransactions);

        kv = new EtcdKV(getIdentifier(), client, prefix(type));

        watcher = new EtcdWatcher(getIdentifier(), client, prefix(type), 0, events -> {
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
        });
    }

    @PostConstruct
    public void init() throws Exception {
        if (!hasSchemaContext) {
            throw new IllegalStateException("onGlobalContextUpdated() not yet called");
        }
        this.isInitialized = true;
        try {
            initialLoad();
        } catch (EtcdException e) {
            this.isInitialized = false;
            throw e;
        }
    }

    @Override
    public synchronized void onGlobalContextUpdated(SchemaContext ctx) {
        super.onGlobalContextUpdated(ctx);
        this.hasSchemaContext = true;
    }

    private static char prefixChar(LogicalDatastoreType type) {
        return (char) prefix(type);
    }

    private static byte prefix(LogicalDatastoreType type) {
        return type.equals(LogicalDatastoreType.CONFIGURATION) ? CONFIGURATION_PREFIX : OPERATIONAL_PREFIX;
    }

    @Override
    public void close() {
        watcher.close();
        kv.close();
    }

    /**
     * On start-up, read back current persistent state from etcd as initial DataTree content.
     * @throws EtcdException if loading failed
     */
    private void initialLoad() throws EtcdException {
        apply(mod -> kv.readAllInto(mod));
    }

    private void apply(CheckedConsumer<DataTreeModification, EtcdException> function) throws EtcdException {
        isInitialized();
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
            kvTx.commit();
        } catch (EtcdException | IllegalArgumentException e) {
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

    private void sendToEtcd(EtcdTxn kvTx, DataTreeCandidate candidate, YangInstanceIdentifier base,
            DataTreeCandidateNode node) throws IllegalArgumentException, EtcdException {
        YangInstanceIdentifier newBase = candidate.getRootNode().equals(node) ? base : base.node(node.getIdentifier());

        ModificationType modificationType = node.getModificationType();
        switch (modificationType) {
            case WRITE:
                kvTx.put(newBase,
                        node.getDataAfter().orElseThrow(() -> new IllegalArgumentException("No dataAfter: " + node)));
                break;

            case DELETE:
                kvTx.delete(newBase);
                break;

            case UNMODIFIED:
            case SUBTREE_MODIFIED:
                // ignore
                break;

            // TODO TDD and take remaining modificationType/s correctly into account...

            default:
                // return completedExceptionally(new UnsupportedOperationException(modificationType.name()));
                throw new UnsupportedOperationException(modificationType.name());
        }

        for (DataTreeCandidateNode childNode : node.getChildNodes()) {
            sendToEtcd(kvTx, candidate, newBase, childNode);
        }
    }

    private void print(String indent, DataTreeCandidateNode node) {
        LOG.info("{}{} DataTreeCandidateNode: modificationType={}, PathArgument identifier={}",
                indent, getIdentifier(), node.getModificationType(), getIdentifierAsString(node));
        // LOG.info("{}  dataBefore= {}", indent, node.getDataBefore());
        LOG.info("{}{}   dataAfter = {}", indent, getIdentifier(), node.getDataAfter());

        for (DataTreeCandidateNode childNode : node.getChildNodes()) {
            print(indent + "    ", childNode);
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
        if (!isInitialized) {
            throw new IllegalStateException("@PostConstruct init() not yet called");
        }
    }
}
