/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.test;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.truth.Truth.assertThat;

import ch.vorburger.dom2kv.Tree.NodeOrLeaf;
import ch.vorburger.dom2kv.impl.MapTreeBuilder;
import ch.vorburger.dom2kv.impl.TreeBuilderImpl;
import ch.vorburger.dom2kv.impl.TreeImpl;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit Test for {@link MapTreeBuilder}.
 *
 * @author Michael Vorburger.ch
 */
public class MapTreeBuilderTest {

    MapTreeBuilder<String, Object> builder = new MapTreeBuilder<>(() -> new TreeBuilderImpl<>());

    @Test
    public void emptyMap() {
        assertThat(builder.fromMap(Collections.emptyMap()).root()).isEmpty();
    }

    @Test
    public void simple() {
        Iterable<NodeOrLeaf<String, Object>> nodes = builder.fromMap(of("k1", "v1", "k2", "v2")).root();
        assertThat(nodes).containsExactly(new TreeImpl.LeafImpl<>("k1", "v1"), new TreeImpl.LeafImpl<>("k2", "v2"));
    }

    @Test
    @Ignore // TODO
    public void list() {
        Iterable<NodeOrLeaf<String, Object>> nodes = builder.fromMap(of("list", ImmutableList.of("a", "b"))).root();
        // TODO assertThat(nodes).containsExactly(???);
    }

    @Test
    public void mapOfMap() {
        Iterable<NodeOrLeaf<String, Object>> nodes = builder.fromMap(of("k1", "v1", "k2", of("x", "y"))).root();
        assertThat(nodes).containsExactly(
                new TreeImpl.LeafImpl<>("k1", "v1"),
                new TreeImpl.NodeImpl<>("k2", new TreeImpl.LeafImpl<>("x", "y")));
    }
}
