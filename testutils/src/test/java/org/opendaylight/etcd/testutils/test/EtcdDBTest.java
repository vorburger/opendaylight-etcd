/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils.test;

import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;

import ch.vorburger.exec.ManagedProcessException;
import com.coreos.jetcd.Client;

import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.etcd.launcher.EtcdLauncher;
import org.opendaylight.etcd.testutils.TestEtcdDataBrokersProvider;
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

/**
 * Tests the etcd-based DataBroker.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdDBTest {

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);

    private static EtcdLauncher etcdServer;
    private static Client client;
    private DataBroker dataBroker;

    @BeforeClass
    public static void beforeClass() throws ManagedProcessException {
        etcdServer = new EtcdLauncher();
        etcdServer.start();
    }

    @Before
    public void before() {
        client = Client.builder().endpoints(etcdServer.getEndpointURL()).build();
        dataBroker = new TestEtcdDataBrokersProvider(client).getDataBroker();
    }

    @After
    public void after() throws ManagedProcessException {
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

    // as in org.opendaylight.controller.md.sal.binding.test.tests.AbstractDataBrokerTestTest

    @Test
    public void testPutSomethingIntoDataStoreReadItBackAndDelete() throws Exception {
        writeInitialState();
        assertThat(isTopInDataStore()).isTrue();
        assertThat(isTopInDataStore(CONFIGURATION)).isFalse();

        deleteTop();
        assertThat(isTopInDataStore()).isFalse();
    }

    @Test
    @Ignore // TODO make this pass!!! ;-) It's, probably, a bit of work...
    public void putSomethingForSubTreeIntoDSReadItBackAndDelete() throws Exception {
        NestedList nl1 = new NestedListBuilder().setKey(new NestedListKey("nested1"))
                .setName("nested1").setType("type1").build();
        TopLevelList tl1 = new TopLevelListBuilder().setKey(new TopLevelListKey("top1"))
                .setName("top1").setNestedList(Arrays.asList(nl1)).build();
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.put(OPERATIONAL, TOP_PATH, new TopBuilder().setTopLevelList(Arrays.asList(tl1)).build());
        writeTx.submit().get();

        try (ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction()) {
            assertThat(readTx.read(OPERATIONAL, path(new TopLevelListKey("top1"))).get().isPresent()).isTrue();
        }

        deleteTop();
        try (ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction()) {
            assertThat(readTx.read(OPERATIONAL, path(new TopLevelListKey("top1"))).get().isPresent()).isFalse();
        }
    }

    @Test
    @Ignore // TODO think about how to best completely clear out external etcd between tests..
    public void testDataStoreIsEmptyInNewTest() throws ReadFailedException {
        assertThat(isTopInDataStore()).isFalse();
    }

    private void deleteTop() throws Exception {
        WriteTransaction deleteTx = dataBroker.newWriteOnlyTransaction();
        deleteTx.delete(OPERATIONAL, TOP_PATH);
        deleteTx.commit().get();
    }

    private void writeInitialState() throws Exception {
        WriteTransaction initialTx = dataBroker.newWriteOnlyTransaction();
        initialTx.put(OPERATIONAL, TOP_PATH, new TopBuilder().build());
        TreeComplexUsesAugment fooAugment = new TreeComplexUsesAugmentBuilder()
                .setContainerWithUses(new ContainerWithUsesBuilder().setLeafFromGrouping("foo").build()).build();
        initialTx.put(OPERATIONAL, path(TOP_FOO_KEY), topLevelList(TOP_FOO_KEY, fooAugment));
        initialTx.submit().get();
    }

    private boolean isTopInDataStore() throws ReadFailedException {
        return isTopInDataStore(OPERATIONAL);
    }

    private boolean isTopInDataStore(LogicalDatastoreType type) throws ReadFailedException {
        try (ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction()) {
            return readTx.read(type, TOP_PATH).checkedGet().isPresent();
        }
    }

    // TODO as in org.opendaylight.controller.md.sal.dom.broker.impl.DOMBrokerTest & Co.

}
