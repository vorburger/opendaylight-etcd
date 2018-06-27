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
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thingie that watches Etcd and updates DataTree.
 *
 * @author Michael Vorburger.ch
 */
class EtcdWatcher implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdWatcher.class);

    private final Watch etcdWatch;
    private final ExecutorService executor;

    EtcdWatcher(Client client) {
        this.etcdWatch = requireNonNull(client, "client").getWatchClient();
        executor = Executors.newSingleThreadExecutor("EtcdWatcher", LOG);
    }

    @Override
    public void close() {
        // TODO stop background thread...
        etcdWatch.close();
        executor.shutdownNow();
    }

    void watch(byte prefix, long revision, Consumer<WatchEvent> consumer) {
        byte[] prefixAsBytes = new byte[1];
        prefixAsBytes[0] = prefix;
        watch(ByteSequence.fromBytes(prefixAsBytes), revision, consumer);
    }

    private void watch(ByteSequence prefix, long revision, Consumer<WatchEvent> consumer) {
        executor.submit(() -> {
            while (true) {
                try (Watcher initialWatcher = etcdWatch.watch(prefix,
                        WatchOption.newBuilder().withRevision(revision).build())) {
                    for (WatchEvent event : initialWatcher.listen().getEvents()) {
                        consumer.accept(event);
                    }
                }
            }
        });
    }
}
