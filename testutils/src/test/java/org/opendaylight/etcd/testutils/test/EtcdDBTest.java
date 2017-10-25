/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.etcd.testutils.TestEtcdDataBrokersProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Tests the etcd-based data broker.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdDBTest {

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);

    private static DataBroker dataBroker;

    @BeforeClass
    public static void beforeClass() {
        dataBroker = new TestEtcdDataBrokersProvider().getDataBroker();
    }

    @Test
    public void testSetUp() {
        assertThat(dataBroker).isNotNull();
    }

    // as in org.opendaylight.controller.md.sal.binding.test.tests.AbstractDataBrokerTestTest

    @Test
    public void bPutSomethingIntoDataStore() throws Exception {
        writeInitialState();
        assertThat(isTopInDataStore()).isTrue();
    }

    @Test
    public void cEnsureDataStoreIsEmptyAgainInNewTest() throws ReadFailedException {
        assertThat(isTopInDataStore()).isFalse();
    }

    private void writeInitialState() throws Exception {
        WriteTransaction initialTx = dataBroker.newWriteOnlyTransaction();
        initialTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, new TopBuilder().build());
        TreeComplexUsesAugment fooAugment = new TreeComplexUsesAugmentBuilder()
                .setContainerWithUses(new ContainerWithUsesBuilder().setLeafFromGrouping("foo").build()).build();
        initialTx.put(LogicalDatastoreType.OPERATIONAL, path(TOP_FOO_KEY), topLevelList(TOP_FOO_KEY, fooAugment));
        initialTx.submit().get();
    }

    private boolean isTopInDataStore() throws ReadFailedException {
        try (ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction()) {
            return readTx.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).checkedGet().isPresent();
        }
    }

    // TODO as in org.opendaylight.controller.md.sal.dom.broker.impl.DOMBrokerTest & Co.

}
