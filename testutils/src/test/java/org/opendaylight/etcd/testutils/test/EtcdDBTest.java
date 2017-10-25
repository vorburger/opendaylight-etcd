/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils.test;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.etcd.testutils.TestEtcdDataBrokersProvider;

/**
 * Tests the etcd-based data broker.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdDBTest {

//    private static DataBroker dataBroker;

//    @BeforeClass
//    public static void beforeClass() {
//        dataBroker = new TestEtcdDataBrokersProvider().getDataBroker();
//    }

    @Test
    public void testSetUp() {
        DataBroker dataBroker = new TestEtcdDataBrokersProvider().getDataBroker();
        assertThat(dataBroker).isNotNull();
    }

}
