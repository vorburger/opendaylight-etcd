/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.DeleteResponse;
import com.coreos.jetcd.kv.PutResponse;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputOutput;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.infrautils.utils.concurrent.CompletableFutures;
import org.opendaylight.infrautils.utils.concurrent.CompletionStages;
import org.opendaylight.infrautils.utils.function.CheckedCallable;
import org.opendaylight.infrautils.utils.function.CheckedConsumer;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Thingie that really writes to and reads from etcd.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
@SuppressWarnings("deprecation")
// intentionally just .impl package-local, for now
class Etcd implements AutoCloseable {

    // TODO remove (make optional) the use of the controller.cluster
    // NormalizedNodeDataOutput & Co. extra SIGNATURE_MARKER byte
    // this isn't a problem at this early stage, but as that is added for *EVERY*
    // key and value, we could (eventually) remove it

    private final KV etcd;
    // private final Watch etcdWatch;

    private final byte prefix;

    Etcd(Client client, byte prefix) {
        this.etcd = requireNonNull(client, "client").getKVClient();
        // this.etcdWatch = client.getWatchClient();

        this.prefix = prefix;
    }

    @Override
    public void close() {
        etcd.close();
        // etcdWatch.close();
    }

    public CompletionStage<PutResponse> put(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        return handleException(() -> {
            ByteSequence key = toByteSequence(path);
            ByteSequence value = toByteSequence(data);
            return etcd.put(key, value);
        });
    }

    public CompletionStage<PutResponse> put(PathArgument pathArgument, NormalizedNode<?, ?> data) {
        return handleException(() -> {
            ByteSequence key = toByteSequence(pathArgument);
            ByteSequence value = toByteSequence(data);
            return etcd.put(key, value);
        });
    }

    public CompletionStage<DeleteResponse> delete(YangInstanceIdentifier path) {
        return handleException(() -> etcd.delete(toByteSequence(path)));
    }

    // Please swallow a headache pill ;) before proceeding to read the following code:
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(YangInstanceIdentifier path) {
        return Futures.makeChecked(
            // TODO CompletionStages.toListenableFuture from https://git.opendaylight.org/gerrit/#/c/64771/ ok?
            CompletionStages.toListenableFuture(
                handleException(() -> etcd.get(toByteSequence(path))
                    .thenCompose(getResponse -> handleException(() -> {
                        if (getResponse.getKvs().isEmpty()) {
                            return CompletableFuture.completedFuture(Optional.absent());
                        } else if (getResponse.getKvs().size() == 1) {
                            try {
                                ByteSequence byteSequence = getResponse.getKvs().get(0).getValue();
                                return completedFuture(Optional.of(fromByteSequence(byteSequence)));
                            } catch (IOException e) {
                                throw new EtcdException("byte[] -> NormalizedNode failed: " + path, e);
                            }
                        } else {
                            throw new EtcdException("Etcd Reponse had more than 1 keys/values: " + path);
                        }
                    })))), e -> new ReadFailedException("Failed to read from etcd: " + path, e));
    }

    public CheckedFuture<Boolean, ReadFailedException> exists(YangInstanceIdentifier path) {
        // TODO how to implement exists() most efficiently? could just do read(), but has overhead..
        // https://github.com/coreos/etcd/issues/4080
        throw new UnsupportedOperationException("TODO");
    }

    private static <T> CompletionStage<T> handleException(CheckedCallable<CompletionStage<T>, EtcdException> callable) {
        try {
            return callable.call();
        } catch (EtcdException e) {
            return CompletableFutures.completedExceptionally(e);
        }
    }

    private static NormalizedNode<?, ?> fromByteSequence(ByteSequence byteSequence) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(byteSequence.getBytes())) {
            try (DataInputStream dataInput = new DataInputStream(bais)) {
                NormalizedNodeDataInput nodeDataInput = NormalizedNodeInputOutput.newDataInput(dataInput);
                return nodeDataInput.readNormalizedNode();
            }
        }
    }

    private ByteSequence toByteSequence(boolean writePrefix,
            CheckedConsumer<NormalizedNodeDataOutput, IOException> consumer) throws IOException {
        // TODO Is there any advantage converting this to use Guava's I/O ?
        // ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (DataOutputStream dataOutput = new DataOutputStream(baos)) {
                if (writePrefix) {
                    dataOutput.writeByte(prefix);
                }
                try (NormalizedNodeDataOutput nodeDataOutput = NormalizedNodeInputOutput.newDataOutput(dataOutput)) {
                    consumer.accept(nodeDataOutput);
                    dataOutput.flush();
                    return ByteSequence.fromBytes(baos.toByteArray());
                }
            }
        }
    }

    private ByteSequence toByteSequence(YangInstanceIdentifier path) throws EtcdException {
        try {
            return toByteSequence(true, nodeDataOutput -> nodeDataOutput.writeYangInstanceIdentifier(path));
        } catch (IOException e) {
            throw new EtcdException("YangInstanceIdentifier toByteSequence failed: " + path.toString(), e);
        }
    }

    private ByteSequence toByteSequence(PathArgument pathArgument) throws EtcdException {
        try {
            return toByteSequence(true, nodeDataOutput -> nodeDataOutput.writePathArgument(pathArgument));
        } catch (IOException e) {
            throw new EtcdException("PathArgument toByteSequence failed: " + pathArgument.toString(), e);
        }
    }

    private ByteSequence toByteSequence(NormalizedNode<?, ?> node) throws EtcdException {
        try {
            return toByteSequence(false, nodeDataOutput -> nodeDataOutput.writeNormalizedNode(node));
        } catch (IOException e) {
            throw new EtcdException("NormalizedNode toByteSequence failed: " + node.toString(), e);
        }
    }

}
