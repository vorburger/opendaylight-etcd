/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;

/**
 * Provides a {@link DOMDataBroker} backed by etcd.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdDOMDataBrokerWiring implements AutoCloseable {

    private final Client etcdClient;
    private final TestEtcdDataBrokerProvider testEtcdDataBrokerProvider;
    private final DataBroker dataBroker;

    // TODO this is currently heavily orient towards testing, and needs to evolve to
    // be able to be used both for testing and real OSGi and standalone runtime environments.
    // The constructor will likely be extended to accept more args for diff between test/RT.

    public EtcdDOMDataBrokerWiring(ClientBuilder etcdClientBuilder) throws Exception {
        this.etcdClient = etcdClientBuilder.build();
        this.testEtcdDataBrokerProvider = new TestEtcdDataBrokerProvider(etcdClient);
        this.dataBroker = testEtcdDataBrokerProvider.getDataBroker();
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    @Override
    public void close() throws Exception {
        if (testEtcdDataBrokerProvider != null) {
            testEtcdDataBrokerProvider.close();
        }
        if (etcdClient != null) {
            etcdClient.close();
        }
    }

}
