/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.launcher;

import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
import java.net.URI;
import java.nio.file.Path;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Launcher for external etcd processes.
 * Useful for integration tests.
 *
 * @deprecated Use io.etcd.jetcd.launcher.EtcdClusterFactory directly.
 *
 * @author Michael Vorburger.ch
 */
@Deprecated
public class EtcdLauncher implements AutoCloseable {

    private final EtcdCluster etcdCluster;

    // NB: wipe doesn't work anymore now; and clientPort & clusterPort are ignored

    public EtcdLauncher(Path etcdWorkingDirectory, boolean wipe) {
        this(etcdWorkingDirectory, wipe, 0, 0);
    }

    public EtcdLauncher(Path etcdWorkingDirectory, boolean wipe, int clientPort, int clusterPort) {
        if (!wipe) {
            throw new IllegalArgumentException("Not wiping is no longer supported since running in a fresh container");
        }
        etcdCluster = EtcdClusterFactory.buildCluster(EtcdLauncher.class.getName(), 1, false, false);
    }

    /**
     * Etcd server endpoint URL.
     * Typically used to pass as an argument to jetcd Client.builder().endpoints().
     */
    public final String getEndpointURL() {
        return getClusterURI().toString();
    }

    public final URI getClusterURI() {
        return etcdCluster.getClientEndpoints().get(0);
    }

    @PostConstruct
    public void start() {
        etcdCluster.start();
    }

    @Override
    @PreDestroy
    public void close() {
        etcdCluster.close();
    }
}
