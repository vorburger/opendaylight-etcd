/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import com.coreos.jetcd.watch.WatchEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.infrautils.utils.function.CheckedBiConsumer;

/**
 * Consumer suitable for EtcdWatcher which can drop watch events.
 * This is useful to simulate problems and is used in tests.
 *
 * @author Michael Vorburger.ch
 */
class EtcdWatcherBlockingConsumer implements TestTool, CheckedBiConsumer<Long, List<WatchEvent>, EtcdException> {

    private final AtomicBoolean isDropping = new AtomicBoolean(false);
    private final CheckedBiConsumer<Long, List<WatchEvent>, EtcdException> delegate;

    EtcdWatcherBlockingConsumer(CheckedBiConsumer<Long, List<WatchEvent>, EtcdException> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void dropWatchNotifications(boolean dropping) {
        this.isDropping.set(dropping);
    }

    @Override
    public void accept(Long revision, List<WatchEvent> allWatchEvents) throws EtcdException {
        if (!isDropping.get()) {
            delegate.accept(revision, allWatchEvents);
        }
    }
}
