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
import ch.vorburger.dom2kv.Tree.Leaf;
import ch.vorburger.dom2kv.Tree.Node;
import ch.vorburger.dom2kv.Tree.NodeOrLeaf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Implementation of {@link Transformer}.
 *
 * @author Michael Vorburger.ch
 */
public class TransformerImpl<I, K, V> implements Transformer<I, K, V> {

    private final Function<Iterable<I>, K> idsToKeyFunction;
    // private final BiFunction<K, Optional<V>, KeyValue<K, V>> keyValueFactory;

    public TransformerImpl(Function<Iterable<I>, K> idsToKeyFunction
            /*BiFunction<K, Optional<V>, KeyValue<K, V>> keyValueFactory*/) {
        this.idsToKeyFunction = idsToKeyFunction;
        // this.keyValueFactory = keyValueFactory;
    }

    @Override
    public void tree2kv(Tree<I, V> tree, BiConsumer<K, Optional<V>> kvConsumer) {
        tree.root().ifPresent(rootNode -> tree2kv(rootNode, kvConsumer, new ArrayList<>()));
    }

    @SuppressWarnings("unchecked")
    private void tree2kv(Node<I> node, BiConsumer<K, Optional<V>> kvConsumer, Collection<I> parentIDs) {
        // TODO thisNodeFQN can be significantly optimized.. use some sort of smarter Sequence+1 type
        List<I> thisNodeFQN = new ArrayList<>(parentIDs);
        thisNodeFQN.add(node.id());
        K key = idsToKeyFunction.apply(thisNodeFQN);

        kvConsumer.accept(key, Optional.empty());
        for (NodeOrLeaf<I> child : node.children()) {
            if (child instanceof Node) {
                tree2kv((Node<I>) child, kvConsumer, thisNodeFQN);
            } else if (child instanceof Leaf) {
                tree2kv((Leaf<I, V>) child, kvConsumer, thisNodeFQN);
            } else {
                throw new IllegalArgumentException("Unknown NodeOrLeaf sub-type: " + child.getClass());
            }
        }
    }

    private void tree2kv(Leaf<I, V> leaf, BiConsumer<K, Optional<V>> kvConsumer, Collection<I> parentIDs) {
        // see above (and don't copy/paste....)
        List<I> thisNodeFQN = new ArrayList<>(parentIDs);
        thisNodeFQN.add(leaf.id());
        K key = idsToKeyFunction.apply(thisNodeFQN);

        kvConsumer.accept(key, Optional.of(leaf.value()));
    }

    @Override
    public Tree<I, V> kv2tree(Iterable<KeyValue<K, V>> keysAndValues) {
        return new TreeImpl<>(); // TODO implement!
    }
}
