/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils;

import org.opendaylight.controller.md.sal.binding.test.ConcurrentDataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.etcd.ds.EtcdDataStore;

/**
 * Customizer, used in {@link TestEtcdDataBrokersProvider}.
 * @see TestEtcdDataBrokersProvider
 * @author Michael Vorburger.ch
 */
// intentionally just package-local, for now
class EtcdConcurrentDataBrokerTestCustomizer extends ConcurrentDataBrokerTestCustomizer {
    // TODO later generalize this a bit so it can also be used for runtime OSGi service, not just tests

    EtcdConcurrentDataBrokerTestCustomizer() {
        super(true);
    }

    @Override
    public DOMDataBroker createDOMDataBroker() {
        // TODO use
        // org.opendaylight.controller.cluster.databroker.ConcurrentDOMDataBroker
        // instead of SerializedDOMDataBroker ?
        return super.createDOMDataBroker();
    }

    @Override
    public DOMStore createConfigurationDatastore() {
        DOMStore store = new EtcdDataStore(null, null
                // CONFIGURATION, getDataTreeChangeListenerExecutor()
                );
        // TODO if EtcdDataStore implements SchemaContextListener:
        // getSchemaService().registerSchemaContextListener(store);
        return store;
    }

    @Override
    public DOMStore createOperationalDatastore() {
        DOMStore store = new EtcdDataStore(null, null
                // CONFIGURATION, getDataTreeChangeListenerExecutor()
                );
        // TODO if EtcdDataStore implements SchemaContextListener:
        // getSchemaService().registerSchemaContextListener(store);
        return store;
    }

}
