/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.gson.test;

import static com.google.common.truth.Truth.assertThat;

import ch.vorburger.dom2kv.Sequence;
import ch.vorburger.dom2kv.gson.jetcd.JsonPathBytes;
import ch.vorburger.dom2kv.impl.SequenceListImpl;
import org.junit.Test;

/**
 * Unit test for {@link JsonPathBytes}.
 *
 * @author Michael Vorburger.ch
 */
public class JsonPathBytesTest {

    @Test
    public void stringSeqToBytes() {
        Sequence<String> seq = JsonPathBytes.BYTES_TO_STRING_SEQ
            .apply(JsonPathBytes.STRING_SEQ_TO_BYTES.apply(new SequenceListImpl<>("a", "⛑️", "b", "🇨🇭")));
        assertThat(seq.head()).isEqualTo("a");
        assertThat(seq.tail().head()).isEqualTo("⛑️");
        assertThat(seq.tail().tail().head()).isEqualTo("b");
        assertThat(seq.tail().tail().tail().head()).isEqualTo("🇨🇭");
    }
}
