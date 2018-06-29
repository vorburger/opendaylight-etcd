/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv;

import java.util.Optional;
import java.util.function.BiConsumer;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Service to transform Documentation Object Model Tree to/from Keys & Values.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
public interface Transformer<I, K, V> {

    void tree2kv(Tree<I, V> tree, BiConsumer<K, Optional<V>> kvConsumer);

    Tree<I, V> kv2tree(Iterable<KeyValue<K, V>> kvs);

}
