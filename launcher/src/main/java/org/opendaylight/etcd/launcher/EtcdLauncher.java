/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.launcher;

import ch.vorburger.exec.ManagedProcess;
import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.exec.ManagedProcessException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;

/**
 * Launcher for external etcd processes.
 * Useful for integration tests.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdLauncher implements AutoCloseable {

    // TODO write a custom log pattern matcher which reacts to I/W/E/N and uses correct log level

    private final ManagedProcess process;

    public EtcdLauncher() throws ManagedProcessException {
        File etcdWorkingDirectory = new File("target/etcd");
        mkdirs(etcdWorkingDirectory);
        process = new ManagedProcessBuilder("etcd")
                // .addArgument("arg1");
                .setWorkingDirectory(etcdWorkingDirectory)
                // .getEnvironment().put("ENV_VAR", "...")
                .setDestroyOnShutdown(true)
                .build();
    }

    public EtcdLauncher start() throws ManagedProcessException {
        process.startAndWaitForConsoleMessageMaxMs("embed: ready to serve client requests", 5000);
        return this;
    }

    @Override
    public void close() throws ManagedProcessException {
        process.destroy();
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private void mkdirs(File directory) {
        directory.mkdirs();
    }
}
