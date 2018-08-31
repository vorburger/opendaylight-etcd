/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.utils;

import com.google.errorprone.annotations.Var;
import io.etcd.jetcd.data.ByteSequence;
import java.util.Arrays;

/**
 * Utilities for {@link ByteSequence}.
 *
 * @author Michael Vorburger.ch
 */
public final class ByteSequences {

    private ByteSequences() { }

    public static Object toStringable(ByteSequence byteSequence) {
        return new Object() {
            @Override
            public String toString() {
                return ByteSequences.asString(byteSequence);
            }
        };
    }

    public static String asString(ByteSequence byteSequence) {
        return asString(byteSequence.getBytes());
    }

    // from ch.vorburger.dom2kv.bite.ByteSeq
    private static String asString(byte[] bytes) {
        StringBuilder text = new StringBuilder();
        text.append('«');
        @Var boolean isEscaping = false;
        for (byte b : bytes) {
            if (b >= 0x20 && b <= 0x7e) {
                if (isEscaping) {
                    text.append('·');
                    isEscaping = false;
                }
                text.append((char) b);
            } else {
                if (!isEscaping) {
                    text.append('·');
                    isEscaping = true;
                }
                text.append(String.format("%02X", b & 0xFF));
            }
        }
        text.append('»');
        return text.toString();
    }

    public static boolean startsWith(ByteSequence baseByteSequence, ByteSequence prefix) {
        return baseByteSequence.getByteString().startsWith(prefix.getByteString());
    }

    public static ByteSequence fromBytes(byte... bytes) {
        return ByteSequence.from(bytes);
    }

    public static ByteSequence append(ByteSequence base, byte... bytes) {
        byte[] newBytes = Arrays.copyOf(base.getBytes(), base.getBytes().length + bytes.length);
        System.arraycopy(bytes, 0, newBytes, base.getBytes().length, bytes.length);
        return ByteSequence.from(newBytes);
    }
}
