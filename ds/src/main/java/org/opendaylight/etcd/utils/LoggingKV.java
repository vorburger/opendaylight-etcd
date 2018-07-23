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
import com.coreos.jetcd.options.CompactOption;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.common.base.MoreObjects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    private static final Logger LOG = LoggerFactory.getLogger(LoggingKV.class);

    private final AtomicLong counter = new AtomicLong();
    private final KV delegate;

    public LoggingKV(KV delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<CompactResponse> compact(long rev) {
        // TODO add logging, like for other methods
        return delegate.compact(rev);
    }

    @Override
    public CompletableFuture<CompactResponse> compact(long rev, CompactOption option) {
        // TODO add logging, like for other methods
        return delegate.compact(rev, option);
    }

    @Override
    public CompletableFuture<DeleteResponse> delete(ByteSequence key) {
        // TODO add logging, like for other methods
        return delegate.delete(key);
    }

    @Override
    public CompletableFuture<DeleteResponse> delete(ByteSequence key, DeleteOption option) {
        // TODO add logging, like for other methods
        return delegate.delete(key, option);
    }

    @Override
    public CompletableFuture<GetResponse> get(ByteSequence key) {
        long id = counter.incrementAndGet();
        return delegate.get(key).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id,
            getResponse -> MessageFormatter.arrayFormat("#{} get: {} ➞ {}",
                new Object[] { id, ByteSequences.asString(key), asString(getResponse) }).getMessage()));
    }

    @Override
    public CompletableFuture<GetResponse> get(ByteSequence key, GetOption option) {
        long id = counter.incrementAndGet();
        return delegate.get(key, option).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id,
            getResponse -> MessageFormatter.arrayFormat("#{} get: {} ({}) ➞ {}",
                new Object[] { id, ByteSequences.asString(key), asString(option), asString(getResponse) })
                .getMessage()));
    }

    // TODO jetcd GetOption & Co. really should have a working toString() - contribute a fix PR there upstream!
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

    private static String asString(GetResponse getResponse) {
        StringBuilder sb = new StringBuilder("count=" + getResponse.getCount() + ", KVs=[");
        getResponse.getKvs().forEach(kv -> {
            sb.append(ByteSequences.asString(kv.getKey()));
            sb.append(" ➙ ");
            sb.append(ByteSequences.asString(kv.getValue()));
            sb.append(", ");
        });
        sb.append(']');
        return sb.toString();
    }

    @Override
    public CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value) {
        long id = counter.incrementAndGet();
        LOG.info("#{} put: {} ➠ {}", id, toStringable(key), toStringable(value));
        return delegate.put(key, value).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id));
    }

    @Override
    public CompletableFuture<PutResponse> put(ByteSequence key, ByteSequence value, PutOption option) {
        long id = counter.incrementAndGet();
        LOG.info("#{} put: {} ➠ {} ({})", id, toStringable(key), toStringable(value), option);
        return delegate.put(key, value).whenComplete(new LoggingCompletableFutureWhenCompleteConsumer<>(id));
    }

    @Override
    public Txn txn() {
        // TODO add logging, like for other methods
        return delegate.txn();
    }

    @SuppressFBWarnings({ "SLF4J_FORMAT_SHOULD_BE_CONST", "SLF4J_SIGN_ONLY_FORMAT" })
    private static class LoggingCompletableFutureWhenCompleteConsumer<T> implements BiConsumer<T, Throwable> {

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
                LOG.error("#{} failed", id, error);
            } else {
                if (LOG.isInfoEnabled()) {
                    LOG.info("{}", messageFunction.apply(response));
                }
            }
        }
    }

}
