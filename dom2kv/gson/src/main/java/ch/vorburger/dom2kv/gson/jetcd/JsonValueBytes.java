/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.gson.jetcd;

import com.coreos.jetcd.data.ByteSequence;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * Functions to convert a JSON value (non-String) to and from an etcd ByteSequence.
 * This is loosely inspired by <a href="http://bsonspec.org">BSON</a>.
 * @author Michael Vorburger.ch
 */
public final class JsonValueBytes {

    // TODO write a unit test covering this

    private JsonValueBytes() { }

    public static final Function<Object, ByteSequence> OBJECT_TO_BYTES = value -> {
        if (value instanceof String) {
            // see JsonPathBytes re. String byte[] encoding
            // TODO UTF-8 instead of platform specific when https://github.com/coreos/jetcd/issues/342 is available
            return prefix(2, ByteSequence.fromString((String) value));
        } else if (value instanceof Double) {
            byte[] bytes = new byte[8];
            ByteBuffer.wrap(bytes).putDouble((Double) value);
            return prefix(1, new ByteSequence(bytes));
        } else if (value instanceof Boolean) {
            if ((Boolean) value) {
                return new ByteSequence(new byte[] { 9 });
            } else {
                return new ByteSequence(new byte[] { 8 });
            }
        } else {
            throw new IllegalArgumentException("Unknown type: " + value);
        }
    };

    public static final Function<ByteSequence, Object> BYTES_TO_OBJECT = byteSequence -> {
        byte[] bytes = byteSequence.getBytes();
        if (bytes.length == 0) {
            return null;
        }
        byte type = bytes[0];

        switch (type) {
            case 2:
                // TODO change to UTF-8 instead of platform specific when above is changed (and see also JsonPathBytes)
                return new String(dropPrefix(bytes), Charset.defaultCharset());

            case 1:
                return ByteBuffer.wrap(dropPrefix(bytes)).getDouble();

            case 9:
                return Boolean.TRUE;

            case 8:
                return Boolean.FALSE;

            default:
                throw new IllegalArgumentException("Unknown type: " + type);
        }
    };

    private static ByteSequence prefix(int type, ByteSequence tail) {
        byte[] bytes = tail.getBytes();
        byte[] withType = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, withType, 1, bytes.length);
        withType[0] = (byte) type;
        return new ByteSequence(withType);
    }

    private static byte[] dropPrefix(byte[] bytes) {
        byte[] withoutType = new byte[bytes.length - 1];
        System.arraycopy(bytes, 1, withoutType, 0, bytes.length - 1);
        return withoutType;
    }
}
