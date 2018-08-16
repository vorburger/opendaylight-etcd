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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concurrency utility to await availability of certain revisions.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
class RevAwaiter {

    private static final Logger LOG = LoggerFactory.getLogger(RevAwaiter.class);

    // TODO This is a first implementation.  It certainly can, and must, be (much) optimized.

    // TODO This must take possible long overflow of the long revision into account...

    private final AtomicLong currentRev = new AtomicLong();
    private final String nodeName;

    RevAwaiter(String nodeName) {
        this.nodeName = nodeName;
    }

    void update(long rev) {
        // Testing here is for debugging problems during development.
        // This IllegalStateException is not expected to ever happen in production,
        // if there are no logical design errors made in the code using this.
        currentRev.getAndUpdate(previous -> {
            if (rev <= previous) {
                throw new IllegalStateException(
                        nodeName + " update must be greater than current value: " + rev + " / " + previous);
            } else {
                return rev;
            }
        });
        LOG.info("{} update: {}", nodeName, rev);
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

//    private static class MyLong implements Comparable<MyLong> {
//    }

}
