/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

/**
 * Internal etcd related exception.
 * This class is never directly exposed to, and cannot be caught by, any calling client code.
 * It may however be set as a cause in a client facing data store API.
 *
 * @author Michael Vorburger.ch
 */
// intentionally just .impl package-local, for now
final class EtcdException extends Exception {

    private static final long serialVersionUID = 1L;

    EtcdException(String message, Throwable cause) {
        super(message, cause);
    }

    EtcdException(String message) {
        super(message);
    }

}
