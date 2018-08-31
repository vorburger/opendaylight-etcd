/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.stream.copypaste;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.opendaylight.etcd.ds.stream.copypaste.dependencies.NormalizedNodeDataOutput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractNormalizedNodeDataOutput implements NormalizedNodeDataOutput, NormalizedNodeStreamWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNormalizedNodeDataOutput.class);

    private final DataOutput output;

    private NormalizedNodeWriter normalizedNodeWriter;
    private boolean headerWritten;
    private QName lastLeafSetQName;

    AbstractNormalizedNodeDataOutput(DataOutput output) {
        this.output = Preconditions.checkNotNull(output);
    }

    protected void ensureHeaderWritten() throws IOException {
        if (!headerWritten) {
            output.writeByte(TokenTypes.SIGNATURE_MARKER);
            output.writeShort(streamVersion());
            headerWritten = true;
        }
    }

    protected abstract short streamVersion();

    protected abstract void writeQName(QName qname) throws IOException;

    protected abstract void writeString(String string) throws IOException;

    @Override
    public final void write(int value) throws IOException {
        ensureHeaderWritten();
        output.write(value);
    }

    @Override
    public final void write(byte[] bytes) throws IOException {
        ensureHeaderWritten();
        output.write(bytes);
    }

    @Override
    public final void write(byte[] bytes, int off, int len) throws IOException {
        ensureHeaderWritten();
        output.write(bytes, off, len);
    }

    @Override
    public final void writeBoolean(boolean value) throws IOException {
        ensureHeaderWritten();
        output.writeBoolean(value);
    }

    @Override
    public final void writeByte(int value) throws IOException {
        ensureHeaderWritten();
        output.writeByte(value);
    }

    @Override
    public final void writeShort(int value) throws IOException {
        ensureHeaderWritten();
        output.writeShort(value);
    }

    @Override
    public final void writeChar(int value) throws IOException {
        ensureHeaderWritten();
        output.writeChar(value);
    }

    @Override
    public final void writeInt(int value) throws IOException {
        ensureHeaderWritten();
        output.writeInt(value);
    }

    @Override
    public final void writeLong(long value) throws IOException {
        ensureHeaderWritten();
        output.writeLong(value);
    }

    @Override
    public final void writeFloat(float value) throws IOException {
        ensureHeaderWritten();
        output.writeFloat(value);
    }

    @Override
    public final void writeDouble(double value) throws IOException {
        ensureHeaderWritten();
        output.writeDouble(value);
    }

    @Override
    public final void writeBytes(String str) throws IOException {
        ensureHeaderWritten();
        output.writeBytes(str);
    }

    @Override
    public final void writeChars(String str) throws IOException {
        ensureHeaderWritten();
        output.writeChars(str);
    }

    @Override
    public final void writeUTF(String str) throws IOException {
        ensureHeaderWritten();
        output.writeUTF(str);
    }

    private NormalizedNodeWriter normalizedNodeWriter() {
        if (normalizedNodeWriter == null) {
            normalizedNodeWriter = newNormalizedNodeWriter();
        }

        return normalizedNodeWriter;
    }

    protected NormalizedNodeWriter newNormalizedNodeWriter() {
        return NormalizedNodeWriter.forStreamWriter(this);
    }

    @Override
    public void writeNormalizedNode(NormalizedNode<?, ?> node) throws IOException {
        ensureHeaderWritten();
        normalizedNodeWriter().write(node);
    }

    @Override
    public void leafNode(NodeIdentifier name, Object value) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.trace("Writing a new leaf node");
        startNode(NodeTypes.LEAF_NODE, name.getNodeType());

        writeObject(value);
    }

    @Override
    public void startLeafSet(NodeIdentifier name, int childSizeHint)

            throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.trace("Starting a new leaf set");

        lastLeafSetQName = name.getNodeType();
        startNode(NodeTypes.LEAF_SET, name.getNodeType());
    }

    @Override
    public void startOrderedLeafSet(NodeIdentifier name, int childSizeHint)
            throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.trace("Starting a new ordered leaf set");

        lastLeafSetQName = name.getNodeType();
        startNode(NodeTypes.ORDERED_LEAF_SET, name.getNodeType());
    }

    @Override
    public void leafSetEntryNode(QName name, Object value) throws IOException, IllegalArgumentException {
        LOG.trace("Writing a new leaf set entry node");

        output.writeByte(NodeTypes.LEAF_SET_ENTRY_NODE);

        // lastLeafSetQName is set if the parent LeafSetNode was previously written. Otherwise this is a
        // stand alone LeafSetEntryNode so write out it's name here.
        if (lastLeafSetQName == null) {
            writeQName(name);
        }

        writeObject(value);
    }

    @Override
    public void startContainerNode(NodeIdentifier name, int childSizeHint)
            throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");

        LOG.trace("Starting a new container node");

        startNode(NodeTypes.CONTAINER_NODE, name.getNodeType());
    }

    @Override
    public void startYangModeledAnyXmlNode(NodeIdentifier name, int childSizeHint)
            throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");

        LOG.trace("Starting a new yang modeled anyXml node");

        startNode(NodeTypes.YANG_MODELED_ANY_XML_NODE, name.getNodeType());
    }

    @Override
    public void startUnkeyedList(NodeIdentifier name, int childSizeHint)
            throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.trace("Starting a new unkeyed list");

        startNode(NodeTypes.UNKEYED_LIST, name.getNodeType());
    }

    @Override
    public void startUnkeyedListItem(NodeIdentifier name, int childSizeHint)
            throws IOException, IllegalStateException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.trace("Starting a new unkeyed list item");

        startNode(NodeTypes.UNKEYED_LIST_ITEM, name.getNodeType());
    }

    @Override
    public void startMapNode(NodeIdentifier name, int childSizeHint)
            throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.trace("Starting a new map node");

        startNode(NodeTypes.MAP_NODE, name.getNodeType());
    }

    @Override
    public void startMapEntryNode(NodeIdentifierWithPredicates identifier, int childSizeHint)
            throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(identifier, "Node identifier should not be null");
        LOG.trace("Starting a new map entry node");
        startNode(NodeTypes.MAP_ENTRY_NODE, identifier.getNodeType());

        writeKeyValueMap(identifier.getKeyValues());

    }

    @Override
    public void startOrderedMapNode(NodeIdentifier name, int childSizeHint)
            throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.trace("Starting a new ordered map node");

        startNode(NodeTypes.ORDERED_MAP_NODE, name.getNodeType());
    }

    @Override
    public void startChoiceNode(NodeIdentifier name, int childSizeHint)
            throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.trace("Starting a new choice node");

        startNode(NodeTypes.CHOICE_NODE, name.getNodeType());
    }

    @Override
    public void startAugmentationNode(AugmentationIdentifier identifier)
            throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(identifier, "Node identifier should not be null");
        LOG.trace("Starting a new augmentation node");

        output.writeByte(NodeTypes.AUGMENTATION_NODE);
        writeQNameSet(identifier.getPossibleChildNames());
    }

    @Override
    public void anyxmlNode(NodeIdentifier name, Object value) throws IOException, IllegalArgumentException {
        Preconditions.checkNotNull(name, "Node identifier should not be null");
        LOG.trace("Writing any xml node");

        startNode(NodeTypes.ANY_XML_NODE, name.getNodeType());

        try {
            StreamResult xmlOutput = new StreamResult(new StringWriter());
            TransformerFactory.newInstance().newTransformer().transform((DOMSource)value, xmlOutput);
            writeObject(xmlOutput.getWriter().toString());
        } catch (TransformerException | TransformerFactoryConfigurationError e) {
            throw new IOException("Error writing anyXml", e);
        }
    }

    @Override
    public void endNode() throws IOException, IllegalStateException {
        LOG.trace("Ending the node");
        lastLeafSetQName = null;
        output.writeByte(NodeTypes.END_NODE);
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    @Override
    public void flush() throws IOException {
        if (output instanceof OutputStream) {
            ((OutputStream)output).flush();
        }
    }

    protected void startNode(byte nodeType, QName qname) throws IOException {
        Preconditions.checkNotNull(qname, "QName of node identifier should not be null.");
        startNode(nodeType);
        // Write Start Tag
        writeQName(qname);
    }

    protected void startNode(byte nodeType) throws IOException {
        ensureHeaderWritten();

        // First write the type of node
        output.writeByte(nodeType);
    }

    private void writeObjSet(Set<?> set) throws IOException {
        output.writeInt(set.size());
        for (Object o : set) {
            Preconditions.checkArgument(o instanceof String, "Expected value type to be String but was %s (%s)",
                o.getClass(), o);

            writeString((String) o);
        }
    }

    @Override
    public void writeSchemaPath(SchemaPath path) throws IOException {
        ensureHeaderWritten();
        output.writeBoolean(path.isAbsolute());

        Collection<QName> qnames = path.getPath();
        output.writeInt(qnames.size());
        for (QName qname : qnames) {
            writeQName(qname);
        }
    }

    @Override
    public void writeYangInstanceIdentifier(YangInstanceIdentifier identifier) throws IOException {
        ensureHeaderWritten();
        writeYangInstanceIdentifierInternal(identifier);
    }

    private void writeYangInstanceIdentifierInternal(YangInstanceIdentifier identifier) throws IOException {
        Collection<PathArgument> pathArguments = identifier.getPathArguments();
        output.writeInt(pathArguments.size());

        for (PathArgument pathArgument : pathArguments) {
            writePathArgument(pathArgument);
        }
    }

    @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST",
            justification = "The casts in the switch clauses are indirectly confirmed via the determination of 'type'.")
    @Override
    public void writePathArgument(PathArgument pathArgument) throws IOException {

        byte type = PathArgumentTypes.getSerializablePathArgumentType(pathArgument);

        output.writeByte(type);

        switch (type) {
            case PathArgumentTypes.NODE_IDENTIFIER:

                NodeIdentifier nodeIdentifier = (NodeIdentifier) pathArgument;

                writeQName(nodeIdentifier.getNodeType());
                break;

            case PathArgumentTypes.NODE_IDENTIFIER_WITH_PREDICATES:

                NodeIdentifierWithPredicates nodeIdentifierWithPredicates =
                    (NodeIdentifierWithPredicates) pathArgument;
                writeQName(nodeIdentifierWithPredicates.getNodeType());

                writeKeyValueMap(nodeIdentifierWithPredicates.getKeyValues());
                break;

            case PathArgumentTypes.NODE_IDENTIFIER_WITH_VALUE :

                NodeWithValue<?> nodeWithValue = (NodeWithValue<?>) pathArgument;

                writeQName(nodeWithValue.getNodeType());
                writeObject(nodeWithValue.getValue());
                break;

            case PathArgumentTypes.AUGMENTATION_IDENTIFIER :

                AugmentationIdentifier augmentationIdentifier = (AugmentationIdentifier) pathArgument;

                // No Qname in augmentation identifier
                writeQNameSet(augmentationIdentifier.getPossibleChildNames());
                break;
            default :
                throw new IllegalStateException("Unknown node identifier type is found : "
                        + pathArgument.getClass().toString());
        }
    }

    private void writeKeyValueMap(Map<QName, Object> keyValueMap) throws IOException {
        if (keyValueMap != null && !keyValueMap.isEmpty()) {
            output.writeInt(keyValueMap.size());

            for (Map.Entry<QName, Object> entry : keyValueMap.entrySet()) {
                writeQName(entry.getKey());
                writeObject(entry.getValue());
            }
        } else {
            output.writeInt(0);
        }
    }

    private void writeQNameSet(Set<QName> children) throws IOException {
        // Write each child's qname separately, if list is empty send count as 0
        if (children != null && !children.isEmpty()) {
            output.writeInt(children.size());
            for (QName qname : children) {
                writeQName(qname);
            }
        } else {
            LOG.debug("augmentation node does not have any child");
            output.writeInt(0);
        }
    }

    private void writeObject(Object value) throws IOException {

        byte type = ValueTypes.getSerializableType(value);
        // Write object type first
        output.writeByte(type);

        switch (type) {
            case ValueTypes.BOOL_TYPE:
                output.writeBoolean((Boolean) value);
                break;
            case ValueTypes.QNAME_TYPE:
                writeQName((QName) value);
                break;
            case ValueTypes.INT_TYPE:
                output.writeInt((Integer) value);
                break;
            case ValueTypes.BYTE_TYPE:
                output.writeByte((Byte) value);
                break;
            case ValueTypes.LONG_TYPE:
                output.writeLong((Long) value);
                break;
            case ValueTypes.SHORT_TYPE:
                output.writeShort((Short) value);
                break;
            case ValueTypes.BITS_TYPE:
                writeObjSet((Set<?>) value);
                break;
            case ValueTypes.BINARY_TYPE:
                byte[] bytes = (byte[]) value;
                output.writeInt(bytes.length);
                output.write(bytes);
                break;
            case ValueTypes.YANG_IDENTIFIER_TYPE:
                writeYangInstanceIdentifierInternal((YangInstanceIdentifier) value);
                break;
            case ValueTypes.EMPTY_TYPE:
                break;
            case ValueTypes.STRING_BYTES_TYPE:
                byte[] valueBytes = value.toString().getBytes(StandardCharsets.UTF_8);
                output.writeInt(valueBytes.length);
                output.write(valueBytes);
                break;
            default:
                output.writeUTF(value.toString());
                break;
        }
    }
}
