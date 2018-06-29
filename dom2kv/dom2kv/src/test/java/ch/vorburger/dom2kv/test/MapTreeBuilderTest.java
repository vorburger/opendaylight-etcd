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
import java.util.Collections;
import org.junit.Test;

/**
 * Unit Test for {@link MapTreeBuilder}.
 *
 * @author Michael Vorburger.ch
 */
public class MapTreeBuilderTest {

    MapTreeBuilder<String, Object> mapTreeBuilder = new MapTreeBuilder<>(() -> new TreeBuilderImpl<>());

    @Test
    public void emptyMap() {
        assertThat(mapTreeBuilder.fromMap(Collections.emptyMap()).root()).isEmpty();
    }

    @Test
    public void simple() {
        Iterable<NodeOrLeaf<String, Object>> nodes = mapTreeBuilder.fromMap(of("k1", "v1", "k2", "v1")).root();
        assertThat(nodes).hasSize(2);
        assertThat(nodes).contains(new TreeImpl.LeafImpl<>("k1", "v1"));
    }

    @Test
    public void mapOfMap() {
        Iterable<NodeOrLeaf<String, Object>> nodes = mapTreeBuilder.fromMap(of("k1", "v1", "k2", of("x", "y"))).root();
        assertThat(nodes).hasSize(2);
        assertThat(nodes).contains(new TreeImpl.LeafImpl<>("k1", "v1"));
        assertThat(nodes).contains(new TreeImpl.NodeImpl<>("k2", new TreeImpl.LeafImpl<>("x", "y")));
    }
}
