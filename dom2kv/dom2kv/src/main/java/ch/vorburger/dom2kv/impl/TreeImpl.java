/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.impl;

import ch.vorburger.dom2kv.Tree;
import java.util.Optional;

/**
 * An implementation of {@link Tree} (but there could well be others, more optimized).
 *
 * @author Michael Vorburger.ch
 */
public class TreeImpl<I, V> implements Tree<I, V> {

    private final Optional<Node<I>> rootNode;

    public TreeImpl() {
        this(Optional.empty());
    }

    public TreeImpl(Optional<Node<I>> rootNode) {
        this.rootNode = rootNode;
    }

    @Override
    public Optional<Node<I>> root() {
        return rootNode;
    }
}
