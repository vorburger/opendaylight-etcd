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
        = key -> new SequenceListImpl<>(Splitter.on(UNIT_SEPARATOR_CHAR).split(new String(key.getBytes(),
                java.nio.charset.Charset.defaultCharset()))); // see below & https://github.com/coreos/jetcd/issues/342

/*
    TODO above works, but is platform specific, so you couldn't copy data to a system with another encoding.
    Below should be platform independent - but breaks the JsonPathBytesTest; why??
    Probably because of the UNIT_SEPARATOR_CHAR...
    also change JsonValueBytes

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final CharsetEncoder UTF8_ENCODER = UTF8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    private static final CharsetDecoder UTF8_DECODER = UTF8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

    public static final Function<Sequence<String>, ByteSequence> STRING_SEQ_TO_BYTES = ids -> {
        try {
            return new ByteSequence(
                UTF8_ENCODER.encode(CharBuffer.wrap(Joiner.on(UNIT_SEPARATOR_CHAR).join(ids))).array());
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("CharacterCodingException for: " + ids, e);
        }
    };

    public static final Function<ByteSequence, Sequence<String>> BYTES_TO_STRING_SEQ = key -> {
        try {
            return new SequenceListImpl<>(
                    Splitter.on(UNIT_SEPARATOR_CHAR).split(UTF8_DECODER.decode(ByteBuffer.wrap(key.getBytes()))));
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("CharacterCodingException for: " + key, e);
        }
    };
*/
}
