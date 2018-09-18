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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launcher for external etcd processes.
 * Useful for integration tests.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdLauncher implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdLauncher.class);

    // TODO write a custom log pattern matcher which reacts to I/W/E/N and uses correct log level

    // TODO refactor this and introduce an EtcdLauncherBuilder, with a start() that returns an EtcdLauncher

    private final ManagedProcess process;
    private final int clientPort;
    private final int clusterPort;

    public EtcdLauncher(Path etcdWorkingDirectory, boolean wipe) throws ManagedProcessException, IOException {
        this(etcdWorkingDirectory, wipe, findNextAvailablePort(), findNextAvailablePort());
    }

    public EtcdLauncher(Path etcdWorkingDirectory, boolean wipe, int clientPort, int clusterPort)
            throws ManagedProcessException, IOException {
        if (wipe) {
            deleteDirectory(etcdWorkingDirectory);
        }
        mkdirs(etcdWorkingDirectory);
        this.clientPort = clientPort;
        this.clusterPort = clusterPort;
        process = new ManagedProcessBuilder("etcd")
                // .addArgument("arg1");
                .setWorkingDirectory(etcdWorkingDirectory.toFile())
                .addArgument("--advertise-client-urls").addArgument(getEndpointURL())
                .addArgument("--listen-client-urls").addArgument(getEndpointURL())
                .addArgument("--listen-peer-urls").addArgument(getClusterURI().toString())
                .addArgument("--initial-cluster").addArgument("default=" + getClusterURI().toString())
                .addArgument("--initial-advertise-peer-urls").addArgument(getClusterURI().toString())
                .setDestroyOnShutdown(true)
                .build();
    }

    /**
     * Etcd server endpoint URL.
     * Typically used to pass as an argument to jetcd Client.builder().endpoints().
     */
    public final String getEndpointURL() {
        return "http://localhost:" + clientPort;
    }

    public final URI getClusterURI() {
        return URI.create("http://localhost:" + clusterPort);
    }

    @PostConstruct
    public void start() throws ManagedProcessException {
        process.startAndWaitForConsoleMessageMaxMs("embed: ready to serve client requests", 5000);
    }

    @Override
    @PreDestroy
    public void close() throws ManagedProcessException {
        if (process.isAlive()) {
            process.destroy();
        }
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private static void mkdirs(Path etcdWorkingDirectory) {
        etcdWorkingDirectory.toFile().mkdirs();
    }

    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    private static void deleteDirectory(Path directory) throws IOException {
        if (!directory.toFile().exists()) {
            LOG.info("Directory to delete did not exist: {}", directory);
            return;
        }
        try {
            try (Stream<Path> stream = Files.walk(directory)) {
                stream.sorted(Comparator.reverseOrder()).forEach(t -> {
                    try {
                        Files.delete(t);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
            LOG.info("Successfully deleted directory: {}", directory);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static int findNextAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
