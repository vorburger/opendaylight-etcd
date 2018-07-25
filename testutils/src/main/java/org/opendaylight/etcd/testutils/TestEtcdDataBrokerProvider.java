/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils;

import com.coreos.jetcd.Client;
import org.opendaylight.controller.md.sal.binding.test.SchemaContextSingleton;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Provides a {@link DataBroker} and a {@link DOMDataBroker} backed by etcd.
 *
 * @author Michael Vorburger.ch
 */
public class TestEtcdDataBrokerProvider implements AutoCloseable {

    private final DataBroker dataBroker;
    private final DOMDataBroker domDataBroker;
    private final EtcdConcurrentDataBrokerTestCustomizer testCustomizer;

    public TestEtcdDataBrokerProvider(Client client) throws Exception {
        testCustomizer = new EtcdConcurrentDataBrokerTestCustomizer(client);
        dataBroker = testCustomizer.createDataBroker();
        domDataBroker = testCustomizer.createDOMDataBroker();
        testCustomizer.updateSchema(getSchemaContext());

        // TODO remove this again once EtcdDataStore automatically (re)loads from etcd
        testCustomizer.getConfigurationDataStore().initialLoad();
        testCustomizer.getOperationalDataStore().initialLoad();
    }

    @Override
    public void close() throws Exception {
        testCustomizer.close();
    }

    public DOMDataBroker getDOMDataBroker() {
        return domDataBroker;
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    // the following is inspired by
    // org.opendaylight.controller.md.sal.binding.test.AbstractBaseDataBrokerTest,
    // org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTestCustomizer,
    // org.opendaylight.controller.md.sal.binding.test.AbstractSchemaAwareTest and
    // org.opendaylight.controller.md.sal.binding.test.ConstantSchemaAbstractDataBrokerTest
    // but that is too intertwined with AbstractConcurrentDataBrokerTest, so we just
    // have our own implementation here, very similar

    // make "final" to avoid http://errorprone.info/bugpattern/ConstructorInvokesOverridable in constructor
    protected final SchemaContext getSchemaContext() throws Exception {
        return SchemaContextSingleton.getSchemaContext(() -> newSchemaContext());
    }

    protected SchemaContext newSchemaContext() {
        Iterable<YangModuleInfo> moduleInfos = loadModuleInfos();
        ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
        moduleContext.addModuleInfos(moduleInfos);
        return moduleContext.tryToCreateSchemaContext().get();
    }

    protected Iterable<YangModuleInfo> loadModuleInfos() {
        return BindingReflections.loadModuleInfos();
    }

}
