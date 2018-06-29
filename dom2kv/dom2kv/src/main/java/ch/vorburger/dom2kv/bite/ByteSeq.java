/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.bite;

import com.google.errorprone.annotations.Var;
import java.util.Arrays;

/**
 * Sequence of Bytes.
 *
 * @author Michael Vorburger.ch
 */
public abstract class ByteSeq {

    abstract byte[] bytes();

    abstract ByteSeq append(ByteSeq byteSequence);

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ByteSeq)) {
            return false;
        }
        ByteSeq other = (ByteSeq) obj;
        return Arrays.equals(bytes(), other.bytes());
    }

    @Override
    public String toString() {
        return asString(bytes());
    }

    public static String asString(byte[] bytes) {
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
}
