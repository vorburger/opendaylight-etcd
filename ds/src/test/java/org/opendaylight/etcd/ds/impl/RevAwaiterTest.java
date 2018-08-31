/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.opendaylight.infrautils.testutils.Asserts.assertThrows;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for RevAwaiter.
 *
 * @author Michael Vorburger.ch
 */
public class RevAwaiterTest {

    private static final Logger LOG = LoggerFactory.getLogger(RevAwaiterTest.class);

    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();
    // public @Rule LogRule logRule = new LogRule();

    private static final Duration MS_100 = Duration.ofMillis(100);

    RevAwaiter awaiter = new RevAwaiter("TEST");

    @Test public void testAwaitFail() {
        assertThrows(TimeoutException.class, () -> awaiter.await(1, MS_100));
    }

    @Test public void testNotify() throws TimeoutException, InterruptedException {
        awaiter.update(1);
        awaiter.await(1, MS_100);
    }

    @Test public void testNotifyHigher() throws TimeoutException, InterruptedException {
        awaiter.update(2);
        awaiter.await(1, MS_100);
    }

    @Test public void testAwaitThenNotify() throws TimeoutException, InterruptedException, ExecutionException {
        ListeningExecutorService executor = Executors.newListeningSingleThreadExecutor("await", LOG);
        ListenableFuture<Void> future = executor.submit(() -> {
            awaiter.await(1, MS_100);
            return null;
        });
        awaiter.update(1);
        future.get(200, MILLISECONDS);
        executor.shutdown();
        executor.awaitTermination(5, MILLISECONDS);
    }

}
