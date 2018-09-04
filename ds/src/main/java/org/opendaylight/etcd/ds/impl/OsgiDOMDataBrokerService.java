/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import io.etcd.jetcd.Client;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.spi.ForwardingDOMDataBroker;
import org.ops4j.pax.cdi.api.OsgiService;

/**
 * {@link DOMDataBroker} registered in the OSGi service registry.
 *
 * @author Michael Vorburger.ch
 */
@Singleton
// do NOT @OsgiServiceProvider(classes = DOMDataBroker.class), because we need odl:type="default" so BP XML
public class OsgiDOMDataBrokerService extends ForwardingDOMDataBroker {

    private final EtcdDOMDataBrokerProvider wiring;

    @Inject
    public OsgiDOMDataBrokerService(@OsgiService DOMSchemaService schemaService) throws Exception {
        // TODO Remove this constructor with hard-coded etcd server endpoint by using @OsgiService Client (below)
        // For the moment this does not work, most probably because of the org/opendaylight/blueprint/ds-blueprint.xml
        // which we need so that we can look up other OSGi services which mdsal and controller registered; this should
        // get solved with odlparent 4.0.0 which does away with org/opendaylight/blueprint and the 2 (std VS odl) BPs.
        this(schemaService, Client.builder().endpoints("http://localhost:2379").build());
    }

    // the Client is set up in the OSGi Service registry by io.etcd:jetcd-osgi, based on etc/io.etcd.jetcd.cfg
    public OsgiDOMDataBrokerService(@OsgiService DOMSchemaService schemaService, @OsgiService Client etcdClient)
            throws Exception {
        wiring = new EtcdDOMDataBrokerProvider(etcdClient, "", schemaService);
    }

    @PreDestroy
    public void close() throws Exception {
        wiring.close();
    }

    @Override
    protected DOMDataBroker delegate() {
        return wiring.getDOMDataBroker();
    }
}
