/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.vorburger.dom2kv.jetcd;

import ch.vorburger.dom2kv.KeyValue;
import com.coreos.jetcd.data.ByteSequence;
import java.util.Optional;

/**
 * Adapter from jetcd {@link com.coreos.jetcd.data.KeyValue} to dom2kv {@link KeyValue}.
 *
 * @author Michael Vorburger.ch
 */
public class JetcdKeyValue implements KeyValue<ByteSequence, ByteSequence> {

    private final com.coreos.jetcd.data.KeyValue etcdKeyValue;

    public JetcdKeyValue(com.coreos.jetcd.data.KeyValue etcdKeyValue) {
        this.etcdKeyValue = etcdKeyValue;
    }

    @Override
    public ByteSequence key() {
        return etcdKeyValue.getKey();
    }

    @Override
    public Optional<ByteSequence> value() {
        return Optional.of(etcdKeyValue.getValue());
    }

}
