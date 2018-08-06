/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static java.util.Objects.requireNonNull;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.Watch.Watcher;
import com.coreos.jetcd.common.exception.ClosedClientException;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.etcd.utils.KeyValues;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.infrautils.utils.function.CheckedConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility with background thread to continuously watch for changes from etcd.
 *
 * @author Michael Vorburger.ch
 */
class EtcdWatcher implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdWatcher.class);

    private final Watch etcdWatch;
    private final ListeningExecutorService executor;
    private final Watcher theWatcher;
    private final String name;
    private final AtomicBoolean isOpen = new AtomicBoolean(true);

    EtcdWatcher(String name, Client client, byte prefix, long revision,
            CheckedConsumer<WatchEvent, EtcdException> consumer) {
        this.name = name;
        this.etcdWatch = requireNonNull(client, "client").getWatchClient();
        this.executor = Executors.newListeningSingleThreadExecutor("EtcdWatcher-" + name, LOG);
        this.theWatcher = watch(prefix, revision, consumer);
    }

    @Override
    public void close() {
        // do not etcdWatch.close(); as that will happen when the Client gets closed
        isOpen.set(false);
        executor.shutdownNow(); // intentionally NOT Executors.shutdownAndAwaitTermination(executor);
        theWatcher.close();
        LOG.info("{} closed.", name);
    }

    private Watcher watch(byte prefix, long revision, CheckedConsumer<WatchEvent, EtcdException> consumer) {
        byte[] prefixBytes = new byte[1];
        prefixBytes[0] = prefix;
        ByteSequence prefixByteSequence = ByteSequence.fromBytes(prefixBytes);

        Watcher watcher = etcdWatch.watch(prefixByteSequence,
                WatchOption.newBuilder().withPrefix(prefixByteSequence).withRevision(revision).build());
                // TODO is .withRange(prefix + 1) needed?!
        Futures.addCallback(executor.submit(() -> {
            while (isOpen.get()) {
                LOG.trace("{} watch: Now (again) listening...", name);
                for (WatchEvent event : watcher.listen().getEvents()) {
                    LOG.info("{} watch: eventType={}, KV={}", name, event.getEventType(),
                            KeyValues.toStringable(event.getKeyValue()));
                    consumer.accept(event);
                }
            }
            return null;
        }), new FutureCallback<Void>() {

            @Override
            public void onFailure(Throwable throwable) {
                // InterruptedException is normal during close() above
                // ClosedClientException happens if we close abruptly due to an error (not normally)
                if (!(throwable instanceof InterruptedException) && !(throwable instanceof ClosedClientException)) {
                    LOG.error("{} watch: executor.submit() (eventually) failed: ", name, throwable);
                }
            }

            @Override
            public void onSuccess(Void nothing) {
                // This will happen when isOpen becomes false on close()
            }
        }, MoreExecutors.directExecutor());
        return watcher;
    }
}
