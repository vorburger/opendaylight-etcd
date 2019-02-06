/*
 * Copyright (c) 2019 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils;

import io.etcd.jetcd.Client;
import java.net.URI;
import java.util.Collection;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.opendaylight.etcd.ds.impl.TestTool;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;

/**
 * JUnit Rule providing a {@link DataBroker} &amp; {@link DOMDataBroker} backed by etcd.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdDataBrokerRule implements TestRule {

    private final Collection<URI> etcdServerURIs;
    private final String name;

    protected TestEtcdDataBrokerProvider currentProvider;

    public EtcdDataBrokerRule(EtcdLauncherRule launcherRule, String name) {
        etcdServerURIs = launcherRule.getClusterURIs();
        this.name = name;
    }

    public EtcdDataBrokerRule(EtcdLauncherRule launcherRule) {
        this(launcherRule, EtcdDataBrokerRule.class.getSimpleName());
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                try (Client client = Client.builder().endpoints(etcdServerURIs).build()) {
                    try (TestEtcdDataBrokerProvider dbProvider = new TestEtcdDataBrokerProvider(client, name)) {
                        currentProvider = dbProvider;
                        statement.evaluate();
                    }
                    currentProvider = null;
                }
            }
        };
    }

    public DOMDataBroker getDOMDataBroker() {
        return currentProvider.getDOMDataBroker();
    }

    public DataBroker getDataBroker() {
        return currentProvider.getDataBroker();
    }

    public TestTool getTestTool() {
        return currentProvider.getTestTool();
    }
}
