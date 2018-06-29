/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv;

/**
 * <a href="https://en.wikipedia.org/wiki/Dom_(mountain)">DOM</a> Tree.
 *
 * @author Michael Vorburger.ch
 */
public interface Tree<I, V> {

    // TODO Think through how to support List... value() of Leaf should List<V> ??

    interface NodeOrLeaf<I, V> {
        I id();
    }

    interface Node<I, V> extends NodeOrLeaf<I, V> {
        Iterable<NodeOrLeaf<I, V>> children();
    }

    interface Leaf<I, V> extends NodeOrLeaf<I, V> {
        V value();
    }

    Iterable<NodeOrLeaf<I, V>> root();
}
