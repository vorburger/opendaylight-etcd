/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.test;

import static com.google.common.truth.Truth.assertThat;

import ch.vorburger.dom2kv.KeyValue;
import ch.vorburger.dom2kv.Transformer;
import ch.vorburger.dom2kv.Tree;
import ch.vorburger.dom2kv.impl.TransformerImpl;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;

/**
 * Unit test for {@link Transformer} implementations.
 *
 * @author Michael Vorburger.ch
 */
public class TransformerTest {

    Transformer<String, String, String> transformer = new TransformerImpl<>();

    @Test public void empty() {
        assertThat(transformer.kv2tree(Collections.emptyIterator()).root().isPresent()).isFalse();

        ListConsumer<KeyValue<String, String>> kvConsumer = new ListConsumer<>();
        Tree<String, String> emptyTree = () -> Optional.empty();
        transformer.tree2kv(emptyTree, kvConsumer);
        assertThat(kvConsumer.getList()).isEmpty();
    }

}
