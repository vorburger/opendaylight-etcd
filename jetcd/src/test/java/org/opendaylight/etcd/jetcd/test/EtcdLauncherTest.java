/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.jetcd.test;

import ch.vorburger.exec.ManagedProcessException;
import org.junit.Test;
import org.opendaylight.etcd.jetcd.EtcdLauncher;

/**
 * Test for {@link EtcdLauncher}.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdLauncherTest {

    @Test
    public void testLaunchEtcd() throws ManagedProcessException {
        // TODO refactor to avoid "// Resource leak: '<unassigned Closeable value>' is never closed"
        // EtcdLauncherBuilder start should return EtcdLauncher
        new EtcdLauncher().start().close();
    }

}
