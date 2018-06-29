/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv;

import java.util.Iterator;

/**
 * <a href="https://en.wikipedia.org/wiki/Dom_(mountain)">DOM</a> Tree.
 *
 * @author Michael Vorburger.ch
 */
public interface Tree {
    // TODO make type of ID and Value generics

    interface NodeOrLeaf {
        ByteSeq id();
    }

    interface Node extends NodeOrLeaf {
        Iterator<NodeOrLeaf> children();
    }

    interface Leaf extends NodeOrLeaf {
        ByteSeq value();
    }

    Node root();
}
