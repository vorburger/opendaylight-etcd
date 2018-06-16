/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils;

import java.util.function.Supplier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * {@link SchemaContext} singleton holder (static).
 *
 * <p>This is useful in scenarios such as unit tests, but not OSGi environments,
 * where there is a flat classpath and thus really only one single
 * SchemaContext.
 *
 * @author Michael Vorburger.ch
 */
public final class SchemaContextSingleton {

    // TODO move org.opendaylight.controller.md.sal.binding.test.SchemaContextSingleton
    // from sal-binding-broker/src/test/java to sal-testutils(?)/src/main/java and use that

    private static SchemaContext staticSchemaContext;

    public static synchronized SchemaContext getSchemaContext(Supplier<SchemaContext> supplier) {
        if (staticSchemaContext == null) {
            staticSchemaContext = supplier.get();
        }
        return staticSchemaContext;
    }

    private SchemaContextSingleton() { }

}
