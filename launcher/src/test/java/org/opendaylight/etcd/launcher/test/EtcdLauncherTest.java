/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.launcher.test;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.truth.Truth.assertThat;

import ch.vorburger.exec.ManagedProcessException;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.data.ByteSequence;
import io.etcd.jetcd.data.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.opendaylight.etcd.launcher.EtcdLauncher;

/**
 * Tests for {@link EtcdLauncher}.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdLauncherTest {

    /**
     * Trivial test just to make sure it starts, without exception.
     */
    @Test
    public void testLaunchEtcd() throws ManagedProcessException, IOException {
        try (EtcdLauncher etcd = new EtcdLauncher(Paths.get("target/etcd"), true)) {
            etcd.start();
        }
    }

    /**
     * Simple test illustrating usage of the jetcd client.
     */
    @Test
    public void testEtcd() throws Exception {
        try (EtcdLauncher etcd = new EtcdLauncher(Paths.get("target/etcd"), true)) {
            etcd.start();
            try (Client client = Client.builder().endpoints(etcd.getEndpointURL()).build()) {
                try (KV kvClient = client.getKVClient()) {

                    ByteSequence key = ByteSequence.from("test_key", UTF_8);
                    ByteSequence value = ByteSequence.from("test_value", UTF_8);
                    kvClient.put(key, value).get();

                    CompletableFuture<GetResponse> getFuture = kvClient.get(key);
                    GetResponse response = getFuture.get();
                    List<KeyValue> values = response.getKvs();
                    assertThat(values).hasSize(1);
                    KeyValue value1 = values.get(0);
                    assertThat(value1.getValue()).isEqualTo(value);
                    assertThat(value1.getKey()).isEqualTo(key);
                }
            }
        }
    }

}
