/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.utils;

import static org.opendaylight.etcd.utils.ByteSequences.toStringable;

import com.coreos.jetcd.KV;
import com.coreos.jetcd.Txn;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.CompactResponse;
import com.coreos.jetcd.kv.DeleteResponse;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.kv.PutResponse;
import com.coreos.jetcd.kv.TxnResponse;
import com.coreos.jetcd.op.Cmp;
import com.coreos.jetcd.op.Op;
import com.coreos.jetcd.options.CompactOption;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.common.base.MoreObjects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

/**
 * {@link KV} wrapper which logs.
 *
 * @author Michael Vorburger.ch
 */
public class LoggingKV implements KV {

    // NB: This has bad performance (due to asString)
    // TODO make use of this wrapper a configurable option

    // TODO upstream this into jetcd

    private static final Logger LOG = LoggerFactory.getLogger(LoggingKV.class);

    private final AtomicLong counter = new AtomicLong();
    private final String prefix;
    private final KV delegate;

    public LoggingKV(String prefix, KV delegate) {
        this.prefix = prefix;
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<CompactResponse> compact(long rev) {
        long id = counter.incrementAndGet();
        LOG.info("{}#{} compact: {}", prefix, id, rev);
        return delegate.compact(rev).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id));
    }

    @Override
    public CompletableFuture<CompactResponse> compact(long rev, CompactOption option) {
        long id = counter.incrementAndGet();
        LOG.info("{}#{} compact: {} ({})", prefix, id, rev, asString(option));
        return delegate.compact(rev, option).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id));
    }

    @Override
    public CompletableFuture<DeleteResponse> delete(ByteSequence key) {
        long id = counter.incrementAndGet();
        LOG.info("{}#{} delete: {}", prefix, id, toStringable(key));
        return delegate.delete(key).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id));
    }

    @Override
    public CompletableFuture<DeleteResponse> delete(ByteSequence key, DeleteOption option) {
        long id = counter.incrementAndGet();
        LOG.info("{}#{} delete: {} ({})", prefix, id, toStringable(key), asString(option));
        return delegate.delete(key, option).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id));
    }

    @Override
    public CompletableFuture<GetResponse> get(ByteSequence key) {
        long id = counter.incrementAndGet();
        LOG.info("{}#{} get: {}", prefix, id, toStringable(key));
        return delegate.get(key).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id,
            getResponse -> MessageFormatter.arrayFormat("#{} got: {}",
                new Object[] { id, asString(getResponse) }).getMessage()));
    }

    @Override
    public CompletableFuture<GetResponse> get(ByteSequence key, GetOption option) {
        long id = counter.incrementAndGet();
        LOG.info("{}#{} get: {} ({})", prefix, id, toStringable(key), asString(option));
        return delegate.get(key, option).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id,
            getResponse -> MessageFormatter.arrayFormat("#{} got: {}",
                new Object[] { id, asString(getResponse) })
                .getMessage()));
    }

    // TODO jetcd GetOption & Co. really should just have a working toString() - contribute a fix PR there upstream!
    private static String asString(GetOption option) {
        return MoreObjects.toStringHelper(option)
                // TODO avoid including fields with default value, to keep it shorter
                .add("isCountOnly", option.isCountOnly())
                .add("isKeysOnly", option.isKeysOnly())
                .add("isSerializable", option.isSerializable())
                .add("revision", option.getRevision())
                .add("endKey", option.getEndKey().map(endKey -> ByteSequences.asString(endKey)))
                .add("limit", option.getLimit())
                .add("sortField", option.getSortField()) // TODO probably NOK
                .add("sortOrder", option.getSortOrder()) // TODO probably NOK
                .toString();
    }

    private static String asString(PutOption option) {
        return MoreObjects.toStringHelper(option)
                .add("leaseId", option.getLeaseId())
                .add("prevKV", option.getPrevKV())
                .toString();
    }

    private static String asString(DeleteOption option) {
        return MoreObjects.toStringHelper(option)
                .add("endKey", option.getEndKey())
                .add("prevKV", option.isPrevKV())
                .toString();
    }

    private static String asString(CompactOption option) {
        return MoreObjects.toStringHelper(option)
                .add("physical", option.isPhysical())
                .toString();
    }

    private static String asString(GetResponse getResponse) {
        StringBuilder sb = new StringBuilder("count=" + getResponse.getCount() + ", KVs=[");
        if (!getResponse.getKvs().isEmpty()) {
            sb.append('\n');
        }
        getResponse.getKvs().forEach(kv -> {
            KeyValues.append(sb, kv);
            sb.append('\n');
        });
        sb.append(']');
        return sb.toString();
    }

    @Override
    public CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value) {
        long id = counter.incrementAndGet();
        LOG.info("{}#{} put: {} ➠ {}", prefix, id, toStringable(key), toStringable(value));
        return delegate.put(key, value).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id));
    }

    @Override
    public CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value, PutOption option) {
        long id = counter.incrementAndGet();
        LOG.info("{}#{} put: {} ➠ {} ({})", prefix, id, toStringable(key), toStringable(value), asString(option));
        return delegate.put(key, value).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id));
    }

    @Override
    public Txn txn() {
        long id = counter.incrementAndGet();
        LOG.info("{}#{} txn...", prefix, id);
        return new LoggingTxn(id, delegate.txn());
    }

    private class LoggingTxn implements Txn {

        private final long id;
        private final Txn delegateTxn;
        private final List<Cmp> allCmps;
        private final List<Op> allThenOps;
        private final List<Op> allElseOps;

        LoggingTxn(long id, Txn txn) {
            this(id, txn, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        LoggingTxn(long id, Txn txn, List<Cmp> cmps, List<Op> thenOps, List<Op> elseOps) {
            this.id = id;
            this.delegateTxn = txn;
            this.allCmps = cmps;
            this.allThenOps = thenOps;
            this.allElseOps = elseOps;
        }

        @Override
        @SuppressFBWarnings("NM_METHOD_NAMING_CONVENTION")
        public Txn If(Cmp... cmps) {
            this.allCmps.addAll(Arrays.asList(cmps));
            return new LoggingTxn(id, delegateTxn.If(cmps), this.allCmps, this.allThenOps, this.allElseOps);
        }

        @Override
        @SuppressFBWarnings("NM_METHOD_NAMING_CONVENTION")
        public Txn Then(Op... thenOps) {
            this.allThenOps.addAll(Arrays.asList(thenOps));
            return new LoggingTxn(id, delegateTxn.Then(thenOps), this.allCmps, this.allThenOps, this.allElseOps);
        }

        @Override
        @SuppressFBWarnings("NM_METHOD_NAMING_CONVENTION")
        public Txn Else(Op... elseOps) {
            this.allElseOps.addAll(Arrays.asList(elseOps));
            return new LoggingTxn(id, delegateTxn.Else(elseOps), this.allCmps, this.allThenOps, this.allElseOps);
        }

        @Override
        public CompletableFuture<TxnResponse> commit() {
            // TODO add missing getters to Cmp & Op so that we can do this (and remove logging in EtcdKV EtcdTxn):
/*
            for (Cmp cmp : allCmps) {
                LOG.info("{}#{} txn IF {}", prefix, id, cmp...);
            }
*/
            return delegateTxn.commit().whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id));
        }
    }

    @SuppressFBWarnings({ "SLF4J_FORMAT_SHOULD_BE_CONST", "SLF4J_SIGN_ONLY_FORMAT" })
    private class LoggingCompletableFutureWhenCompleteConsumer<T> implements BiConsumer<T, Throwable> {

        private final long id;
        private final Function<T, String> messageFunction;

        LoggingCompletableFutureWhenCompleteConsumer(long id, Function<T, String> messageFunction) {
            this.id = id;
            this.messageFunction = messageFunction;
        }

        LoggingCompletableFutureWhenCompleteConsumer(long id) {
            this(id, aVoid -> MessageFormatter.format("#{} completed", id).getMessage());
        }

        @Override
        public void accept(T response, Throwable error) {
            if (error != null) {
                LOG.error("{}#{} failed", prefix, id, error);
            } else {
                if (LOG.isInfoEnabled()) {
                    LOG.info("{}{}", prefix, messageFunction.apply(response));
                }
            }
        }
    }

}
