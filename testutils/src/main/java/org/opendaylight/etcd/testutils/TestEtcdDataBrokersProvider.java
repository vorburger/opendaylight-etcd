/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.binding.test.ConcurrentDataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Provides a {@link DataBroker} and a {@link DOMDataBroker} backed by etcd.
 *
 * @author Michael Vorburger.ch
 */
public class TestEtcdDataBrokersProvider {
    // TODO later generalize this a bit so it can also be used for runtime OSGi service, not just tests

    private final DataBroker dataBroker;
    private final DOMDataBroker domDataBroker;

    public TestEtcdDataBrokersProvider() {
        AbstractDataBrokerTestCustomizer testCustomizer = new ConcurrentDataBrokerTestCustomizer(true);
        dataBroker = testCustomizer.createDataBroker();
        domDataBroker = testCustomizer.createDOMDataBroker();
        testCustomizer.updateSchema(getSchemaContext());
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

    protected SchemaContext getSchemaContext() {
        return SchemaContextSingleton.getSchemaContext(() -> newSchemaContext());
    }

    protected SchemaContext newSchemaContext() {
        final Iterable<YangModuleInfo> moduleInfos = loadModuleInfos();
        final ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
        moduleContext.addModuleInfos(moduleInfos);
        return moduleContext.tryToCreateSchemaContext().get();
    }

    protected Iterable<YangModuleInfo> loadModuleInfos() {
        return BindingReflections.loadModuleInfos();
    }

}
