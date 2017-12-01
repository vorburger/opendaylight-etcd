/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.AbstractDOMStoreTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Implementation of Read/Write transaction backed by etcd.
 *
 * @author Michael Vorburger.ch
 */
// intentionally just .impl package-local, for now
class EtcdReadWriteTransaction
        extends AbstractDOMStoreTransaction<TransactionIdentifier>
        implements DOMStoreReadWriteTransaction {

    private final Etcd etcd;

    EtcdReadWriteTransaction(EtcdDataStore etcdDataStore, TransactionIdentifier identifier, boolean debug) {
        super(identifier, debug);
        this.etcd = new Etcd(etcdDataStore.getKV(), etcdDataStore.getPrefix());
    }

    @Override
    public void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        // TODO implementation transactions! ;)
        etcd.put(path, data);
    }

    @Override
    public void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        // TODO implementation transactions! ;)
        // TODO a merge() is NOT just a put() ..
        etcd.put(path, data);
    }

    @Override
    public void delete(YangInstanceIdentifier path) {
        // TODO implementation transactions! ;)
        etcd.delete(path);
    }


    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(YangInstanceIdentifier path) {
        // TODO implementation transactions! ;)
        return etcd.read(path);
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        return new SimpleDOMStoreThreePhaseCommitCohort();
    }

    @Override
    public void close() {
    }

    private static class SimpleDOMStoreThreePhaseCommitCohort implements DOMStoreThreePhaseCommitCohort {
        // TODO This is just to get started, and obviously will need more work, later...

        @Override
        public ListenableFuture<Boolean> canCommit() {
            return immediateFuture(Boolean.TRUE);
        }

        @Override
        public ListenableFuture<Void> preCommit() {
            return immediateFuture(null);
        }

        @Override
        public ListenableFuture<Void> commit() {
            return immediateFuture(null);
        }

        @Override
        public ListenableFuture<Void> abort() {
            return immediateFuture(null);
        }

    }

}
