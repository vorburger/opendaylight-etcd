/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import java.io.DataOutput;
import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.etcd.ds.stream.copypaste.NormalizedNodeOutputStreamWriter;
import org.opendaylight.etcd.ds.stream.copypaste.TokenTypes;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;

/**
 * Writer of {@link NormalizedNode}s.
 * Similar to {@link NormalizedNodeDataOutput}, but different in that this will not write out the deep contents, only
 * the hull.  This is because in a Key Value data store, a NormalizedNode is broken up into (and re-composed from) many
 * individual values.  This also does not write out the QName of nodes etc. because that will be stored in the Key of
 * the backing Key Value store.
 *
 * @author Michael Vorburger.ch
 */
@NotThreadSafe
class ShallowNormalizedNodeDataOutputWriter extends NormalizedNodeOutputStreamWriter {

    ShallowNormalizedNodeDataOutputWriter(DataOutput output) {
        super(output);
    }

    @Override
    protected NormalizedNodeWriter newNormalizedNodeWriter() {
        return new ShallowNormalizedNodeWriter(this);
    }

    /**
     * Does not write out any version header!
     * If we ever do need this, we'll do it globally stored once, instead of wasting 3 bytes in EVERY value.
     * See also {@link ShallowNormalizedNodeInputStreamReader#readSignatureMarkerAndVersionIfNeeded()}.
     */
    @Override
    protected void ensureHeaderWritten() {
    }

    @Override
    protected void writeString(String string) throws IOException {
        // similar to original but, for now, without the stringCodeMap compression
        // mostly just to make it easier to understand what is being written and read during early debugging
        // TODO when re-integrating, make stringCodeMap compression a configuration option
        if (string != null) {
            writeByte(TokenTypes.IS_STRING_VALUE);
            writeUTF(string);
        } else {
            writeByte(TokenTypes.IS_NULL_VALUE);
        }
    }

    @Override
    protected void startNode(byte nodeType, QName qname) throws IOException {
        // ditch the QName (startNode variant with only nodeType added in copypaste parent)
        super.startNode(nodeType);
    }

}
