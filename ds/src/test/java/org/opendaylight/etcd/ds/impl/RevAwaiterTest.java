/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static org.opendaylight.infrautils.testutils.Asserts.assertThrows;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.infrautils.testutils.LogCaptureRule;

/**
 * Unit test for RevAwaiter.
 *
 * @author Michael Vorburger.ch
 */
public class RevAwaiterTest {

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

    // TODO multi-threaded tests

}
