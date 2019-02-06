/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.utils;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;

/**
 * Utilities for {@link ByteSequence}.
 *
 * @author Michael Vorburger.ch
 */
public final class KeyValues {

    private KeyValues() { }

    public static void append(StringBuilder sb, KeyValue kv) {
        sb.append(ByteSequences.asString(kv.getKey()));
        sb.append(" ➙ ");
        sb.append(ByteSequences.asString(kv.getValue()));
        sb.append(" (modRev=");
        sb.append(kv.getModRevision());
/*
        sb.append(", createRev=");
        sb.append(kv.getCreateRevision());
        sb.append(", ver=");
        sb.append(kv.getVersion());
*/
        sb.append(')');
    }

    public static Object toStringable(KeyValue kv) {
        return new Object() {
            @Override
            public String toString() {
                return KeyValues.asString(kv);
            }
        };
    }

    public static String asString(KeyValue kv) {
        StringBuilder sb = new StringBuilder();
        append(sb, kv);
        return sb.toString();
    }

}
