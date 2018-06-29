/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Service to transform Documentation Object Model Tree to/from Keys & Values.
 *
 * @author Michael Vorburger.ch
 */
public interface Transformer {

    void tree2kv(Tree tree, Consumer<KeyValue> kvConsumer);

    Tree kv2tree(Iterator<KeyValue> keysAndValues);

}
