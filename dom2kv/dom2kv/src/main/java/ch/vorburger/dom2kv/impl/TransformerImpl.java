/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.impl;

import ch.vorburger.dom2kv.KeyValue;
import ch.vorburger.dom2kv.Transformer;
import ch.vorburger.dom2kv.Tree;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Implementation of {@link Transformer}.
 *
 * @author Michael Vorburger.ch
 */
public class TransformerImpl<I, K, V> implements Transformer<I, K, V> {

    private BiFunction<K, V, KeyValue<K, V>> keyValueFactory;

    @Override
    public void tree2kv(Tree<I, V> tree, Consumer<KeyValue<K, V>> kvConsumer) {
        tree.root().ifPresent(rootNode -> { });
    }

    @Override
    public Tree<I, V> kv2tree(Iterator<KeyValue<K, V>> keysAndValues) {
        return new TreeImpl<>();
    }
}
