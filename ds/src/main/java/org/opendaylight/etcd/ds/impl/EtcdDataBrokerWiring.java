/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.etcd.ds.impl;

import static org.opendaylight.etcd.ds.impl.EtcdDataStore.CONFIGURATION_PREFIX;
import static org.opendaylight.etcd.ds.impl.EtcdDataStore.OPERATIONAL_PREFIX;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import javassist.ClassPool;
import javax.annotation.PostConstruct;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreConfigProperties;

/**
 * Provides a {@link DataBroker} backed by etcd for use in production and tests.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdDataBrokerWiring implements AutoCloseable {

    private final String name;
    private final Client etcdClient;
    private final EtcdDataStore configDS;
    private final EtcdDataStore operDS;
    private final DOMDataBroker domDataBroker;
    private final DataBroker dataBroker;
    private final EtcdWatcher watcher;
    private final RevAwaiter revAwaiter;

    /**
     * Constructor.
     *
     * @param etcdClientBuilder connection parameters to (cluster of) etcd server/s
     * @param nodeName          name used as prefix in logs; intended for in-process
     *                          clustering test cases, not production (where it can
     *                          be empty)
     */
    public EtcdDataBrokerWiring(ClientBuilder etcdClientBuilder, String nodeName,
            ListeningExecutorService commitCoordinatorExecutor, ListeningExecutorService dtclExecutor,
            DOMSchemaService schemaService, ClassLoadingStrategy loading)
            throws Exception {
        this.name = nodeName;
        this.etcdClient = etcdClientBuilder.build();

        revAwaiter = new RevAwaiter();

        // copy/pasted from org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer:
        configDS = createConfigurationDatastore(CONFIGURATION, dtclExecutor, schemaService);
        operDS = createConfigurationDatastore(OPERATIONAL, dtclExecutor, schemaService);
        Map<LogicalDatastoreType, DOMStore> datastores = ImmutableMap.of(CONFIGURATION, configDS, OPERATIONAL, operDS);
        // TODO use ConcurrentDOMDataBroker instead SerializedDOMDataBroker ?
        domDataBroker = new SerializedDOMDataBroker(datastores, commitCoordinatorExecutor);

        watcher = new EtcdWatcher(nodeName, etcdClient, EtcdDataStore.BASE_PREFIX, new EtcdWatcherSplittingConsumer(
                Optional.of(revAwaiter), ImmutableMap.of(CONFIGURATION_PREFIX, configDS, OPERATIONAL_PREFIX, operDS)));

        ClassPool pool = ClassPool.getDefault();
        DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(pool));
        BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(generator);
        BindingToNormalizedNodeCodec bindingToNormalized = new BindingToNormalizedNodeCodec(loading, codecRegistry);
        schemaService.registerSchemaContextListener(bindingToNormalized);
        dataBroker = new BindingDOMDataBrokerAdapter(domDataBroker, bindingToNormalized);
    }

    @PostConstruct
    public void init() throws Exception {
        configDS.init();
        operDS.init();

        long revNow = EtcdServerUtils.getServerRevision(etcdClient.getKVClient());
        watcher.start(revNow);
        revAwaiter.update(revNow);
    }

    @Override
    public void close() throws Exception {
        if (watcher != null) {
            watcher.close();
        }
        if (operDS != null) {
            operDS.close();
        }
        if (configDS != null) {
            configDS.close();
        }
        if (etcdClient != null) {
            etcdClient.close();
        }
    }

    public DOMDataBroker getDOMDataBroker() {
        return domDataBroker;
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }
/*
    public EtcdDataStore getConfigurationDataStore() {
        return configurationDataStore;
    }

    public EtcdDataStore getOperationalDataStore() {
        return operationalDataStore;
    }
*/
    private EtcdDataStore createConfigurationDatastore(LogicalDatastoreType type,
            ExecutorService dataTreeChangeListenerExecutor, DOMSchemaService schemaService) {
        EtcdDataStore store = new EtcdDataStore(name, type, dataTreeChangeListenerExecutor,
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE, etcdClient, true,
                revAwaiter);
        schemaService.registerSchemaContextListener(store);
        return store;
    }
}
