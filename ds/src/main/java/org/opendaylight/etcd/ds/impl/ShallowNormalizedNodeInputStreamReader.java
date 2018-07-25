/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import java.io.DataInput;
import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.etcd.ds.stream.copypaste.NormalizedNodeInputStreamReader;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Reader of {@link NormalizedNode}s.
 * Similar to {@link NormalizedNodeInputStreamReader}, but different in that this will not read child nodes.
 * It can takes the node's name as an argument instead of reading it from the stream.
 *
 * @author Michael Vorburger.ch
 */
@NotThreadSafe
class ShallowNormalizedNodeInputStreamReader extends NormalizedNodeInputStreamReader {

    private QName firstQName;

    ShallowNormalizedNodeInputStreamReader(DataInput input, QName firstQName) {
        this(input);
        this.firstQName = firstQName;
    }

    ShallowNormalizedNodeInputStreamReader(DataInput input) {
        super(input, false);
    }

    /**
     * See {@link ShallowNormalizedNodeDataOutputWriter#ensureHeaderWritten()}.
     */
    @Override
    protected void readSignatureMarkerAndVersionIfNeeded() {
    }

    @Override
    protected QName readQName() throws IOException {
        if (firstQName != null) {
            QName theFirstQName = firstQName;
            firstQName = null;
            return theFirstQName;
        } else {
            return super.readQName();
        }
    }
}
