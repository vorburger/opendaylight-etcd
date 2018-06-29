/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.impl;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import ch.vorburger.dom2kv.Sequence;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Implementation of {@link Sequence} based on a {@link List}.
 *
 * @author Michael Vorburger.ch
 */
@NotThreadSafe
public class SequenceListImpl<T> implements Sequence<T> {

    private final List<T> delegate;

    public SequenceListImpl(Iterable<T> delegate) {
        this.delegate = ImmutableList.copyOf(requireNonNull(delegate, "delegate"));
    }

    @SafeVarargs
    public SequenceListImpl(T... list) {
        this(list.length > 0 ? asList(list) : ImmutableList.of());
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

    @Override
    public Sequence<T> append(T element) {
        ArrayList<T> newList = new ArrayList<>(delegate.size() + 1);
        newList.addAll(delegate);
        newList.add(element);
        return new SequenceListImpl<>(newList);
    }

    @Override
    public int hashCode() {
        return 31 + delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        SequenceListImpl<T> other = (SequenceListImpl<T>) obj;
        return delegate.equals(other.delegate);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
