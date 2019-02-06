/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListeningExecutorService;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.Watch.Watcher;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
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
    private final String name;
    private final AtomicBoolean isOpen = new AtomicBoolean(true);

    private final ByteSequence prefix;
    private final CheckedBiConsumer<Long, List<WatchEvent>, EtcdException> consumer;

    private Watcher theWatcher;

    EtcdWatcher(String name, Client client, ByteSequence prefix,
            CheckedBiConsumer<Long, List<WatchEvent>, EtcdException> consumer) {
        this.name = name;
        this.prefix = prefix;
        this.consumer = consumer;
        this.etcdWatch = requireNonNull(client, "client").getWatchClient();

        this.executor = Executors.newListeningSingleThreadExecutor("EtcdWatcher-" + name, LOG);
    }

    public void start(long revision) {
        this.theWatcher = watch(revision);
    }

    @Override
    @PreDestroy
    public void close() {
        // do not etcdWatch.close(); as that will happen when the Client gets closed
        isOpen.set(false);
        executor.shutdownNow(); // intentionally NOT Executors.shutdownAndAwaitTermination(executor);
        if (theWatcher != null) {
            theWatcher.close();
        }
        LOG.info("{} closed.", name);
    }

    private Watcher watch(long revision) {
        Watch.Listener listener = Watch.listener(response -> {
            List<WatchEvent> events = response.getEvents();
            for (WatchEvent event : events) {
                LOG.info("{} watch: eventType={}, KV={}", name, event.getEventType(),
                        KeyValues.toStringable(event.getKeyValue()));
            }
            try {
                consumer.accept(response.getHeader().getRevision(), events);
            } catch (EtcdException e) {
                LOG.error("watch consumer accept failed", e);
            }
        });
        Watcher watcher = etcdWatch.watch(prefix,
                WatchOption.newBuilder().withPrefix(prefix).withRevision(revision).build(), listener);
        // TODO is .withRange(prefix + 1) needed?!
        return watcher;
    }
}
