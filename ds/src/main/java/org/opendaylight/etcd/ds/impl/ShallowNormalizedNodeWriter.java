/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;

/**
 * Iterator over a {@link NormalizedNode}.
 * Similar to {@link NormalizedNodeWriter}, but different in that this will skip children.
 *
 * @author Michael Vorburger.ch
 */
@NotThreadSafe
class ShallowNormalizedNodeWriter extends NormalizedNodeWriter {

    ShallowNormalizedNodeWriter(NormalizedNodeStreamWriter writer) {
        super(writer);
    }

    @Override
    protected boolean writeChildren(Iterable<? extends NormalizedNode<?, ?>> children) throws IOException {
        // like parent, just actually ignoring children
        getWriter().endNode();
        return true;
    }

}
