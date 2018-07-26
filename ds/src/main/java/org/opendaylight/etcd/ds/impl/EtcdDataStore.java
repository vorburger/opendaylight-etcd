/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.coreos.jetcd.Client;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
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

    public EtcdDataStore(String name, LogicalDatastoreType type, ExecutorService dataChangeListenerExecutor,
            int maxDataChangeListenerQueueSize, Client client, boolean debugTransactions) {
        super(name + "-" + prefixChar(type), dataChangeListenerExecutor, maxDataChangeListenerQueueSize,
                debugTransactions);

        kv = new EtcdKV(getIdentifier(), client, prefix(type));

        watcher = new EtcdWatcher(getIdentifier(), client, prefix(type), 0, watchEvent -> {
            // TODO actually update DataTree on watch notifications
        });
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
    // TODO make private; this is only temporarily public, for EtcdConcurrentDataBrokerTestCustomizer
    // The idea is to make this private again later, and have it called automatically whenever we're behind etcd
    public void initialLoad() throws EtcdException {
        // TODO requires https://git.opendaylight.org/gerrit/#/c/73482/ which makes dataTree protected instead of private
        DataTreeModification mod = dataTree.takeSnapshot().newModification();
        kv.readAllInto(mod);
        mod.ready();

        try {
            dataTree.validate(mod);
        } catch (DataValidationFailedException e) {
            throw new EtcdException("Initial load from etcd into (supposedly) empty data store caused "
                    + "unexpected DataValidationFailedException", e);
        }
        DataTreeCandidate candidate = dataTree.prepare(mod);
        dataTree.commit(candidate);

        // also requires https://git.opendaylight.org/gerrit/#/c/73482/ which adds a protected notifyListeners to InMemoryDOMDataStore
        notifyListeners(candidate);

        LOG.info("{} initialLoad: DataTreeModification={}, DataTreeCandidate={}", getIdentifier(), mod, candidate);
    }

    @Override
    // requires https://git.opendaylight.org/gerrit/#/c/73208/ :-( or figure out if we can hook into InMemoryDOMDataStore via a commit cohort?!
    protected synchronized void commit(DataTreeCandidate candidate) {
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
            sendToEtcd(candidate, candidate.getRootPath(), candidate.getRootNode()).toCompletableFuture().get(1,
                    SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // TODO This is ugly, wrong, and just temporary.. but how to correctly return problems from here?!
            throw new RuntimeException(e);
        }
        super.commit(candidate);
    }

    private CompletionStage<Void> sendToEtcd(DataTreeCandidate candidate, YangInstanceIdentifier base,
            DataTreeCandidateNode node) {
        List<CompletableFuture<Void>> futures = new ArrayList<>(1 + node.getChildNodes().size());

        YangInstanceIdentifier newBase = candidate.getRootNode().equals(node) ? base : base.node(node.getIdentifier());

        ModificationType modificationType = node.getModificationType();
        switch (modificationType) {
            case WRITE:
                add(futures, kv.put(newBase,
                        node.getDataAfter().orElseThrow(() -> new IllegalArgumentException("No dataAfter: " + node))));
                break;

            case DELETE:
                add(futures, kv.delete(newBase));
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
            add(futures, sendToEtcd(candidate, newBase, childNode));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    private void add(List<CompletableFuture<Void>> futures, CompletionStage<?> future) {
        futures.add(future.thenApply(whatever -> (Void)null).toCompletableFuture());
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
}
