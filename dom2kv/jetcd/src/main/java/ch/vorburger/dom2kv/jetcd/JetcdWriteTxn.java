/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.jetcd;

import ch.vorburger.dom2kv.Transformer;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.PutOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Open etcd write transaction.
 * Implements BiConsumer to be suitable for {@link Transformer#tree2kv(ch.vorburger.dom2kv.Tree, BiConsumer)}.
 *
 * @author Michael Vorburger.ch
 */
public class JetcdWriteTxn implements BiConsumer<ByteSequence, Optional<ByteSequence>> {

    private static final ByteSequence EMPTY = ByteSequence.from(new byte[0]);

    private final Txn etcdTXn;

    public JetcdWriteTxn(KV etcdKV) {
        this.etcdTXn = etcdKV.txn();
    }

    // TODO abstract away dealing with TxnResponse?
    public CompletableFuture<TxnResponse> commit() {
        return etcdTXn.commit();
    }

    public void put(ByteSequence key, ByteSequence value) {
        // TODO etcdTXn.put how-to?
        // ? etcdTXn.If(Cmp.Op.EQUAL);
        etcdTXn.Then(Op.put(key, value, PutOption.DEFAULT));

    }

    @Override
    public void accept(ByteSequence key, Optional<ByteSequence> value) {
        put(key, value.orElse(EMPTY));
    }
}
