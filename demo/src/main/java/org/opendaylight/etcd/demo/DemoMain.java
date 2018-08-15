/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.demo;

import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import java.util.concurrent.ExecutionException;
import org.opendaylight.etcd.testutils.TestEtcdDataBrokerProvider;
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
        tx.put(OPERATIONAL, iid, new HelloWorldContainerBuilder().setName("hello, world").build());
        tx.commit().get();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("USAGE: etcd server host:port (list of)\nEXAMPLE: localhost:2379");
            return;
        }
        ClientBuilder client = Client.builder().endpoints(args);
        try (TestEtcdDataBrokerProvider dbProvider = new TestEtcdDataBrokerProvider(client, "demo")) {
            DataBroker dataBroker = dbProvider.getDataBroker();

            write(dataBroker);

        } catch (Exception e) {
            LOG.error("Demo failed", e);
        } finally {
            // TODO find out why non-daemon thread "pool-2-thread-1" causes hung exit without this hack..
            System.exit(0);
        }
    }

    private DemoMain() { }
}
