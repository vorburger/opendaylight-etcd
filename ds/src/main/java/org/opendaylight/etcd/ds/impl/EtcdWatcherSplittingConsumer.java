/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static com.google.common.collect.ImmutableMap.builderWithExpectedSize;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.watch.WatchEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.infrautils.utils.function.CheckedBiConsumer;
import org.opendaylight.infrautils.utils.function.CheckedConsumer;

/**
 * Consumer suitable for EtcdWatcher which "splits" watch events by prefix.
 * NB: Events not matching any prefixes are silently (!) dropped.
 *
 * @author Michael Vorburger.ch
 */
class EtcdWatcherSplittingConsumer implements CheckedBiConsumer<Long, List<WatchEvent>, EtcdException> {

    private final Optional<RevAwaiter> revAwaiter;
    private final ImmutableMap<ByteSequence, CheckedConsumer<List<WatchEvent>, EtcdException>> splitConsumers;

    EtcdWatcherSplittingConsumer(Optional<RevAwaiter> revAwaiter,
            Map<ByteSequence, CheckedConsumer<List<WatchEvent>, EtcdException>> splitConsumers) {
        this.revAwaiter = revAwaiter;
        this.splitConsumers = ImmutableMap.copyOf(splitConsumers);
    }

    @Override
    public void accept(Long revision, List<WatchEvent> allWatchEvents) throws EtcdException {
        Builder<ByteSequence, List<WatchEvent>> listsBuilder = builderWithExpectedSize(splitConsumers.size());
        ImmutableSet<ByteSequence> prefixes = splitConsumers.keySet();
        for (ByteSequence keyPrefix : prefixes) {
            listsBuilder.put(keyPrefix, new ArrayList<WatchEvent>());
        }
        ImmutableMap<ByteSequence, List<WatchEvent>> lists = listsBuilder.build();

        for (WatchEvent watchEvent : allWatchEvents) {
            for (ByteSequence keyPrefix : prefixes) {
                if (watchEvent.getKeyValue().getKey().startsWith(keyPrefix)) {
                    lists.get(keyPrefix).add(watchEvent);
                }
            }
        }

        for (Map.Entry<ByteSequence, List<WatchEvent>> list: lists.entrySet()) {
            List<WatchEvent> eventsList = list.getValue();
            // Important optimization, because with separate Oper/Config, one or the other is typically empty
            if (!eventsList.isEmpty()) {
                splitConsumers.get(list.getKey()).accept(eventsList);
            }
        }

        revAwaiter.ifPresent(revAwait -> revAwait.update(revision));
    }

}
