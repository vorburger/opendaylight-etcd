/*
 * Copyright (c) 2019 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.testutils;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.opendaylight.etcd.launcher.EtcdLauncher;

/**
 * JUnit Rule for {@link EtcdLauncher}.
 *
 * @deprecated Use io.etcd.jetcd.launcher.junit.EtcdClusterResource instead.
 *
 * @author Michael Vorburger.ch
 */
@Deprecated
public class EtcdLauncherRule implements TestRule {

    // TODO upstream this into jetcd ...

    protected final Path etcdWorkingDirectory;
    protected EtcdLauncher currentEtcdServer;

    public EtcdLauncherRule() {
        this(Paths.get("target/etcd"));
    }

    public EtcdLauncherRule(Path etcdWorkingDirectory) {
        this.etcdWorkingDirectory = etcdWorkingDirectory;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                try (EtcdLauncher etcdServer = new EtcdLauncher(etcdWorkingDirectory, true)) {
                    etcdServer.start();
                    currentEtcdServer = etcdServer;

                    statement.evaluate();
                }
                currentEtcdServer = null;
            }
        };
    }

    public Collection<URI> getClusterURIs() {
        if (currentEtcdServer != null) {
            return Collections.singleton(currentEtcdServer.getClusterURI());
        } else {
            throw new IllegalStateException("getClusterURIs() can only be called with active rule");
        }
    }
}
