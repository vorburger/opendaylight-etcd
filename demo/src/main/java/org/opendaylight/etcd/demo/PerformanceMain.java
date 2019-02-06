/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.demo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;

import io.etcd.jetcd.Client;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.etcd.testutils.TestEtcdDataBrokerProvider;
import org.opendaylight.infrautils.metrics.MetricDescriptor;
import org.opendaylight.infrautils.metrics.Timer;
import org.opendaylight.infrautils.metrics.internal.MetricProviderImpl;
import org.opendaylight.infrautils.metrics.internal.MetricsFileReporter;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.etcd.test.rev180628.HelloWorldContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.etcd.test.rev180628.HelloWorldContainerBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Performance Tester.
 *
 * @author Michael Vorburger.ch
 */
@SuppressWarnings("checkstyle:RegexpSingleLineJava")
public final class PerformanceMain {

    private static void onePutAndCommit(DataBroker dataBroker) throws InterruptedException, ExecutionException {
        InstanceIdentifier<HelloWorldContainer> iid = InstanceIdentifier.create(HelloWorldContainer.class);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(CONFIGURATION, iid, new HelloWorldContainerBuilder().setName("hello, world").build());
        tx.commit().get();
    }

    public static void main(String[] args) throws Exception {
        MetricProviderImpl metricProvider = new MetricProviderImpl();
        MetricDescriptor descriptor = MetricDescriptor.builder()
                .anchor(new PerformanceMain()).project("etcd").module("demo").id("performance").build();
        Timer timer = metricProvider.newTimer(descriptor);

        List<String> endpoints = Collections.singletonList("http://localhost:2379");

        try (Client client = Client.builder().endpoints(endpoints.toArray(new String[0])).build()) {
            try (TestEtcdDataBrokerProvider dbProvider = new TestEtcdDataBrokerProvider(client, "demo")) {
                DataBroker dataBroker = dbProvider.getDataBroker();

                int numberOfWrites = 10000;
                long startedAtMS = System.currentTimeMillis();
                for (int i = 0; i < numberOfWrites; i++) {
                    timer.time(() -> onePutAndCommit(dataBroker));
                }
                long duration = System.currentTimeMillis() - startedAtMS;
                long onePutAndCommitDuration = duration / numberOfWrites;
                System.out.println("onePutAndCommitDurationInMS = " + onePutAndCommitDuration);
            } finally {
                client.close();
            }
        }

        dumpMetricsToStdOut(metricProvider);
        metricProvider.close();
    }

    private static void dumpMetricsToStdOut(MetricProviderImpl metricProvider) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8)))) {
            try (MetricsFileReporter reporter = new MetricsFileReporter(metricProvider.getRegistry(), Duration.ZERO)) {
                reporter.report(pw);
            }
        }
    }

    private PerformanceMain() { }
}
