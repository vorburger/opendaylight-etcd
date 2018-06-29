/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.test;

import static com.google.common.truth.Truth.assertThat;

import ch.vorburger.dom2kv.Tree;
import ch.vorburger.dom2kv.Tree.Node;
import ch.vorburger.dom2kv.Tree.NodeOrLeaf;
import ch.vorburger.dom2kv.TreeBuilder;
import ch.vorburger.dom2kv.impl.SequenceListImpl;
import ch.vorburger.dom2kv.impl.TreeBuilderImpl;
import java.util.Iterator;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit test for {@link TreeBuilder} implementations.
 *
 * @author Michael Vorburger.ch
 */
public class TreeBuilderImplTest {

    TreeBuilder<String, String> treeBuilder = new TreeBuilderImpl<>();

    @Test public void empty() {
        assertThat(treeBuilder.build().root()).isEmpty();
    }

    @Test(expected = IllegalArgumentException.class) public void emptyPath() {
        treeBuilder.createNode(new SequenceListImpl<>());
    }

    @Test public void root() {
        Iterable<NodeOrLeaf<String, String>> nodes = treeBuilder.createNode(new SequenceListImpl<>("/")).build().root();
        assertThat(nodes).isNotEmpty();
        assertThat(nodes.iterator().next().id()).isEqualTo("/");

        Node<String, String> node1 = (Node<String, String>)nodes.iterator().next();
        assertThat(node1.children()).isEmpty();
    }

    @Test public void more() {
        Tree<String, String> tree = treeBuilder
                .createNode(new SequenceListImpl<>("/", "dir1"))
                // must work without this: .createNode(new SequenceListImpl<>("/", "dir2"))
                .createLeaf(new SequenceListImpl<>("/", "dir2", "file"), "...")
                .build();

        assertThat(tree.root()).isNotEmpty();
        Node<String, String> node0 = (Node<String, String>)tree.root().iterator().next();
        assertThat(node0.id()).isEqualTo("/");
        assertThat(node0.children()).hasSize(2);

        Iterator<NodeOrLeaf<String, String>> iterator = node0.children().iterator();
        NodeOrLeaf<String, String> node1 = iterator.next();
        assertThat(node1).isInstanceOf(Tree.Node.class);
        Node<String, String> dir1 = (Node<String, String>) node1;
        assertThat(dir1.id()).isEqualTo("dir2");

        NodeOrLeaf<String, String> dir2 = iterator.next();
        assertThat(dir2.id()).isEqualTo("dir1");

        assertThat(dir1.children()).hasSize(1);
        NodeOrLeaf<String, String> file = dir1.children().iterator().next();
        assertThat(file.id()).isEqualTo("file");
        assertThat(file).isInstanceOf(Tree.Leaf.class);
        assertThat(((Tree.Leaf<String, String>) file).value()).isEqualTo("...");
    }

    @Ignore // TODO broken, make work; if this ever is useful? (for tests; the API allows it, for other impls)
    @Test public void leafsOnRoot() {
        Tree<String, String> tree = treeBuilder
                .createLeaf(new SequenceListImpl<>(), "a")
                .createLeaf(new SequenceListImpl<>(), "b")
                .build();
        assertThat(tree.root()).hasSize(2);
        assertThat(tree.root().iterator().next()).isInstanceOf(Tree.Leaf.class);
    }

    @Test(expected = IllegalArgumentException.class) public void idReuseNodeLeaf() {
        treeBuilder
                .createNode(new SequenceListImpl<>("/", "dir1"))
                .createLeaf(new SequenceListImpl<>("/", "dir1"), "...")
                .build();
    }

    @Ignore // this is a little more tricky to detect with the implementation as-is
    @Test(expected = IllegalArgumentException.class) public void idReuseNodeNode() {
        treeBuilder
                .createNode(new SequenceListImpl<>("/", "dir1"))
                .createNode(new SequenceListImpl<>("/", "dir1"))
                .build();
    }
}
