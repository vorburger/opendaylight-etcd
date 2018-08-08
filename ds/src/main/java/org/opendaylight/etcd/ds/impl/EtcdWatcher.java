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
import com.coreos.jetcd.watch.WatchResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import org.opendaylight.etcd.utils.KeyValues;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.infrautils.utils.function.CheckedBiConsumer;
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

    EtcdWatcher(String name, Client client, ByteSequence prefix, long revision,
            CheckedBiConsumer<Long, List<WatchEvent>, EtcdException> consumer) {
        this.name = name;
        this.etcdWatch = requireNonNull(client, "client").getWatchClient();

        // TODO better to do this in a @PostConstruct start() instead?
        this.executor = Executors.newListeningSingleThreadExecutor("EtcdWatcher-" + name, LOG);
        this.theWatcher = watch(prefix, revision, consumer);
    }

    @Override
    @PreDestroy
    public void close() {
        // do not etcdWatch.close(); as that will happen when the Client gets closed
        isOpen.set(false);
        executor.shutdownNow(); // intentionally NOT Executors.shutdownAndAwaitTermination(executor);
        theWatcher.close();
        LOG.info("{} closed.", name);
    }

    private Watcher watch(ByteSequence prefix, long revision,
            CheckedBiConsumer<Long, List<WatchEvent>, EtcdException> consumer) {
        Watcher watcher = etcdWatch.watch(prefix,
                WatchOption.newBuilder().withPrefix(prefix).withRevision(revision).build());
                // TODO is .withRange(prefix + 1) needed?!
        Futures.addCallback(executor.submit(() -> {
            while (isOpen.get()) {
                WatchResponse response = watcher.listen();
                List<WatchEvent> events = response.getEvents();
                for (WatchEvent event : events) {
                    LOG.info("{} watch: eventType={}, KV={}", name, event.getEventType(),
                            KeyValues.toStringable(event.getKeyValue()));
                }
                consumer.accept(response.getHeader().getRevision(), events);
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
