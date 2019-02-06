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
import static org.opendaylight.etcd.utils.ByteSequences.toStringable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Txn;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import javax.annotation.CheckReturnValue;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.etcd.ds.stream.copypaste.dependencies.NormalizedNodeDataInput;
import org.opendaylight.etcd.ds.stream.copypaste.dependencies.NormalizedNodeDataOutput;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes to and reads from etcd.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
// intentionally just .impl package-local, for now
class EtcdYangKV implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdYangKV.class);

    // Max. time (in milliseconds) we're willing to wait for replies from etcd server
    // TODO In an ideal world, we'd like to be 101% async everywhere here, and never do a blocking get() ...
    // NB surprisingly 300ms is way too little and often fails; so let's use 3s
    // TODO make this a configuration option (perhaps in upstream jetcd-osgi?)
    static final long TIMEOUT_MS = 3000;

    // TODO remove (make optional) the use of the controller.cluster
    // NormalizedNodeDataOutput & Co. extra SIGNATURE_MARKER byte
    // this isn't a problem at this early stage, but as that is added for *EVERY*
    // key *AND* value, we could (eventually) remove it

    private final KV etcd;
    private final byte[] prefixByteArray;
    private final ByteSequence prefixByteSequence;
    private final String name;

    EtcdYangKV(String name, Client client, ByteSequence prefix) {
        // TODO make the LoggingKV a configuration option (for performance)
        this.name = name;
        this.etcd = new LoggingKV(name + " ", requireNonNull(client, "client").getKVClient());
        this.prefixByteArray = prefix.getBytes();
        this.prefixByteSequence = prefix;
    }

    @Override
    @PreDestroy
    public void close() {
        etcd.close();
    }

    public EtcdTxn newTransaction() {
        return new EtcdTxn();
    }
/*
    public @CheckReturnValue CompletionStage<PutResponse> put(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        return handleException(() -> {
            ByteSequence key = toByteSequence(path);
            ByteSequence value = toByteSequence(data);
            return etcd.put(key, value);
        });
    }

    public @CheckReturnValue CompletionStage<DeleteResponse> delete(YangInstanceIdentifier path) {
        return handleException(() -> etcd.delete(toByteSequence(path)));
    }

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

    public void readAllInto(long rev, DataTreeModification dataTree) throws EtcdException {
        try {
            GetOption getOpt = GetOption.newBuilder().withRevision(rev).withPrefix(prefixByteSequence).build();
            read(prefixByteSequence, getOpt, kvs -> {
                for (KeyValue kv : kvs) {
                    applyPut(dataTree, kv.getKey(), kv.getValue());
                }
                return completedFuture(null);
            }).toCompletableFuture().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new EtcdException("readAllInto() failed", e);
        }
    }

    private @CheckReturnValue <T> CompletionStage<T> read(ByteSequence key, GetOption option,
            CheckedFunction<List<KeyValue>, CompletionStage<T>, EtcdException> transformer) {
        return handleException(() -> etcd.get(key, option)
            .thenCompose(getResponse -> handleException(() -> transformer.apply(getResponse.getKvs()))));
    }

    private static @CheckReturnValue <T>
        CompletionStage<T> handleException(CheckedCallable<CompletionStage<T>, EtcdException> callable) {
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
        for (byte prefix : prefixByteArray) {
            byte readPrefix = dataInput.readByte();
            if (readPrefix != prefix) {
                throw new EtcdException(
                        "The read prefix does not match the expected prefix: " + readPrefix + " -VS- " + prefix);
            }
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
                    dataOutput.write(prefixByteArray, 0, prefixByteArray.length);
                }
                try (NormalizedNodeDataOutput nodeDataOutput = new ShallowNormalizedNodeDataOutputWriter(dataOutput)) {
                    consumer.accept(nodeDataOutput);
                    dataOutput.flush();
                    return ByteSequence.from(baos.toByteArray());
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

    public class EtcdTxn {

        private final Txn txn;
        private final List<Op> opsList;

        EtcdTxn() {
            txn = etcd.txn();
            // TODO txn.If();
            opsList = new ArrayList<>();
        }

        public void put(YangInstanceIdentifier path, NormalizedNode<?, ?> data) throws EtcdException {
            ByteSequence key = toByteSequence(path);
            ByteSequence value = toByteSequence(data);
            opsList.add(Op.put(key, value, PutOption.DEFAULT));
            // TODO remove logging here once LoggingKV can correctly support txn() [missing getters]
            LOG.info("{} TXN put: {} ➠ {}", name, toStringable(key), toStringable(value));
        }

        public void delete(YangInstanceIdentifier path) throws EtcdException {
            ByteSequence key = toByteSequence(path);
            opsList.add(Op.delete(key, DeleteOption.DEFAULT));
            // TODO remove logging here once LoggingKV can correctly support txn() [missing getters]
            LOG.info("{} TXN delete: {}", name, toStringable(key));
        }

        public @CheckReturnValue CompletionStage<TxnResponse> commit() {
            txn.Then(opsList.toArray(new Op[opsList.size()]));
            return txn.commit();
        }
    }
}
