/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils.test;

import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;
import static org.opendaylight.infrautils.testutils.Asserts.assertThrows;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.etcd.ds.impl.EtcdDataStore;
import org.opendaylight.etcd.testutils.EtcdLauncherRule;
import org.opendaylight.etcd.testutils.TestEtcdDataBrokerProvider;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.DataValidationFailedException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.OptimisticLockFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.etcd.test.rev180628.HelloWorldContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.etcd.test.rev180628.HelloWorldContainer2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.etcd.test.rev180628.HelloWorldContainer2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.etcd.test.rev180628.HelloWorldContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the etcd-based DataBroker.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdDBTest {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdDBTest.class);

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);

    public static @ClassRule EtcdLauncherRule etcdLauncher = new EtcdLauncherRule();

    private Client client;
    private TestEtcdDataBrokerProvider dbProviderA;
    private DataBroker dataBrokerA;
    private TestEtcdDataBrokerProvider dbProviderB;
    private DataBroker dataBrokerB;

    // TODO figure out LoggingKV InterruptedException and uncomment LogCaptureRule
    // public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();
    public @Rule LogRule logRule = new LogRule();

    @Before
    public void before() throws Exception {
        client = Client.builder().endpoints(etcdLauncher.getClusterURIs()).build();

        // STOP any DB Watcher that is possibly still running from previous test
        closeProviders();

        deleteEtcd(EtcdDataStore.OPERATIONAL_PREFIX);
        deleteEtcd(EtcdDataStore.CONFIGURATION_PREFIX);

        recreateFreshDataBrokerClient();
    }

    private void recreateFreshDataBrokerClient() throws Exception {
        LOG.info("recreateFreshDataBrokerClient()");
        closeProviders();
        dbProviderA = new TestEtcdDataBrokerProvider(client, "a");
        dataBrokerA = dbProviderA.getDataBroker();
        dbProviderB = new TestEtcdDataBrokerProvider(client, "b");
        dataBrokerB = dbProviderB.getDataBroker();
    }

    private void closeProviders() throws Exception {
        if (dbProviderA != null) {
            dbProviderA.close();
        }
        if (dbProviderB != null) {
            dbProviderB.close();
        }
    }

    @After
    public void after() throws Exception {
        closeProviders();
        client.close();
    }

    @Test
    public void testDataBrokerIsNotNull() {
        assertThat(dataBrokerA).isNotNull();
        assertThat(dataBrokerB).isNotNull();
    }

    @Test
    public void testSimpleTestModelIntoDataStoreReadItBackAndDelete() throws Exception {
        InstanceIdentifier<HelloWorldContainer> iid = InstanceIdentifier.create(HelloWorldContainer.class);
        WriteTransaction initialTx = dataBrokerA.newWriteOnlyTransaction();
        initialTx.put(OPERATIONAL, iid, new HelloWorldContainerBuilder().setName("hello, world").build());
        initialTx.commit().get();

        recreateFreshDataBrokerClient();

        try (ReadTransaction readTx = dataBrokerA.newReadOnlyTransaction()) {
            assertThat(readTx.read(OPERATIONAL, iid).get().get().getName()).isEqualTo("hello, world");
        }
    }

    @Test
    public void testPutSomethingSlightlyMoreComplexIntoAReadItBackOnB() throws Exception {
        writeInitialState();
        assertThat(isTopInDataStore(dataBrokerB)).isTrue();
        deleteTop();
        assertThat(isTopInDataStore(dataBrokerB)).isFalse();
    }

    @Test
    public void testPutSomethingSlightlyMoreComplexIntoDataStoreReadItBackAndDelete() throws Exception {
        writeInitialState();
        recreateFreshDataBrokerClient();

        assertThat(isTopInDataStore()).isTrue();
        assertThat(isTopInDataStore(CONFIGURATION)).isFalse();
        recreateFreshDataBrokerClient();

        deleteTop();
        assertThat(isTopInDataStore()).isFalse();
        recreateFreshDataBrokerClient();
        assertThat(isTopInDataStore()).isFalse();

        // make sure etcd really is completely empty
        assertThatEtcdIsEmpty(EtcdDataStore.OPERATIONAL_PREFIX);
        assertThatEtcdIsEmpty(EtcdDataStore.CONFIGURATION_PREFIX);
    }

    private void assertThatEtcdIsEmpty(ByteSequence keyPrefix) throws InterruptedException, ExecutionException {
        try (KV kvClient = client.getKVClient()) {
            CompletableFuture<GetResponse> getFuture = kvClient.get(keyPrefix,
                    GetOption.newBuilder().withPrefix(keyPrefix).build());
            GetResponse response = getFuture.get();
            List<KeyValue> values = response.getKvs();
            assertThat(values).isEmpty();
        }
    }

    private void deleteEtcd(ByteSequence keyPrefix) throws InterruptedException, ExecutionException {
        try (KV kvClient = client.getKVClient()) {
            kvClient.delete(keyPrefix, DeleteOption.newBuilder().withPrefix(keyPrefix).build()).get();
        }
    }

    @Test
    public void testPutSomethingMoreComplexForSubTreeIntoDSReadItBackAndDelete() throws Exception {
        NestedList nl1 = new NestedListBuilder().withKey(new NestedListKey("nested1"))
                .setName("nested1").setType("type1").build();
        TopLevelList tl1 = new TopLevelListBuilder().withKey(new TopLevelListKey("top1"))
                .setName("top1").setNestedList(Arrays.asList(nl1)).build();
        WriteTransaction writeTx = dataBrokerA.newWriteOnlyTransaction();
        writeTx.put(OPERATIONAL, TOP_PATH, new TopBuilder().setTopLevelList(Arrays.asList(tl1)).build());
        writeTx.commit().get();

        recreateFreshDataBrokerClient();
        try (ReadTransaction readTx = dataBrokerA.newReadOnlyTransaction()) {
            assertThat(readTx.read(OPERATIONAL, path(new TopLevelListKey("top1"))).get().isPresent()).isTrue();
        }

        deleteTop();
        recreateFreshDataBrokerClient();
        try (ReadTransaction readTx = dataBrokerA.newReadOnlyTransaction()) {
            assertThat(readTx.read(OPERATIONAL, path(new TopLevelListKey("top1"))).get().isPresent()).isFalse();
        }
    }

    @Test
    public void testDataStoreIsEmptyInNewTest() throws Exception {
        assertThat(isTopInDataStore()).isFalse();
    }

    /**
     * Test that commit with missing mandatory fails.
     */
    @Test
    @Ignore // TODO Huh, this should fail, but it doesn't fail with the base test in-memory DB either?!
    public void testPutInvalidDueToMissingMandatory() throws Exception {
        InstanceIdentifier<HelloWorldContainer> iid = InstanceIdentifier.create(HelloWorldContainer.class);
        WriteTransaction tx = dataBrokerA.newWriteOnlyTransaction();
        tx.put(OPERATIONAL, iid, new HelloWorldContainerBuilder() /* .setName("hello, world") */.build());
        ExecutionException ex = assertThrows(ExecutionException.class, () -> tx.commit().get());
        assertThat(ex.getCause()).isInstanceOf(DataValidationFailedException.class);
    }

    /**
     * Test that a put which modifies what was concurrently modified on the same node in another TX fails.
     */
    @Test
    @Ignore // TODO must use an IF in TXN...
    public void testRealConflict() throws Exception {
        InstanceIdentifier<HelloWorldContainer> iid = InstanceIdentifier.create(HelloWorldContainer.class);
        HelloWorldContainer helloWorldContainer = new HelloWorldContainerBuilder().setName("hello, world").build();

        // Make sure that we get the OptimisticLockFailedException not because the Watcher
        // meanwhile updated our DataTree.. we need to detect this while writing out to etcd.
        dbProviderA.getTestTool().dropWatchNotifications(true);
        WriteTransaction txA = dataBrokerA.newWriteOnlyTransaction();
        WriteTransaction txB = dataBrokerA.newWriteOnlyTransaction();
        txA.put(OPERATIONAL, iid, helloWorldContainer);
        txB.put(OPERATIONAL, iid, helloWorldContainer);
        txA.commit().get();

        ExecutionException ex = assertThrows(ExecutionException.class, () -> txB.commit().get());
        assertThat(ex.getCause()).isInstanceOf(OptimisticLockFailedException.class);
    }

    /**
     * Test that a put which modifies what was concurrently modified on another cluster node fails.
     */
    @Test
    @Ignore // TODO as above
    public void testRealConflictInCluster() throws Exception {
        InstanceIdentifier<HelloWorldContainer> iid = InstanceIdentifier.create(HelloWorldContainer.class);
        HelloWorldContainer helloWorldContainer = new HelloWorldContainerBuilder().setName("hello, world").build();

        // NB:Contrary to above, we use dbProviderB for txB here (and therefore make it drop watch notifications)
        dbProviderB.getTestTool().dropWatchNotifications(true);
        WriteTransaction txA = dataBrokerA.newWriteOnlyTransaction();
        WriteTransaction txB = dataBrokerB.newWriteOnlyTransaction();
        txA.put(OPERATIONAL, iid, helloWorldContainer);
        txB.put(OPERATIONAL, iid, helloWorldContainer);
        txA.commit().get();

        ExecutionException ex = assertThrows(ExecutionException.class, () -> txB.commit().get());
        assertThat(ex.getCause()).isInstanceOf(OptimisticLockFailedException.class);
    }

    /**
     * Test that two concurrent puts on separate cluster nodes on separate non-overlapping non-conflicting paths work.
     */
    @Test
    public void testNoConflictInCluster() throws Exception {
        InstanceIdentifier<HelloWorldContainer> iidA = InstanceIdentifier.create(HelloWorldContainer.class);
        HelloWorldContainer helloWorldContainerA = new HelloWorldContainerBuilder().setName("hello, world").build();

        InstanceIdentifier<HelloWorldContainer2> iidB = InstanceIdentifier.create(HelloWorldContainer2.class);
        HelloWorldContainer2 helloWorldContainerB = new HelloWorldContainer2Builder().setName("hello, world").build();

        dbProviderB.getTestTool().dropWatchNotifications(true);
        WriteTransaction txA = dataBrokerA.newWriteOnlyTransaction();
        WriteTransaction txB = dataBrokerB.newWriteOnlyTransaction();
        txA.put(OPERATIONAL, iidA, helloWorldContainerA);
        txB.put(OPERATIONAL, iidB, helloWorldContainerB);
        txA.commit().get();
        txB.commit().get();
    }

    private void deleteTop() throws Exception {
        LOG.info("deleteTop()");
        WriteTransaction deleteTx = dataBrokerA.newWriteOnlyTransaction();
        deleteTx.delete(OPERATIONAL, TOP_PATH);
        deleteTx.commit().get();
    }

    // as in org.opendaylight.controller.md.sal.binding.test.tests.AbstractDataBrokerTestTest
    private void writeInitialState() throws Exception {
        LOG.info("writeInitialState: put Top & TopLevelList with augmentation");
        WriteTransaction initialTx = dataBrokerA.newWriteOnlyTransaction();
        initialTx.put(OPERATIONAL, TOP_PATH, new TopBuilder().build());

        TreeComplexUsesAugment fooAugment = new TreeComplexUsesAugmentBuilder()
                .setContainerWithUses(new ContainerWithUsesBuilder().setLeafFromGrouping("foo").build()).build();
        initialTx.put(OPERATIONAL, path(TOP_FOO_KEY), topLevelList(TOP_FOO_KEY, fooAugment));

        initialTx.commit().get();
    }

    private static boolean isTopInDataStore(LogicalDatastoreType type, DataBroker dataBroker) throws Exception {
        try (ReadTransaction readTx = dataBroker.newReadOnlyTransaction()) {
            Optional<Top> optTop = readTx.read(type, TOP_PATH).get();
            boolean present = optTop.isPresent();
            if (present) {
                // verify everything we wrote in the method above is really there...
                Top top = optTop.get();
                assertThat(top.getTopLevelList()).hasSize(1);
                TopLevelList topLevelList0 = top.getTopLevelList().get(0);
                assertThat(topLevelList0.key()).isEqualTo(TOP_FOO_KEY);
                assertThat(topLevelList0.getName()).isEqualTo("foo");
                assertThat(topLevelList0.getNestedList()).isNull();
                TreeComplexUsesAugment fooAugment = topLevelList0.augmentation(TreeComplexUsesAugment.class);
                assertThat(fooAugment).isNotNull();
                assertThat(fooAugment.getListViaUses()).isNull();
                assertThat(fooAugment.getContainerWithUses().getLeafFromGrouping()).isEqualTo("foo");
            }
            return present;
        }
    }

    private static boolean isTopInDataStore(DataBroker dataBroker) throws Exception {
        return isTopInDataStore(OPERATIONAL, dataBroker);
    }

    private boolean isTopInDataStore(LogicalDatastoreType type) throws Exception {
        return isTopInDataStore(type, dataBrokerA);
    }

    private boolean isTopInDataStore() throws Exception {
        return isTopInDataStore(OPERATIONAL);
    }

    // TODO add more as in org.opendaylight.controller.md.sal.dom.broker.impl.DOMBrokerTest & Co.

}
