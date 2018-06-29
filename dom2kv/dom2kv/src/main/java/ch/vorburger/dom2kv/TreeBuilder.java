/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Builder for {@link Tree}.
 *
 * @author Michael Vorburger.ch
 */
@NotThreadSafe
public interface TreeBuilder<I, V> {

    TreeBuilder<I, V> createNode(Sequence<I> path);

    TreeBuilder<I, V> createLeaf(Sequence<I> path, V value);

    Tree<I, V> build();
}
