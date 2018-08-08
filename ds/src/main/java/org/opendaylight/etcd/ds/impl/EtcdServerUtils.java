/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.coreos.jetcd.KV;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.Response.Header;
import com.coreos.jetcd.options.GetOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Small utility methods related to the etcd server.
 *
 * @author Michael Vorburger.ch
 */
final class EtcdServerUtils {

    // It does not matter what key we read; we just want to obtain the headers
    private static final ByteSequence ANY_KEY = EtcdDataStore.CONFIGURATION_PREFIX;
    private static final GetOption MINIMAL_GET_OPTION = GetOption.newBuilder().withKeysOnly(true).withLimit(0).build();

    private EtcdServerUtils() { }

    public static long getServerRevision(KV etcdKV) throws EtcdException {
        return getServerHeader(etcdKV).getRevision();
    }

    public static Header getServerHeader(KV etcdKV) throws EtcdException {
        try {
            return etcdKV.get(ANY_KEY, MINIMAL_GET_OPTION).get(EtcdYangKV.TIMEOUT_MS, MILLISECONDS).getHeader();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new EtcdException("failed to connect (in time) to etcd server", e);
        }
    }
}
