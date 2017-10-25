/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.jetcd.test;

import static com.google.common.truth.Truth.assertThat;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.opendaylight.etcd.jetcd.EtcdLauncher;

/**
 * Simple test to learn using the jetcd client.
 *
 * @author Michael Vorburger.ch
 */
public class LearnEtcdTest {

    @Test
    public void testEtcd() throws Exception {
        try (EtcdLauncher etcdServer = new EtcdLauncher().start()) {
            Client client = Client.builder().endpoints("http://localhost:2379").build();
            KV kvClient = client.getKVClient();

            ByteSequence key = ByteSequence.fromString("test_key");
            ByteSequence value = ByteSequence.fromString("test_value");
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
