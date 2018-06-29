/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.impl;

import ch.vorburger.dom2kv.Sequence;
import ch.vorburger.dom2kv.Tree;
import ch.vorburger.dom2kv.TreeBuilder;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Builds a {@link Tree} given a {@link Map}.
 *
 * @author Michael Vorburger.ch
 */
public class MapTreeBuilder<I, V> {

    private final Supplier<TreeBuilder<I, V>> treeBuilderFactory;

    public MapTreeBuilder(Supplier<TreeBuilder<I, V>> treeBuilderFactory) {
        this.treeBuilderFactory = treeBuilderFactory;
    }

    public Tree<I, V> fromMap(Map<I, Object> map) {
        TreeBuilder<I, V> treeBuilder = treeBuilderFactory.get();
        fromMap(map, treeBuilder);
        return treeBuilder.build();
    }

    public void fromMap(Map<I, Object> map, TreeBuilder<I, V> treeBuilder) {
        transform(map, treeBuilder, new SequenceListImpl<>());
    }

    private void transform(Map<I, Object> innerMap, TreeBuilder<I, V> treeBuilder, Sequence<I> parentIDs) {
        innerMap.forEach((id, value) -> {
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<I, Object> valueAsMap = (Map<I, Object>) value;
                transform(valueAsMap, treeBuilder, parentIDs.append(id));
            } else {
                @SuppressWarnings("unchecked")
                V nonMapValue = (V) value;
                treeBuilder.createLeaf(parentIDs.append(id), nonMapValue);
            }
        });
    }
}
