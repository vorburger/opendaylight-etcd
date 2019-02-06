/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils;

import com.google.common.collect.ImmutableSet;
import io.etcd.jetcd.Client;
import javassist.ClassPool;
import javax.annotation.PostConstruct;
import org.opendaylight.controller.md.sal.binding.test.SchemaContextSingleton;
import org.opendaylight.etcd.ds.impl.EtcdDOMDataBrokerProvider;
import org.opendaylight.etcd.ds.impl.TestTool;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.adapter.test.util.MockSchemaService;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Provides a {@link DataBroker} and a {@link DOMDataBroker} backed by etcd for use in tests.
 *
 * @author Michael Vorburger.ch
 */
public class TestEtcdDataBrokerProvider implements AutoCloseable {

    private final MockSchemaService schemaService;
    private final EtcdDOMDataBrokerProvider wiring;
    private final DataBroker dataBroker;

    // TODO pass Client instead of ClientBuilder
    public TestEtcdDataBrokerProvider(Client client, String name) throws Exception {
        // from org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer
        schemaService = new MockSchemaService();

        // create DOMDataBroker
        wiring = new EtcdDOMDataBrokerProvider(client, name, schemaService);

        // create DataBroker
        ClassPool pool = ClassPool.getDefault();
        ClassLoadingStrategy classLoading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
        DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(pool));
        BindingNormalizedNodeCodecRegistry codecs = new BindingNormalizedNodeCodecRegistry(generator);
        BindingToNormalizedNodeCodec bindingToNormalized = new BindingToNormalizedNodeCodec(classLoading, codecs);
        schemaService.registerSchemaContextListener(bindingToNormalized);
        dataBroker = new BindingDOMDataBrokerAdapter(wiring.getDOMDataBroker(), bindingToNormalized);

        // Must happen AFTER above - else no BindingRuntimeContext set in
        // BindingNormalizedNodeCodecRegistry's onBindingRuntimeContextUpdated()
        SchemaContext schemaContext = SchemaContextSingleton.getSchemaContext(() -> newSchemaContext());
        schemaService.changeSchema(schemaContext);
        wiring.init();
    }

    @Override
    @PostConstruct
    public void close() throws Exception {
        wiring.close();
    }

    public DOMSchemaService getDOMSchemaService() {
        return schemaService;
    }

    public DOMDataBroker getDOMDataBroker() {
        return wiring.getDOMDataBroker();
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public TestTool getTestTool() {
        return wiring.getTestTool();
    }

    // the following is inspired by
    // org.opendaylight.controller.md.sal.binding.test.AbstractBaseDataBrokerTest,
    // org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTestCustomizer,
    // org.opendaylight.controller.md.sal.binding.test.AbstractSchemaAwareTest and
    // org.opendaylight.controller.md.sal.binding.test.ConstantSchemaAbstractDataBrokerTest
    // but that is too intertwined with AbstractConcurrentDataBrokerTest, so we just
    // have our own implementation here, very similar, just few lines of copy/paste:

    private static SchemaContext newSchemaContext() {
        ImmutableSet<YangModuleInfo> moduleInfos = loadModuleInfos();
        ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
        moduleContext.addModuleInfos(moduleInfos);
        return moduleContext.tryToCreateSchemaContext().get();
    }

    private static ImmutableSet<YangModuleInfo> loadModuleInfos() {
        return BindingReflections.loadModuleInfos();
    }
}
