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
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import ch.vorburger.exec.ManagedProcessException;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.options.GetOption;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.etcd.ds.impl.EtcdDataStore;
import org.opendaylight.etcd.launcher.EtcdLauncher;
import org.opendaylight.etcd.testutils.TestEtcdDataBrokersProvider;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.etcd.test.rev180628.HelloWorldContainer;
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

    private static EtcdLauncher etcdServer;
    private static Client client;

    private TestEtcdDataBrokersProvider testEtcdDataBrokersProvider;
    private DataBroker dataBroker;

    public @Rule LogRule logRule = new LogRule();

    @BeforeClass
    public static void beforeClass() throws ManagedProcessException, IOException {
        etcdServer = new EtcdLauncher(Paths.get("target/etcd"), true);
        etcdServer.start();
    }

    @Before
    public void before() throws Exception {
        client = Client.builder().endpoints(etcdServer.getEndpointURL()).build();
        recreateFreshDataBrokerClient();
    }

    private void recreateFreshDataBrokerClient() throws Exception {
        LOG.info("recreateFreshDataBrokerClient()");
        if (testEtcdDataBrokersProvider != null) {
            testEtcdDataBrokersProvider.close();
        }
        testEtcdDataBrokersProvider = new TestEtcdDataBrokersProvider(client);
        dataBroker = testEtcdDataBrokersProvider.getDataBroker();
    }

    @After
    public void after() throws Exception {
        testEtcdDataBrokersProvider.close();
        testEtcdDataBrokersProvider = null;
        dataBroker = null;
        client.close();
        client = null;
    }

    @AfterClass
    public static void afterClass() throws ManagedProcessException {
        etcdServer.close();
        etcdServer = null;
    }

    @Test
    public void testDataBrokerIsNotNull() {
        assertThat(dataBroker).isNotNull();
    }

    @Test
    public void testSimpleTestModelIntoDataStoreReadItBackAndDelete() throws Exception {
        InstanceIdentifier<HelloWorldContainer> iid = InstanceIdentifier.create(HelloWorldContainer.class);
        WriteTransaction initialTx = dataBroker.newWriteOnlyTransaction();
        initialTx.put(OPERATIONAL, iid, new HelloWorldContainerBuilder().setName("hello, world").build());
        initialTx.commit().get();

        recreateFreshDataBrokerClient();

        try (ReadTransaction readTx = dataBroker.newReadOnlyTransaction()) {
            assertThat(readTx.read(OPERATIONAL, iid).get().get().getName()).isEqualTo("hello, world");
        }
    }

    // as in org.opendaylight.controller.md.sal.binding.test.tests.AbstractDataBrokerTestTest

    @Test
    public void testPutSomethingSlightlyMoreComplexIntoDataStoreReadItBackAndDelete() throws Exception {
        writeInitialState();
        recreateFreshDataBrokerClient();

        assertThat(isTopInDataStore()).isTrue();
        assertThat(isTopInDataStore(CONFIGURATION)).isFalse();
        recreateFreshDataBrokerClient();

        // TODO modify what we just wrote and read back to make sure value changed in etcd

        // TODO write Top to Oper instead Config, delete Config's, ensure it's gone but Oper's still there

        deleteTop();
        assertThat(isTopInDataStore()).isFalse();
        recreateFreshDataBrokerClient();
        assertThat(isTopInDataStore()).isFalse();

        // make sure etcd really is completely empty
        assertThatEtcdSubTreeIsEmpty(ByteSequence.fromBytes(bytes(EtcdDataStore.OPERATIONAL_PREFIX)));
        assertThatEtcdSubTreeIsEmpty(ByteSequence.fromBytes(bytes(EtcdDataStore.CONFIGURATION_PREFIX)));
    }

    private void assertThatEtcdSubTreeIsEmpty(ByteSequence keyPrefix) throws InterruptedException, ExecutionException {
        try (KV kvClient = client.getKVClient()) {
            CompletableFuture<GetResponse> getFuture = kvClient.get(keyPrefix,
                    GetOption.newBuilder().withPrefix(keyPrefix).build());
            GetResponse response = getFuture.get();
            List<KeyValue> values = response.getKvs();
            assertThat(values).isEmpty();
        }
    }

    @Test
    @Ignore // TODO re-activate this later.. with recreateFreshDataBrokerClient(); - without it it's (now) pointless
    public void testPutSomethingMoreComplexForSubTreeIntoDSReadItBackAndDelete() throws Exception {
        NestedList nl1 = new NestedListBuilder().withKey(new NestedListKey("nested1"))
                .setName("nested1").setType("type1").build();
        TopLevelList tl1 = new TopLevelListBuilder().withKey(new TopLevelListKey("top1"))
                .setName("top1").setNestedList(Arrays.asList(nl1)).build();
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.put(OPERATIONAL, TOP_PATH, new TopBuilder().setTopLevelList(Arrays.asList(tl1)).build());
        writeTx.commit().get();

        try (ReadTransaction readTx = dataBroker.newReadOnlyTransaction()) {
            assertThat(readTx.read(OPERATIONAL, path(new TopLevelListKey("top1"))).get().isPresent()).isTrue();
        }

        deleteTop();
        try (ReadTransaction readTx = dataBroker.newReadOnlyTransaction()) {
            assertThat(readTx.read(OPERATIONAL, path(new TopLevelListKey("top1"))).get().isPresent()).isFalse();
        }
    }

    @Test
    public void testDataStoreIsEmptyInNewTest() throws Exception {
        assertThat(isTopInDataStore()).isFalse();
    }

    private void deleteTop() throws Exception {
        LOG.info("deleteTop()");
        WriteTransaction deleteTx = dataBroker.newWriteOnlyTransaction();
        deleteTx.delete(OPERATIONAL, TOP_PATH);
        deleteTx.commit().get();
    }

    private void writeInitialState() throws Exception {
        LOG.info("writeInitialState: put Top");
        WriteTransaction initialTx = dataBroker.newWriteOnlyTransaction();
        initialTx.put(OPERATIONAL, TOP_PATH, new TopBuilder().build());

        TreeComplexUsesAugment fooAugment = new TreeComplexUsesAugmentBuilder()
                .setContainerWithUses(new ContainerWithUsesBuilder().setLeafFromGrouping("foo").build()).build();
        LOG.info("writeInitialState: put TopLevelList with augmentation");
        initialTx.put(OPERATIONAL, path(TOP_FOO_KEY), topLevelList(TOP_FOO_KEY, fooAugment));

        initialTx.commit().get();
    }

    private boolean isTopInDataStore(LogicalDatastoreType type) throws Exception {
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

    private boolean isTopInDataStore() throws Exception {
        return isTopInDataStore(OPERATIONAL);
    }

    private byte[] bytes(byte... bytes) {
        return bytes;
    }

    // TODO add more as in org.opendaylight.controller.md.sal.dom.broker.impl.DOMBrokerTest & Co.

}
