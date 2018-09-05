/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.eos.impl;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.mdsal.eos.dom.simple.SimpleDOMEntityOwnershipService;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;

/**
 * DOMEntityOwnershipService implementation, based on etcd.
 *
 * @author Michael Vorburger.ch
 */
@Singleton
@OsgiServiceProvider(classes = DOMEntityOwnershipService.class)
public class EtcdDOMEntityOwnershipService extends DelegatingDOMEntityOwnershipService {

    // TODO replace this fake (simple) EOS by a real one backed by etcd...

    @Inject
    public EtcdDOMEntityOwnershipService() {
        super(new SimpleDOMEntityOwnershipService());
    }

}
