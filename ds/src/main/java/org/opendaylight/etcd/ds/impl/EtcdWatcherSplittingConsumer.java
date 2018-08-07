/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static com.google.common.collect.ImmutableMap.builderWithExpectedSize;

import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.watch.WatchEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.opendaylight.etcd.utils.ByteSequences;
import org.opendaylight.infrautils.utils.function.CheckedBiConsumer;

/**
 * Consumer suitable for EtcdWatcher which "splits" watch events by prefix.
 * NB: Events not matching any prefixes are silently (!) dropped.
 *
 * @author Michael Vorburger.ch
 */
class EtcdWatcherSplittingConsumer implements CheckedBiConsumer<Long, List<WatchEvent>, EtcdException> {

    private final RevAwaiter revAwaiter;
    private final ImmutableMap<ByteSequence, Consumer<List<WatchEvent>>> splitConsumers;

    EtcdWatcherSplittingConsumer(RevAwaiter revAwaiter,
            Map<ByteSequence, Consumer<List<WatchEvent>>> splitConsumers) {
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
                if (ByteSequences.startsWith(watchEvent.getKeyValue().getKey(), keyPrefix)) {
                    lists.get(keyPrefix).add(watchEvent);
                }
            }
        }

        lists.forEach((prefix, list) -> splitConsumers.get(prefix).accept(list));

        revAwaiter.update(revision);
    }

}
