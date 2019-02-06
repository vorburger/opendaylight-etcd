/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.eos.impl;

import java.util.Optional;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;

/**
 * DOMEntityOwnershipService which delegates to another one.
 *
 * @author Michael Vorburger.ch
 */
class DelegatingDOMEntityOwnershipService implements DOMEntityOwnershipService {

    private final DOMEntityOwnershipService delegate;

    DelegatingDOMEntityOwnershipService(DOMEntityOwnershipService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<EntityOwnershipState> getOwnershipState(DOMEntity arg0) {
        return delegate.getOwnershipState(arg0);
    }

    @Override
    public boolean isCandidateRegistered(DOMEntity arg0) {
        return delegate.isCandidateRegistered(arg0);
    }

    @Override
    public DOMEntityOwnershipCandidateRegistration registerCandidate(DOMEntity arg0)
            throws CandidateAlreadyRegisteredException {
        return delegate.registerCandidate(arg0);
    }

    @Override
    public DOMEntityOwnershipListenerRegistration registerListener(String arg0, DOMEntityOwnershipListener arg1) {
        return delegate.registerListener(arg0, arg1);
    }

}
