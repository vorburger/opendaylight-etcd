/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static java.util.Objects.requireNonNull;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Watch;
import java.util.concurrent.ExecutorService;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

/**
 * ODL DOM Data Store implementation based on etcd.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
public class EtcdDataStore extends InMemoryDOMDataStore {

    private static final byte CONFIGURATION_PREFIX = 67; // 'C'
    private static final byte OPERATIONAL_PREFIX   = 79; // 'O'

    private final KV etcdKV;
    private final Watch etcdWatch;
    private final Etcd etcd;

    public EtcdDataStore(LogicalDatastoreType type, ExecutorService dataChangeListenerExecutor,
            int maxDataChangeListenerQueueSize, Client client, boolean debugTransactions) {
        super(type.name(), type, dataChangeListenerExecutor, maxDataChangeListenerQueueSize, debugTransactions);

        byte prefix = type.equals(LogicalDatastoreType.CONFIGURATION) ? CONFIGURATION_PREFIX : OPERATIONAL_PREFIX;
        this.etcdKV = requireNonNull(client, "client").getKVClient();
        this.etcd = new Etcd(etcdKV , prefix);

        this.etcdWatch = client.getWatchClient();

        // TODO need to read back current persistent state from etcd on start-up...
    }

    @Override
    public void close() {
        etcdKV.close();
        etcdWatch.close();
    }

    @Override
    protected synchronized void commit(DataTreeCandidate candidate) {
        // TODO transform DataTreeCandidate into etcd operations...
        super.commit(candidate);
    }
}
