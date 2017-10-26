/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
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
class EtcdReadWriteTransaction extends AbstractDOMStoreTransaction<TransactionIdentifier>
        implements DOMStoreReadWriteTransaction {

    EtcdReadWriteTransaction(TransactionIdentifier identifier, boolean debug) {
        super(identifier, debug);
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(YangInstanceIdentifier path) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void delete(YangInstanceIdentifier path) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public DOMStoreThreePhaseCommitCohort ready() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void close() {
    }

}
