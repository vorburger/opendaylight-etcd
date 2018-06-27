/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.utils.test;

import static com.coreos.jetcd.data.ByteSequence.fromBytes;
import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.etcd.utils.ByteSequences.toStringable;

import com.coreos.jetcd.data.ByteSequence;
import org.junit.Test;

/**
 * Unit test for {@link ByteSequence}.
 *
 * @author Michael Vorburger.ch
 */
public class ByteSequencesTest {

    @Test
    public void testToString() {
        assertBytesToString("«»");
        assertBytesToString("«·00»", 0);
        assertBytesToString("«·FF»", 255);
        assertBytesToString("«·0000»", 0, 0);
        assertBytesToString("«A»", 65);
        assertBytesToString("«AB»", 65, 66);
        assertBytesToString("«·00·a»", 0, 97);
        assertBytesToString("«a·0A»", 97, 10);
    }

    private void assertBytesToString(String expectedText, int... givenBytes) {
        assertThat(toStringable(fromBytes(bytes(givenBytes))).toString()).isEqualTo(expectedText);
    }

    private byte[] bytes(int... bytes) {
        byte[] byteArray = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byteArray[i] = (byte) bytes[i];
        }
        return byteArray;
    }

}
