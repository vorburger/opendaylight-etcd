/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.sal.core.compat.LegacyDOMDataBrokerAdapter;

/**
 * Adapter between the legacy controller API-based DOMDataBroker and the mdsal API-based DOMDataBroker.
 * This class's only purpose is to cause the Import-Package: for LegacyDOMDataBrokerAdapter
 * and (controller's) DOMDataBroker in the MANIFEST.
 *
 * @author Michael Vorburger.ch
 */
@Singleton
@SuppressWarnings("deprecation") // because that's the whole point of this class
// the implements, while implicit to Java, is required for an Import-Package: for controller
@SuppressFBWarnings("RI_REDUNDANT_INTERFACES")
public class EtcdLegacyDOMDataBroker extends LegacyDOMDataBrokerAdapter
        implements org.opendaylight.controller.md.sal.dom.api.DOMDataBroker {

    @Inject
    public EtcdLegacyDOMDataBroker(EtcdDOMDataBroker delegate) {
        super(delegate);
    }

}
