/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.impl;

import ch.vorburger.dom2kv.Tree;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.concurrent.Immutable;

/**
 * An implementation of {@link Tree} (but there could well be others, more optimized).
 *
 * @author Michael Vorburger.ch
 */
@Immutable
public class TreeImpl<I, V> implements Tree<I, V> {

    private final Optional<Node<I>> rootNode;

    public TreeImpl() {
        this.rootNode = Optional.empty();
    }

    public TreeImpl(Node<I> rootNode) {
        this.rootNode = Optional.of(rootNode);
    }

    @Override
    public Optional<Node<I>> root() {
        return rootNode;
    }

    @Override
    public int hashCode() {
        return 31 + rootNode.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        @SuppressWarnings("unchecked")
        TreeImpl<I, V> other = (TreeImpl<I, V>) obj;
        return rootNode.equals(other.rootNode);
    }


    public static class NodeImpl<I> implements Node<I> {

        private final I id;
        private final NodeOrLeaf<I>[] children;

        @SafeVarargs
        public NodeImpl(I id, NodeOrLeaf<I>... children) {
            this.id = Objects.requireNonNull(id, "id");
            this.children = children;
        }

        @Override
        public I id() {
            return id;
        }

        @Override
        public Iterator<NodeOrLeaf<I>> children() {
            return Arrays.stream(children).iterator();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(children);
            result = prime * result + id.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            @SuppressWarnings("unchecked")
            NodeImpl<I> other = (NodeImpl<I>) obj;
            if (!Arrays.equals(children, other.children)) {
                return false;
            }
            return id.equals(other.id);
        }
    }

    public static class LeafImpl<I, V> implements Leaf<I, V> {

        private final I id;
        private final V value;

        public LeafImpl(I id, V value) {
            this.id = Objects.requireNonNull(id, "id");
            this.value = Objects.requireNonNull(value, "value");
        }

        @Override
        public I id() {
            return id;
        }

        @Override
        public V value() {
            return value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + id.hashCode();
            result = prime * result + value.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            @SuppressWarnings("unchecked")
            LeafImpl<I, V> other = (LeafImpl<I, V>) obj;
            if (!id.equals(other.id)) {
                return false;
            }
            return value.equals(other.value);
        }


    }
}
