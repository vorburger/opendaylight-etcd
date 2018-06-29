/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.utils;

import ch.vorburger.dom2kv.bite.ByteSeq;
import com.coreos.jetcd.data.ByteSequence;

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
        return ByteSeq.asString(byteSequence.getBytes());
    }

}
