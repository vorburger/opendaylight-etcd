/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * This package contains code copy/pasted verbatim from
 * org.opendaylight.mdsal.dom.store.inmemory. It's basically unmodified except
 * for https://git.opendaylight.org/gerrit/#/c/73217/ to make
 * validate/prepare/commit methods in InMemoryDOMDataStore protected (plus
 * adjustments for Error-Prone).
 * TODO remove this after refactoring in mdsal.
 */
package org.opendaylight.etcd.ds.inmemory.copypaste;
