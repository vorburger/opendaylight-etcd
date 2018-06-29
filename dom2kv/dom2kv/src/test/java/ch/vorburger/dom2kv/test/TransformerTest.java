/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.test;

import static com.google.common.truth.Truth.assertThat;

import ch.vorburger.dom2kv.Transformer;
import ch.vorburger.dom2kv.Tree;
import ch.vorburger.dom2kv.impl.KeyValueImpl;
import ch.vorburger.dom2kv.impl.SequenceListImpl;
import ch.vorburger.dom2kv.impl.TransformerImpl;
import ch.vorburger.dom2kv.impl.TreeImpl;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.Collections;
import org.junit.Test;

/**
 * Unit test for {@link Transformer} implementations.
 *
 * @author Michael Vorburger.ch
 */
public class TransformerTest {

    ListBiConsumer<String, String> kvs = new ListBiConsumer<>();

    Transformer<String, String, String> transformer = new TransformerImpl<>(
        ids -> Joiner.on(".").join(ids),
        key -> new SequenceListImpl<>(Splitter.on(".").split(key)));

    @Test public void empty() {
        assertThat(transformer.kv2tree(Collections.emptyList()).root()).isEmpty();

        Tree<String, String> emptyTree = new TreeImpl<>();
        transformer.tree2kv(emptyTree, kvs);
        assertThat(kvs).isEmpty();

        assertThat(transformer.kv2tree(kvs)).isEqualTo(emptyTree);
    }

    @Test public void root() {
        Tree<String, String> tree = new TreeImpl<>(new TreeImpl.NodeImpl<String, String>("groot"));
        transformer.tree2kv(tree, kvs);
        assertThat(kvs).containsExactly(new KeyValueImpl<>("groot"));
        assertThat(transformer.kv2tree(kvs)).isEqualTo(tree);
    }


    // TODO add more tests...
}
