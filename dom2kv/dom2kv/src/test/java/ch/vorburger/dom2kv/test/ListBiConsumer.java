/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.test;

import ch.vorburger.dom2kv.KeyValue;
import ch.vorburger.dom2kv.impl.KeyValueImpl;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * {@link BiConsumer} which stores elements as {@link KeyValue} in a list, for tests.
 *
 * @author Michael Vorburger.ch
 */
public class ListBiConsumer<K, V> implements BiConsumer<K, Optional<V>>, Iterable<KeyValue<K, V>> {

    private final List<KeyValue<K, V>> list = new ArrayList<>();

    @Override
    public void accept(K key, Optional<V> value) {
        list.add(new KeyValueImpl<>(key, value));
    }

    @Override
    public Iterator<KeyValue<K, V>> iterator() {
        return list.iterator();
    }

}
