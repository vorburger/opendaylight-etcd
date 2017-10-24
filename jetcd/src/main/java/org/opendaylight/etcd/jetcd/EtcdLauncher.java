/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.jetcd;

import ch.vorburger.exec.ManagedProcess;
import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;
import java.io.File;

/**
 * Launcher for external etcd processes.
 * Useful for integration tests.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdLauncher {

    // TODO write a custom log pattern matcher which reacts to I/W/E/N and uses correct log level

    private ManagedProcess process;

    public EtcdLauncher start() throws ManagedProcessException {
        File etcdWorkingDirectory = new File("target/etcd");
        etcdWorkingDirectory.mkdirs();
        process = new ManagedProcessBuilder("etcd")
                // .addArgument("arg1");
                .setWorkingDirectory(etcdWorkingDirectory)
                // .getEnvironment().put("ENV_VAR", "...")
                .setDestroyOnShutdown(true)
                .build();
        process.startAndWaitForConsoleMessageMaxMs("embed: ready to serve client requests", 5000);
        return this;
    }

    public EtcdLauncher stop() throws ManagedProcessException {
        process.destroy();
        return this;
    }
}
