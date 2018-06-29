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
import ch.vorburger.dom2kv.Tree.NodeOrLeaf;
import ch.vorburger.dom2kv.TreeBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.errorprone.annotations.Var;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An implementation of {@link TreeBuilder} (but there could well be others, more optimized).
 *
 * @author Michael Vorburger.ch
 */
@NotThreadSafe
public class TreeBuilderImpl<I, V> implements TreeBuilder<I, V> {

    private static class MutableNodeOrLeaf<I, V> {
        final Map<I, MutableNodeOrLeaf<I, V>> children = new HashMap<>();
        final Optional<V> value;

        MutableNodeOrLeaf(V value) {
            this.value = Optional.of(value);
        }

        MutableNodeOrLeaf() {
            this.value = Optional.empty();
        }
    }

    private final MutableNodeOrLeaf<I, V> root = new MutableNodeOrLeaf<>();

    @Override
    public TreeBuilder<I, V> createNode(Sequence<I> path) {
        create(path, (remainingPath, current) ->
            current.children.computeIfAbsent(remainingPath.head(), id -> new MutableNodeOrLeaf<>()));
        return this;
    }

    @Override
    public TreeBuilder<I, V> createLeaf(Sequence<I> path, V value) {
        create(path, (remainingPath, current) -> {
            if (remainingPath.tail().isEmpty()) {
                return current.children.computeIfAbsent(remainingPath.head(), id -> new MutableNodeOrLeaf<>(value));
            } else {
                return current.children.computeIfAbsent(remainingPath.head(), id -> new MutableNodeOrLeaf<>());
            }
        });
        return this;
    }

    private void create(Sequence<I> path, BiFunction1<Sequence<I>, MutableNodeOrLeaf<I, V>> biFunction) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("path is empty Sequence");
        }
        @Var Sequence<I> remainingPath = path;
        @Var MutableNodeOrLeaf<I, V> current = root;

        while (!remainingPath.isEmpty()) {
//            if (current.children.containsKey(remainingPath.head())) {
//                throw new IllegalArgumentException("Path already set: " + path);
//            }
            current = biFunction.apply(remainingPath, current);
            remainingPath = remainingPath.tail();
        }
    }

    @Override
    public Tree<I, V> build() {
        if (root.children.isEmpty()) {
            return new TreeImpl<>();
        }
        if (root.value.isPresent()) {
            throw new IllegalStateException();
        }
        return new TreeImpl<>(transform(root.children));
    }

    private Iterable<NodeOrLeaf<I, V>> transform(Map<I, MutableNodeOrLeaf<I, V>> map) {
        Builder<NodeOrLeaf<I, V>> listBuilder = ImmutableList.<NodeOrLeaf<I, V>>builderWithExpectedSize(map.size());
        map.forEach((id, mutableNodeOrLeaf) -> {
            if (mutableNodeOrLeaf.value.isPresent()) {
                listBuilder.add(new TreeImpl.LeafImpl<>(id, mutableNodeOrLeaf.value.get()));
            } else {
                listBuilder.add(new TreeImpl.NodeImpl<>(id, transform(mutableNodeOrLeaf.children)));
            }
        });
        return listBuilder.build();
    }

    private interface BiFunction1<T, U> extends BiFunction<T, U, U> {
    }
}
