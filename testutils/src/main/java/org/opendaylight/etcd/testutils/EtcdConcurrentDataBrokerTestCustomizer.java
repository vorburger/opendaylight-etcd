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
import org.opendaylight.controller.md.sal.binding.test.ConcurrentDataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.etcd.ds.impl.EtcdDataStore;

/**
 * Customizer, used in {@link TestEtcdDataBrokersProvider}.
 * @see TestEtcdDataBrokersProvider
 * @author Michael Vorburger.ch
 */
// intentionally just package-local, for now
class EtcdConcurrentDataBrokerTestCustomizer extends ConcurrentDataBrokerTestCustomizer {

    private static final byte CONFIGURATION_PREFIX = 67; // 'C'
    private static final byte OPERATIONAL_PREFIX   = 79; // 'O'

    private final Client client;

    // TODO later generalize this a bit so it can also be used for runtime OSGi service, not just tests

    EtcdConcurrentDataBrokerTestCustomizer(Client client) {
        super(true);
        this.client = client;
    }

    private DOMStore createConfigurationDatastore(LogicalDatastoreType type) {
        byte prefix = type.equals(LogicalDatastoreType.CONFIGURATION) ? CONFIGURATION_PREFIX : OPERATIONAL_PREFIX;
        DOMStore store = new EtcdDataStore(prefix, client, true /* , getDataTreeChangeListenerExecutor() */);
        // TODO if EtcdDataStore implements SchemaContextListener:
        // getSchemaService().registerSchemaContextListener(store);
        return store;
    }

    @Override
    public DOMStore createConfigurationDatastore() {
        return createConfigurationDatastore(CONFIGURATION);
    }

    @Override
    public DOMStore createOperationalDatastore() {
        return createConfigurationDatastore(OPERATIONAL);
    }

    @Override
    public DOMDataBroker createDOMDataBroker() {
        // TODO use
        // org.opendaylight.controller.cluster.databroker.ConcurrentDOMDataBroker
        // instead of SerializedDOMDataBroker ?
        return super.createDOMDataBroker();
    }
}
