/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.impl;

import ch.vorburger.dom2kv.KeyValue;
import ch.vorburger.dom2kv.Sequence;
import ch.vorburger.dom2kv.Transformer;
import ch.vorburger.dom2kv.Tree;
import ch.vorburger.dom2kv.Tree.Leaf;
import ch.vorburger.dom2kv.Tree.Node;
import ch.vorburger.dom2kv.Tree.NodeOrLeaf;
import ch.vorburger.dom2kv.TreeBuilder;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of {@link Transformer}.
 *
 * @author Michael Vorburger.ch
 */
public class TransformerImpl<I, K, V> implements Transformer<I, K, V> {

    private final Supplier<Sequence<I>> newEmptySequenceProvider;
    private final Function<Sequence<I>, K> idsToKeyFunction;
    private final Function<K, Sequence<I>> keysToIdFunction;
    private final Supplier<TreeBuilder<I, V>> newTreeBuilderProvider;

    public TransformerImpl(Supplier<Sequence<I>> newEmptySequenceProvider,
            Function<Sequence<I>, K> idsToKeyFunction,
            Function<K, Sequence<I>> keysToIdFunction, Supplier<TreeBuilder<I, V>> newTreeBuilderProvider) {
        this.newEmptySequenceProvider = newEmptySequenceProvider;
        this.idsToKeyFunction = idsToKeyFunction;
        this.keysToIdFunction = keysToIdFunction;
        this.newTreeBuilderProvider = newTreeBuilderProvider;
    }

    public TransformerImpl(Function<Sequence<I>, K> idsToKeyFunction,
            Function<K, Sequence<I>> keysToIdFunction, Supplier<TreeBuilder<I, V>> newTreeBuilderProvider) {
        this(() -> new SequenceListImpl<>(), idsToKeyFunction, keysToIdFunction, newTreeBuilderProvider);
    }

    public TransformerImpl(Function<Sequence<I>, K> idsToKeyFunction, Function<K, Sequence<I>> keysToIdFunction) {
        this(() -> new SequenceListImpl<>(), idsToKeyFunction, keysToIdFunction, () -> new TreeBuilderImpl<>());
    }

    @Override
    public void tree2kv(Tree<I, V> tree, BiConsumer<K, Optional<V>> kvConsumer) {
        tree2kv(tree.root(), kvConsumer, newEmptySequenceProvider.get());
    }

    private void tree2kv(Iterable<NodeOrLeaf<I, V>> nodesOrLeafs, BiConsumer<K, Optional<V>> kvConsumer,
            Sequence<I> parentIDs) {
        for (NodeOrLeaf<I, V> child : nodesOrLeafs) {
            if (child instanceof Node) {
                tree2kv((Node<I, V>) child, kvConsumer, parentIDs.append(child.id()));
            } else if (child instanceof Leaf) {
                tree2kv((Leaf<I, V>) child, kvConsumer, parentIDs.append(child.id()));
            } else {
                throw new IllegalArgumentException("Unknown NodeOrLeaf sub-type: " + child.getClass());
            }
        }
    }

    private void tree2kv(Node<I, V> node, BiConsumer<K, Optional<V>> kvConsumer, Sequence<I> parentIDs) {
        K key = idsToKeyFunction.apply(parentIDs);
        kvConsumer.accept(key, Optional.empty());
        tree2kv(node.children(), kvConsumer, parentIDs);
    }

    private void tree2kv(Leaf<I, V> leaf, BiConsumer<K, Optional<V>> kvConsumer, Sequence<I> parentIDs) {
        K key = idsToKeyFunction.apply(parentIDs);
        kvConsumer.accept(key, Optional.of(leaf.value()));
    }

    @Override
    public Tree<I, V> kv2tree(Iterable<KeyValue<K, V>> keysAndValues) {
        TreeBuilder<I, V> treeBuilder = newTreeBuilderProvider.get();
        for (KeyValue<K, V> kv : keysAndValues) {
            Sequence<I> id = keysToIdFunction.apply(kv.key());
            Optional<V> value = kv.value();
            if (value.isPresent()) {
                treeBuilder.createLeaf(id, value.get());
            } else {
                treeBuilder.createNode(id);
            }
        }
        return treeBuilder.build();
    }
}
