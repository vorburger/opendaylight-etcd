/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Transaction ID.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
// intentionally just .impl package-local, for now
final class TransactionIdentifier {

    // TODO static factory methods

    private static final AtomicLong COUNTER = new AtomicLong(0);

    static TransactionIdentifier next() {
        return new TransactionIdentifier(COUNTER.getAndIncrement());
    }

    private final long id;

    private TransactionIdentifier(long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        return prime + (int) (id ^ id >>> 32);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TransactionIdentifier)) {
            return false;
        }
        TransactionIdentifier other = (TransactionIdentifier) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return Long.toString(id);
    }

}
