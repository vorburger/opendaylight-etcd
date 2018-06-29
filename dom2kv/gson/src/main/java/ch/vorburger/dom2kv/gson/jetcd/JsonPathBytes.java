/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.gson.jetcd;

import ch.vorburger.dom2kv.Sequence;
import ch.vorburger.dom2kv.impl.SequenceListImpl;
import com.coreos.jetcd.data.ByteSequence;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.function.Function;

/**
 * Functions to convert a JSON (String) path of (String) property names to and from an etcd ByteSequence.
 * @author Michael Vorburger.ch
 */
public final class JsonPathBytes {

    private JsonPathBytes() { }

    // https://en.wikipedia.org/wiki/C0_and_C1_control_codes
    private static final char UNIT_SEPARATOR_CHAR = 31;

    public static final Function<Sequence<String>, ByteSequence> STRING_SEQ_TO_BYTES
        = ids -> ByteSequence.fromString(Joiner.on(UNIT_SEPARATOR_CHAR).join(ids));

    public static final Function<ByteSequence, Sequence<String>> BYTES_TO_STRING_SEQ
        = key -> new SequenceListImpl<>(Splitter.on(UNIT_SEPARATOR_CHAR).split(new String(key.getBytes())));
}
