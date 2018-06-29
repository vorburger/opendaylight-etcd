/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.impl;

import ch.vorburger.dom2kv.Tree;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An implementation of {@link Tree} (but there could well be others, more optimized).
 *
 * @author Michael Vorburger.ch
 */
@Immutable
@ThreadSafe
public class TreeImpl<I, V> implements Tree<I, V> {

    private final Iterable<NodeOrLeaf<I, V>> rootNodes;

    public TreeImpl() {
        this.rootNodes = Collections.emptyList();
    }

    public TreeImpl(Iterable<NodeOrLeaf<I, V>> rootNodes) {
        this.rootNodes = ImmutableList.copyOf(rootNodes);
    }

    @Override
    public Iterable<NodeOrLeaf<I, V>> root() {
        return rootNodes;
    }

    @Override
    public int hashCode() {
        return 31 + rootNodes.hashCode();
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
        return rootNodes.equals(other.rootNodes);
    }


    public static class NodeImpl<I, V> implements Node<I, V> {

        private final I id;
        private final Iterable<NodeOrLeaf<I, V>> children;

        public NodeImpl(I id, Iterable<NodeOrLeaf<I, V>> children) {
            this.id = Objects.requireNonNull(id, "id");
            this.children = ImmutableList.copyOf(children);
        }

        @SafeVarargs
        public NodeImpl(I id, NodeOrLeaf<I, V>... children) {
            this(id, ImmutableList.copyOf(children));
        }

        @Override
        public I id() {
            return id;
        }

        @Override
        public Iterable<NodeOrLeaf<I, V>> children() {
            return children;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + children.hashCode();
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
            NodeImpl<I, V> other = (NodeImpl<I, V>) obj;
            if (!children.equals(other.children)) {
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
