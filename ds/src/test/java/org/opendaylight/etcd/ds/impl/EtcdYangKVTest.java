/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static com.google.common.truth.Truth.assertThat;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.data.ByteSequence;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.etcd.utils.ByteSequences;
import org.opendaylight.yang.gen.v1.urn.opendaylight.etcd.test.rev180628.HelloWorldContainer;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;

/**
 * Unit test for {@link EtcdYangKV}.
 *
 * @author Michael Vorburger.ch
 */
@SuppressWarnings("javadoc")
public class EtcdYangKVTest {

    @Test
    public void testYangInstanceIdentifierToAndFromByteSequence() throws EtcdException {
        @SuppressWarnings("resource") // because Client is just mocked anyway
        EtcdYangKV etcdKV = new EtcdYangKV("Test", Mockito.mock(Client.class), ByteSequences.fromBytes((byte)'t'));
        YangInstanceIdentifier path = YangInstanceIdentifier.EMPTY.node(new NodeIdentifier(HelloWorldContainer.QNAME));
        ByteSequence byteSequence = etcdKV.toByteSequence(path);
        YangInstanceIdentifier path2 = etcdKV.fromByteSequenceToYangInstanceIdentifier(byteSequence);
        assertThat(path).named(ByteSequences.asString(byteSequence)).isEqualTo(path2);
    }

}
