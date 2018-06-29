/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.impl;

import ch.vorburger.dom2kv.Sequence;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Implementation of {@link Sequence} based on a {@link List}. This
 * implementation intentionally (for performance) does NOT guard against
 * external modification of the supplied list delegate.
 *
 * @author Michael Vorburger.ch
 */
@NotThreadSafe
public class SequenceListImpl<T> implements Sequence<T> {

    private final List<T> delegate;

    public SequenceListImpl(List<T> delegate) {
        this.delegate = delegate;
    }

    @SafeVarargs
    public SequenceListImpl(T... list) {
        this.delegate = Arrays.asList(list);
    }

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public T head() {
        if (isEmpty()) {
            throw new NoSuchElementException("Sequence is empty");
        }
        return delegate.get(0);
    }

    @Override
    public Sequence<T> tail() {
        if (isEmpty()) {
            return this;
        } else {
            return new SequenceListImpl<>(delegate.subList(1, delegate.size()));
        }
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

}
