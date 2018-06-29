/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.jetcd;

import ch.vorburger.dom2kv.Transformer;
import com.coreos.jetcd.data.ByteSequence;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Open etcd write transaction.
 * Implements BiConsumer to be suitable for {@link Transformer#tree2kv(ch.vorburger.dom2kv.Tree, BiConsumer)}.
 *
 * @author Michael Vorburger.ch
 */
public class JetcdWriteTxn implements BiConsumer<ByteSequence, Optional<ByteSequence>> {

    @Override
    public void accept(ByteSequence key, Optional<ByteSequence> value) {
    }

}
