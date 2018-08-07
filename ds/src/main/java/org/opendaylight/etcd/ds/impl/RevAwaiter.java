/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

/**
 * Concurrency utility to await availability of certain revisions.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
class RevAwaiter {

    // TODO This is a first implementation.  It certainly can, and must, be (much) optimized.

    private final AtomicLong currentRev = new AtomicLong();

    void update(long rev) {
        currentRev.set(rev);
    }

    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    void await(long rev, Duration maxWaitTime) throws TimeoutException {
        try {
            Awaitility.await("RevAwaiter")
                .atMost(maxWaitTime.toMillis(), MILLISECONDS)
                .pollInterval(100, MILLISECONDS) // ?
                .pollDelay(org.awaitility.Duration.ZERO)
                // .until(() -> currentRev.get() >= rev);
                // .until(() -> currentRev, org.hamcrest.Matchers.equalTo(rev));
                .until(currentRev::get, org.hamcrest.Matchers.greaterThanOrEqualTo(rev));
        } catch (ConditionTimeoutException cte) {
            throw new TimeoutException(currentRev.get() + "__" + cte.getMessage());
        }
    }



}
