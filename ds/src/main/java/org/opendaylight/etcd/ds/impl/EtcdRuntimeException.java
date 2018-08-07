/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

/**
 * Internal etcd related runtime exception.
 * This is thrown from data store APIs which do not declare other checked exceptions we could wrap a EtcdException into.
 *
 * @author Michael Vorburger.ch
 */
// intentionally just .impl package-local, for now
final class EtcdRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    EtcdRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    EtcdRuntimeException(String message) {
        super(message);
    }

}
