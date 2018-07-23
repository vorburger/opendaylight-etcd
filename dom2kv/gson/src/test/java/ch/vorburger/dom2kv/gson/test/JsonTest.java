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
import ch.vorburger.dom2kv.Tree;
import ch.vorburger.dom2kv.gson.GsonTree;
import ch.vorburger.dom2kv.impl.SequenceListImpl;
import ch.vorburger.dom2kv.impl.TransformerImpl;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.gson.JsonSyntaxException;
import java.util.function.Function;
import org.junit.Test;

/**
 * Test JSON to KVs.
 *
 * @author Michael Vorburger.ch
 */
public class JsonTest {

    @Test
    public void validEmptyJSON() {
        assertThat(new GsonTree().parse("{}").root()).isEmpty();
    }

    @Test(expected = JsonSyntaxException.class)
    public void invalidEmptyString() {
        new GsonTree().parse("");
    }

    @Test(expected = JsonSyntaxException.class)
    public void invalidJSON() {
        new GsonTree().parse("This is not JSON");
    }

    @Test
    public void validFlat() {
        Tree<String, Object> tree = new GsonTree().parse("{\"a\": \"b\",\n\"c\": \"d\",\n\"e\": \"f\"\n}");
        assertThat(tree.root()).isNotEmpty();
    }

    @Test
    public void validComplex() {
        Tree<String, Object> tree = new GsonTree().parse("{\n"
// TODO support array
//                + "  \"array\": [\n"
//                + "    1,\n"
//                + "    2,\n"
//                + "    3\n"
//                + "  ],\n"
                + "  \"boolean\": true,\n"
                + "  \"null\": null,\n"
                + "  \"number\": 123,\n"
                + "  \"object\": {\n"
                + "    \"a\": \"b\",\n"
                + "    \"c\": \"d\",\n"
                + "    \"e\": \"f\"\n"
                + "  },\n"
                + "  \"string\": \"hello, world\"\n"
                + "}");
        assertThat(tree.root()).isNotEmpty();

        PropertiesBiConsumer kvConsumer = new PropertiesBiConsumer();
        Function<Sequence<String>, String> idsToKeyFunction = ids -> Joiner.on(".").join(ids);
        Function<String, Sequence<String>> keysToIdFunction = key -> new SequenceListImpl<>(
                Splitter.on(".").split(key));
        new TransformerImpl<>(idsToKeyFunction, keysToIdFunction).tree2kv(tree, kvConsumer);

        assertThat(kvConsumer.getProperties()).containsEntry("string", "hello, world");
        assertThat(kvConsumer.getProperties()).containsEntry("number", 123.0);
        assertThat(kvConsumer.getProperties()).containsEntry("boolean", true);
        assertThat(kvConsumer.getProperties()).containsKey("null");

        assertThat(kvConsumer.getProperties()).containsEntry("object.a", "b");
        // TODO assertThat(kvConsumer.getProperties()).containsEntry("array.1", "1");

        assertThat(kvConsumer.getProperties()).hasSize(8);
    }
}
