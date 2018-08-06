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
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.DeleteResponse;
import com.coreos.jetcd.kv.PutResponse;
import com.coreos.jetcd.options.GetOption;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.etcd.utils.ByteSequences;
import org.opendaylight.etcd.utils.LoggingKV;
import org.opendaylight.infrautils.utils.concurrent.CompletableFutures;
import org.opendaylight.infrautils.utils.function.CheckedCallable;
import org.opendaylight.infrautils.utils.function.CheckedConsumer;
import org.opendaylight.infrautils.utils.function.CheckedFunction;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

/**
 * Thingie that really writes to and reads from etcd.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
// intentionally just .impl package-local, for now
class EtcdKV implements AutoCloseable {

    // TODO remove (make optional) the use of the controller.cluster
    // NormalizedNodeDataOutput & Co. extra SIGNATURE_MARKER byte
    // this isn't a problem at this early stage, but as that is added for *EVERY*
    // key *AND* value, we could (eventually) remove it

    private final KV etcd;
    private final byte prefix;
    private final ByteSequence prefixByteSequence;

    EtcdKV(String name, Client client, byte prefix) {
        // TODO make the LoggingKV a configuration option (for performance)
        this.etcd = new LoggingKV(name + " ", requireNonNull(client, "client").getKVClient());
        this.prefix = prefix;
        this.prefixByteSequence = ByteSequence.fromBytes(bytes(prefix));
    }

    private static byte[] bytes(byte... bytes) {
        return bytes;
    }

    @Override
    public void close() {
        etcd.close();
    }

    public CompletionStage<PutResponse> put(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        return handleException(() -> {
            ByteSequence key = toByteSequence(path);
            ByteSequence value = toByteSequence(data);
            return etcd.put(key, value);
        });
    }

    public CompletionStage<DeleteResponse> delete(YangInstanceIdentifier path) {
        return handleException(() -> etcd.delete(toByteSequence(path)));
    }

/*
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(YangInstanceIdentifier path) {
        return Futures.makeChecked(
            CompletionStages.toListenableFuture(
                read(toByteSequence(path), GetOption.DEFAULT, kvs -> {
                    if (kvs.isEmpty()) {
                        return CompletableFuture.completedFuture(Optional.absent());
                    } else if (kvs.size() == 1) {
                        ByteSequence byteSequence = kvs.get(0).getValue();
                        return completedFuture(Optional.of(fromByteSequenceToNormalizedNode(byteSequence)));
                    } else {
                        throw new EtcdException("Etcd Reponse unexpectedly had more than 1 keys/values: " + path);
                    }
                })), e -> new ReadFailedException("Failed to read from etcd: " + path, e));
    }
*/

    public void applyDelete(DataTreeModification dataTree, ByteSequence key) throws EtcdException {
        YangInstanceIdentifier path = fromByteSequenceToYangInstanceIdentifier(key);
        dataTree.delete(path);
    }

    public void applyPut(DataTreeModification dataTree, ByteSequence key, ByteSequence value) throws EtcdException {
        try {
            YangInstanceIdentifier path = fromByteSequenceToYangInstanceIdentifier(key);
            PathArgument pathArgument = path.getLastPathArgument();
            NormalizedNode<?, ?> data = pathArgument instanceof AugmentationIdentifier
                    // because an AugmentationIdentifier has no node type QName
                    ? fromByteSequenceToNormalizedNode(value)
                    : fromByteSequenceToNormalizedNode(value, pathArgument.getNodeType());
            // TODO when to write and when to merge, that is the question ...
            dataTree.write(path, data);
        } catch (IllegalArgumentException e) {
            // LOG.error("readAllInto write failed: {} ➠ {}", ByteSequences.asString(kv.getKey()),
            //        ByteSequences.asString(kv.getValue()), e);
            throw new EtcdException("readAllInto write failed: " + ByteSequences.asString(key)
                    + " ➠ " + ByteSequences.asString(value), e);
        }
    }

    public void readAllInto(DataTreeModification dataTree) throws EtcdException {
        try {
            read(prefixByteSequence, GetOption.newBuilder().withPrefix(prefixByteSequence).build(), kvs -> {
                for (KeyValue kv : kvs) {
                    applyPut(dataTree, kv.getKey(), kv.getValue());
                }
                return completedFuture(null);
            }).toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new EtcdException("readAllInto() failed", e);
        }
    }

    private <T> CompletionStage<T> read(ByteSequence key, GetOption option,
            CheckedFunction<List<KeyValue>, CompletionStage<T>, EtcdException> transformer) {
        return handleException(() -> etcd.get(key, option)
            .thenCompose(getResponse -> handleException(() -> transformer.apply(getResponse.getKvs()))));
    }

    private static <T> CompletionStage<T> handleException(CheckedCallable<CompletionStage<T>, EtcdException> callable) {
        try {
            return callable.call();
        } catch (EtcdException e) {
            return CompletableFutures.completedExceptionally(e);
        }
    }

    private static NormalizedNode<?, ?> fromByteSequenceToNormalizedNode(ByteSequence byteSequence)
            throws EtcdException {
        return fromByteSequenceToNormalizedNode(byteSequence,
            dataInput -> new ShallowNormalizedNodeInputStreamReader(dataInput));
    }

    private static NormalizedNode<?, ?> fromByteSequenceToNormalizedNode(ByteSequence byteSequence, QName qname)
            throws EtcdException {
        return fromByteSequenceToNormalizedNode(byteSequence,
            dataInput -> new ShallowNormalizedNodeInputStreamReader(dataInput, qname));
    }

    private static NormalizedNode<?, ?> fromByteSequenceToNormalizedNode(ByteSequence byteSequence,
            Function<DataInputStream, NormalizedNodeDataInput> nodeDataInputProvider) throws EtcdException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(byteSequence.getBytes())) {
            try (DataInputStream dataInput = new DataInputStream(bais)) {
                return nodeDataInputProvider.apply(dataInput).readNormalizedNode();
            }
        } catch (IOException e) {
            throw new EtcdException("byte[] -> NormalizedNode failed", e);
        }
    }

    @VisibleForTesting
    YangInstanceIdentifier fromByteSequenceToYangInstanceIdentifier(ByteSequence byteSequence) throws EtcdException {
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(byteSequence.getBytes());
        byte readPrefix = dataInput.readByte();
        if (readPrefix != prefix) {
            throw new EtcdException(
                    "The read prefix does not match the expected prefix: " + readPrefix + " -VS- " + prefix);
        }

        try {
            NormalizedNodeDataInput nodeDataInput = new ShallowNormalizedNodeInputStreamReader(dataInput);
            return nodeDataInput.readYangInstanceIdentifier();
        } catch (IOException e) {
            throw new EtcdException("byte[] -> YangInstanceIdentifier failed", e);
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
                try (NormalizedNodeDataOutput nodeDataOutput = new ShallowNormalizedNodeDataOutputWriter(dataOutput)) {
                    consumer.accept(nodeDataOutput);
                    dataOutput.flush();
                    return ByteSequence.fromBytes(baos.toByteArray());
                }
            }
        }
    }

    @VisibleForTesting
    ByteSequence toByteSequence(YangInstanceIdentifier path) throws EtcdException {
        try {
            return toByteSequence(true, nodeDataOutput -> nodeDataOutput.writeYangInstanceIdentifier(path));
        } catch (IOException e) {
            throw new EtcdException("YangInstanceIdentifier toByteSequence failed: " + path.toString(), e);
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
