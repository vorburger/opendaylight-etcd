/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import com.google.errorprone.annotations.Var;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concurrency utility to await availability of certain revisions.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
class RevAwaiter {

    // TODO This must take possible long overflow of the long revision into account...

    // TODO is it better to use java.util.concurrent.locks.Condition instead of Object.wait() - why?

    private static final Logger LOG = LoggerFactory.getLogger(RevAwaiter.class);

    private static final AwaitableRev[] EMPTY_ARRAY = new AwaitableRev[0];

    private static class AwaitableRev {
        final long rev;

        AwaitableRev(long rev) {
            this.rev = rev;
        }

        @Override
        public String toString() {
            return "AwaitableRev-" + rev;
        }
    }

    private final AtomicLong currentRev = new AtomicLong();
    private final Queue<AwaitableRev> pq = new PriorityQueue<>((o1, o2) -> Long.compare(o1.rev, o2.rev));
    private final String nodeName;

    RevAwaiter(String nodeName) {
        this.nodeName = nodeName;
    }

    // shut up FindBugs, I'm a grown up knowing what I'm doing here - I hope! ;-)
    @SuppressFBWarnings({ "NO_NOTIFY_NOT_NOTIFYALL", "NN_NAKED_NOTIFY" })
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

        AwaitableRev[] pqCopy;
        synchronized (pq) {
            pqCopy = pq.toArray(EMPTY_ARRAY);
        }
        for (AwaitableRev awaitable : pqCopy) {
            if (rev >= awaitable.rev) {
                synchronized (awaitable) {
                    awaitable.notify();
                }
            } else {
                break;
            }
        }

        LOG.info("{} update: {}", nodeName, rev);
    }

    void await(long rev, Duration maxWaitTime) throws TimeoutException, InterruptedException {
        if (currentRev.get() >= rev) {
            return;
        }
        AwaitableRev awaitable = new AwaitableRev(rev);
        synchronized (pq) {
            pq.add(awaitable);
        }
        synchronized (awaitable) {
            // account for possible spurious wake up
            @Var long now = System.nanoTime();
            long deadline = now + maxWaitTime.toNanos();
            while (currentRev.get() < rev && now < deadline) {
                // http://errorprone.info/bugpattern/WaitNotInLoop
                awaitable.wait((deadline - now) / 1000000);
                now = System.nanoTime();
            }
            if (now >= deadline) {
                throw new TimeoutException();
            }
            // else it's a real awaitable.notify(), not spurious nor timeout, and we return to caller.
        }
    }

    @Override
    public String toString() {
        return "RevAwaiter: currentRev=" + currentRev;
    }
}
