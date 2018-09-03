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
import org.ops4j.pax.cdi.api.OsgiServiceProvider;

/**
 * {@link DOMDataBroker} registered in the OSGi service registry.
 *
 * @author Michael Vorburger.ch
 */
@Singleton
@OsgiServiceProvider(classes = DOMDataBroker.class)
public class OsgiDOMDataBrokerService extends ForwardingDOMDataBroker {

    private final EtcdDOMDataBrokerProvider wiring;

    @Inject
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
