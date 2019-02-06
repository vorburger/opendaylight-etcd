/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.demo;

import static com.google.common.base.Charsets.US_ASCII;
import static io.etcd.jetcd.options.GetOption.SortOrder.ASCEND;
import static io.etcd.jetcd.options.GetOption.SortTarget.KEY;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;

import com.google.common.collect.Lists;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.options.GetOption;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.etcd.ds.impl.EtcdDataStore;
import org.opendaylight.etcd.testutils.TestEtcdDataBrokerProvider;
import org.opendaylight.etcd.utils.KeyValues;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.etcd.test.rev180628.HelloWorldContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.etcd.test.rev180628.HelloWorldContainerBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ODL etcd YANG Demo.
 *
 * @author Michael Vorburger.ch
 */
@SuppressWarnings({ "checkstyle:RegexpSingleLineJava", "checkstyle:IllegalCatch"})
public final class DemoMain {

    private static final Logger LOG = LoggerFactory.getLogger(DemoMain.class);

    private static void write(DataBroker dataBroker) throws InterruptedException, ExecutionException {
        InstanceIdentifier<HelloWorldContainer> iid = InstanceIdentifier.create(HelloWorldContainer.class);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(CONFIGURATION, iid, new HelloWorldContainerBuilder().setName("hello, world").build());
        tx.commit().get();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("USAGE: write|read etcd-server-host:port (list of)\nEXAMPLE: write http://localhost:2379");
            return;
        }
        String operation = args[0];
        List<String> endpoints = Lists.newArrayList(args).subList(1, args.length);

        System.out.println("Operation: " + operation + "; connecting to etcd server/s on: " + endpoints);
        try (Client client = Client.builder().endpoints(endpoints.toArray(new String[0])).build()) {
            if ("write".equalsIgnoreCase(operation)) {
                try (TestEtcdDataBrokerProvider dbProvider = new TestEtcdDataBrokerProvider(client, "demo")) {
                    DataBroker dataBroker = dbProvider.getDataBroker();
                    write(dataBroker);
                } finally {
                    client.close();
                }
            } else {
                read(client);
            }
        } catch (Exception e) {
            LOG.error("Demo failed", e);
        }
        // TODO find out why non-daemon thread "pool-2-thread-1" causes hung exit without this hack..
        System.exit(0);
    }

    private static void read(Client client) throws InterruptedException, ExecutionException, TimeoutException {
        ByteSequence prefix = EtcdDataStore.CONFIGURATION_PREFIX;
        // TODO huh, the sorting by key doesn't seem to work?  Running this after restconf demo has aaa interspersed
        GetOption getOpt = GetOption.newBuilder().withPrefix(prefix).withSortField(KEY).withSortOrder(ASCEND).build();
        List<KeyValue> kvs = client.getKVClient().get(prefix, getOpt).get(3000, MILLISECONDS).getKvs();
        System.out.println(kvs.size() + " key/values in etcd under prefix " + prefix.toString(US_ASCII) + "/ :");
        for (KeyValue kv : kvs) {
            System.out.println(KeyValues.asString(kv));
        }
    }

    private DemoMain() { }
}
