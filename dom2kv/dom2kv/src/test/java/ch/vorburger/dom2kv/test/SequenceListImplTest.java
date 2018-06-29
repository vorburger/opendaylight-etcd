/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.test;

import static com.google.common.truth.Truth.assertThat;

import ch.vorburger.dom2kv.impl.SequenceListImpl;
import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * Unit test for {@link SequenceListImpl}.
 *
 * @author Michael Vorburger.ch
 */
public class SequenceListImplTest {

    @Test public void empty() {
        assertThat(new SequenceListImpl<>().isEmpty()).isTrue();
        assertThat(new SequenceListImpl<>().tail().isEmpty()).isTrue();
    }

    @Test(expected = NoSuchElementException.class) public void emptyHead() {
        new SequenceListImpl<>().head();
    }

    @Test public void one() {
        assertThat(new SequenceListImpl<>("hello").isEmpty()).isFalse();
        assertThat(new SequenceListImpl<>("hello").head()).isEqualTo("hello");
        assertThat(new SequenceListImpl<>("hello").tail().isEmpty()).isTrue();
    }

    @Test public void two() {
        assertThat(new SequenceListImpl<>("hello", "world").isEmpty()).isFalse();
        assertThat(new SequenceListImpl<>("hello", "world").head()).isEqualTo("hello");
        assertThat(new SequenceListImpl<>("hello", "world").tail().head()).isEqualTo("world");
        assertThat(new SequenceListImpl<>("hello", "world").tail().tail().isEmpty()).isTrue();
    }
}
