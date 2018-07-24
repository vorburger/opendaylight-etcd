/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import com.coreos.jetcd.Client;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.etcd.ds.impl.EtcdDataStore;
import org.opendaylight.mdsal.binding.dom.adapter.test.ConcurrentDataBrokerTestCustomizer;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;

/**
 * Customizer, used in {@link TestEtcdDataBrokersProvider}.
 *
 * @see TestEtcdDataBrokersProvider
 *
 * @author Michael Vorburger.ch
 */
// intentionally just package-local, for now
class EtcdConcurrentDataBrokerTestCustomizer extends ConcurrentDataBrokerTestCustomizer {

    // TODO later generalize this a bit so it can also be used for runtime OSGi service, not just tests

    private final Client client;
    private EtcdDataStore configurationDataStore;
    private EtcdDataStore operationalDataStore;

    EtcdConcurrentDataBrokerTestCustomizer(Client client) {
        super(true);
        this.client = client;
    }

    private EtcdDataStore createConfigurationDatastore(LogicalDatastoreType type) {
        EtcdDataStore store = new EtcdDataStore(type, getDataTreeChangeListenerExecutor(),
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE, client, true);
        getSchemaService().registerSchemaContextListener(store);
        return store;
    }

    @Override
    public DOMStore createConfigurationDatastore() {
        if (configurationDataStore != null) {
            throw new IllegalStateException("Whoa; configurationDataStore already created!");
        }
        configurationDataStore = createConfigurationDatastore(CONFIGURATION);
        return configurationDataStore;
    }

    public EtcdDataStore getConfigurationDataStore() {
        return configurationDataStore;
    }

    @Override
    public DOMStore createOperationalDatastore() {
        if (operationalDataStore != null) {
            throw new IllegalStateException("Whoa; operationalDataStore already created!");
        }
        operationalDataStore = createConfigurationDatastore(OPERATIONAL);
        return operationalDataStore;
    }

    public EtcdDataStore getOperationalDataStore() {
        return operationalDataStore;
    }

    @Override
    public DOMDataBroker createDOMDataBroker() {
        // TODO use
        // org.opendaylight.controller.cluster.databroker.ConcurrentDOMDataBroker
        // instead of SerializedDOMDataBroker ?
        return super.createDOMDataBroker();
    }
}
