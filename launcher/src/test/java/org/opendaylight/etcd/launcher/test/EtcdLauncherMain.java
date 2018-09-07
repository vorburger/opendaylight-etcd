/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.launcher.test;

import ch.vorburger.exec.ManagedProcessException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import org.opendaylight.etcd.launcher.EtcdLauncher;

/**
 * Launcher with main.
 *
 * @author Michael Vorburger.ch
 */
public final class EtcdLauncherMain {

    private EtcdLauncherMain() {
    }

    public static void main(String[] args) throws ManagedProcessException, IOException {
        EtcdLauncher etcd = new EtcdLauncher(Paths.get("target/etcd-main"), true);
        etcd.start();
        waitForKeyPressToCleanlyExit();
        etcd.close();
    }

    @SuppressWarnings("checkstyle:RegExpSingleLineJava")
    private static void waitForKeyPressToCleanlyExit() throws IOException {
        // NOTE: In Eclipse, System.console() is not available.. so: (@see
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=122429)
        System.out.println("\n\nHit Enter to quit...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        reader.readLine();
    }
}
