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
import java.util.function.Consumer;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches Etcd and updates DataTree.
 *
 * @author Michael Vorburger.ch
 */
class EtcdWatcher implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdWatcher.class);

    private final Watch etcdWatch;
    private final ListeningExecutorService executor;
    private final Watcher theWatcher;

    EtcdWatcher(Client client, byte prefix, long revision, Consumer<WatchEvent> consumer) {
        this.etcdWatch = requireNonNull(client, "client").getWatchClient();
        executor = Executors.newListeningSingleThreadExecutor("EtcdWatcher", LOG);
        theWatcher = watch(prefix, revision, consumer);
    }

    @Override
    public void close() {
        // do not etcdWatch.close(); as that will happen when the Client gets closed
        executor.shutdownNow(); // intentionally NOT Executors.shutdownAndAwaitTermination(executor);
        theWatcher.close();
        LOG.info("{} close", this);
    }

    private Watcher watch(byte prefix, long revision, Consumer<WatchEvent> consumer) {
        byte[] prefixBytes = new byte[1];
        prefixBytes[0] = prefix;
        ByteSequence prefixByteSequence = ByteSequence.fromBytes(prefixBytes);

        Watcher watcher = etcdWatch.watch(prefixByteSequence, WatchOption.newBuilder().withRevision(revision).build());
        Futures.addCallback(executor.submit(() -> {
            while (true) {
                for (WatchEvent event : watcher.listen().getEvents()) {
                    consumer.accept(event);
                }
            }
        }), new FutureCallback<Void>() {

            @Override
            public void onFailure(Throwable throwable) {
                // InterruptedException is normal during close() above
                // ClosedClientException happens if we close abruptly due to an error (not normally)
                if (!(throwable instanceof InterruptedException) && !(throwable instanceof ClosedClientException)) {
                    LOG.error("watch: executor.submit() (eventually) failed: ", throwable);
                }
            }

            @Override
            public void onSuccess(Void nothing) {
                // ignore
            }
        }, MoreExecutors.directExecutor());
        return watcher;
    }
}
