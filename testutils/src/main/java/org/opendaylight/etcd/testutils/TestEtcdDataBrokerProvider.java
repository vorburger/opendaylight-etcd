/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils;

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.coreos.jetcd.ClientBuilder;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.opendaylight.controller.md.sal.binding.test.SchemaContextSingleton;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.test.util.MockSchemaService;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Provides a {@link DataBroker} and a {@link DOMDataBroker} backed by etcd for use in tests.
 *
 * @author Michael Vorburger.ch
 */
public class TestEtcdDataBrokerProvider implements AutoCloseable {

    private final DataBroker dataBroker;
    private final DOMDataBroker domDataBroker;
    private final EtcdDataBrokerWiring wiring;

    public TestEtcdDataBrokerProvider(ClientBuilder clientBuilder, String name) throws Exception {
        // from org.opendaylight.mdsal.binding.dom.adapter.test.ConcurrentDataBrokerTestCustomizer
        ListeningExecutorService dataTreeChangeListenerExecutorSingleton = listeningDecorator(newCachedThreadPool());
        ListeningExecutorService commitCoordinatorExecutor = listeningDecorator(newSingleThreadExecutor());
        // from org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer
        MockSchemaService schemaService = new MockSchemaService();
        ClassLoadingStrategy classLoading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();

        wiring = new EtcdDataBrokerWiring(clientBuilder, name, commitCoordinatorExecutor,
                dataTreeChangeListenerExecutorSingleton, schemaService, classLoading);
        dataBroker = wiring.getDataBroker();
        domDataBroker = wiring.getDOMDataBroker();

        SchemaContext schemaContext = SchemaContextSingleton.getSchemaContext(() -> newSchemaContext());
        schemaService.changeSchema(schemaContext);
        wiring.init();
    }

    @Override
    public void close() throws Exception {
        wiring.close();
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
    // have our own implementation here, very similar, just few lines of copy/paste:

    private static SchemaContext newSchemaContext() {
        Iterable<YangModuleInfo> moduleInfos = loadModuleInfos();
        ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
        moduleContext.addModuleInfos(moduleInfos);
        return moduleContext.tryToCreateSchemaContext().get();
    }

    private static Iterable<YangModuleInfo> loadModuleInfos() {
        return BindingReflections.loadModuleInfos();
    }
}
