/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv;

import java.util.NoSuchElementException;

/**
 * Sequence data type - heads or tails.
 *
 * @author Michael Vorburger.ch
 */
public interface Sequence<T> extends Iterable<T> {

    /**
     * Get the Head of this Sequence.
     * @return head (never null)
     * @throws NoSuchElementException if {@link #isEmpty()}.
     */
    T head();

    /**
     * Get the Tail of this Sequence.
     * @return another Sequence (may be <code>this</code> if {@link #isEmpty()}, but doesn't have to be)
     */
    Sequence<T> tail();

    /**
     * Returns <tt>true</tt> if this Sequence contains no elements.
     *
     * @return <tt>true</tt> if this Sequence contains no elements
     */
    boolean isEmpty();

    // TODO MutableSequence extends Sequence: Sequence<T> add(T element);
}
