/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.impl;

import ch.vorburger.dom2kv.ByteSeq;
import ch.vorburger.dom2kv.KeyValue;
import ch.vorburger.dom2kv.Transformer;
import ch.vorburger.dom2kv.Tree;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Implementation of {@link Transformer}.
 *
 * @author Michael Vorburger.ch
 */
public class TransformerImpl implements Transformer {

    private BiFunction<ByteSeq, ByteSeq, KeyValue> keyValueFactory;

    @Override
    public void tree2kv(Tree tree, Consumer<KeyValue> kvConsumer) {
    }

    @Override
    public Tree kv2tree(Iterator<KeyValue> keysAndValues) {
        return null;
    }

}
