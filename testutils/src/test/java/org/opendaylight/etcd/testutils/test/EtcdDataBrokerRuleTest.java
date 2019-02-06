/*
 * Copyright (c) 2019 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils.test;

import static com.google.common.truth.Truth.assertThat;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.etcd.testutils.EtcdDataBrokerRule;
import org.opendaylight.etcd.testutils.EtcdLauncherRule;

/**
 * Test for the {@link EtcdDataBrokerRule}.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdDataBrokerRuleTest {

    public static @ClassRule EtcdLauncherRule etcdLauncher = new EtcdLauncherRule();

    public @Rule EtcdDataBrokerRule dbRule = new EtcdDataBrokerRule(etcdLauncher);

    @Test
    public void testDataBroker() {
        assertThat(dbRule.getDataBroker()).isNotNull();
    }
}
