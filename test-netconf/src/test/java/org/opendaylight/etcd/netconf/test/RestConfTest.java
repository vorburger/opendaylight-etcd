/*
 * Copyright (c) 2019 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.netconf.test;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.etcd.testutils.EtcdDataBrokerRule;
import org.opendaylight.etcd.testutils.EtcdLauncherRule;
import org.opendaylight.restconf.nb.rfc8040.handlers.SchemaContextHandler;
import org.opendaylight.restconf.nb.rfc8040.handlers.TransactionChainHandler;

/**
 * Test RESTCONF running on etcd.
 *
 * @author Michael Vorburger.ch
 */
public class RestConfTest {

    // TODO when https://git.opendaylight.org/gerrit/#/c/79388/ is merged, use that

    public static @ClassRule EtcdLauncherRule etcdLauncher = new EtcdLauncherRule();

    public @Rule EtcdDataBrokerRule dbRule = new EtcdDataBrokerRule(etcdLauncher);

    @Test
    // for https://github.com/vorburger/opendaylight-etcd/issues/8, but doesn't
    // really reproduce it, unless there are a lot of YANG models on the classpath
    public void testSchemaContextHandlerInitialization() {
        try (TransactionChainHandler transactionChainHandler = new TransactionChainHandler(dbRule.getDOMDataBroker())) {
            try (SchemaContextHandler schemaContextHandler = SchemaContextHandler.newInstance(transactionChainHandler,
                    dbRule.getDOMSchemaService())) {
                schemaContextHandler.init();
                schemaContextHandler.onGlobalContextUpdated(dbRule.getDOMSchemaService().getGlobalContext());
            }
        }
    }

    @Test
    @Ignore // TODO https://github.com/vorburger/opendaylight-etcd/issues/9
    public void testSchemaContextHandlerInitializationAgain() {
        try (TransactionChainHandler transactionChainHandler = new TransactionChainHandler(dbRule.getDOMDataBroker())) {
            try (SchemaContextHandler schemaContextHandler = SchemaContextHandler.newInstance(transactionChainHandler,
                    dbRule.getDOMSchemaService())) {
                schemaContextHandler.init();
                schemaContextHandler.onGlobalContextUpdated(dbRule.getDOMSchemaService().getGlobalContext());
            }
        }
    }
}
