/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.impl;

import ch.vorburger.dom2kv.KeyValue;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Implementation of {@link KeyValue}.
 *
 * @author Michael Vorburger.ch
 */
@Immutable
@ThreadSafe
public final class KeyValueImpl<K, V> implements KeyValue<K, V> {

    private final @NonNull K key;
    private final @NonNull Optional<V> value;

    public KeyValueImpl(K key, V value) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = Optional.of(Objects.requireNonNull(value, "value"));
    }

    public KeyValueImpl(K key) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = Optional.empty();
    }

    public KeyValueImpl(K key, Optional<V> value) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    public K key() {
        return key;
    }

    @Override
    public Optional<V> value() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (key == null ? 0 : key.hashCode());
        result = prime * result + (value == null ? 0 : value.hashCode());
        return result;
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
        KeyValueImpl<K, V> other = (KeyValueImpl<K, V>) obj;
        if (!key.equals(other.key)) {
            return false;
        }
        if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "KeyValueImpl [key=" + key + ", value=" + value + "]";
    }
}
