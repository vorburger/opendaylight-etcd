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

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import io.etcd.jetcd.Client;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import javax.annotation.PostConstruct;
import org.opendaylight.infrautils.utils.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a {@link DataBroker} backed by etcd for use in production and tests.
 *
 * @author Michael Vorburger.ch
 */
public class EtcdDataBrokerWiring implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdDataBrokerWiring.class);

    private final String name;
    private final Client etcdClient;
    private final EtcdDataStore configDS;
    private final EtcdDataStore operDS;
    private final DOMDataBroker domDataBroker;
    private final EtcdWatcher watcher;
    private final RevAwaiter revAwaiter;
    private final EtcdWatcherBlockingConsumer etcdWatcherConsumer;

    /**
     * Constructor.
     *
     * @param etcdClient        connection to (cluster of) etcd server/s
     * @param nodeName          name used as prefix in logs; intended for in-process
     *                          clustering test cases, not production (where it can
     *                          be empty)
     * @param schemaService     the DOMSchemaService
     */
    public EtcdDataBrokerWiring(Client etcdClient, String nodeName, DOMSchemaService schemaService) throws Exception {
        // choice of suitable executors originally inspired from
        // org.opendaylight.mdsal.binding.dom.adapter.test.ConcurrentDataBrokerTestCustomizer
        this(etcdClient, nodeName, schemaService,
                Executors.newListeningSingleThreadExecutor("EtcdDB-commitCoordinator", LOG),
                Executors.newListeningCachedThreadPool("EtcdDB-DTCLs", LOG));
    }

    public EtcdDataBrokerWiring(Client etcdClient, String nodeName, DOMSchemaService schemaService,
            ListeningExecutorService commitCoordinatorExecutor, ListeningExecutorService dtclExecutor)
            throws Exception {
        this.name = nodeName;
        this.etcdClient = etcdClient;

        revAwaiter = new RevAwaiter(nodeName);

        // copy/pasted from org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTestCustomizer:
        configDS = createConfigurationDatastore(CONFIGURATION, dtclExecutor, schemaService);
        operDS = createConfigurationDatastore(OPERATIONAL, dtclExecutor, schemaService);
        Map<LogicalDatastoreType, DOMStore> datastores = ImmutableMap.of(CONFIGURATION, configDS, OPERATIONAL, operDS);
        // TODO use ConcurrentDOMDataBroker instead SerializedDOMDataBroker ?
        domDataBroker = new SerializedDOMDataBroker(datastores, commitCoordinatorExecutor);

        etcdWatcherConsumer = new EtcdWatcherBlockingConsumer(
                new EtcdWatcherSplittingConsumer(Optional.of(revAwaiter),
                        ImmutableMap.of(CONFIGURATION_PREFIX, configDS, OPERATIONAL_PREFIX, operDS)));
        watcher = new EtcdWatcher(nodeName, etcdClient, EtcdDataStore.BASE_PREFIX, etcdWatcherConsumer);
    }

    @PostConstruct
    public void init() throws Exception {
        long revNow = EtcdServerUtils.getServerRevision(etcdClient.getKVClient());
        configDS.init(revNow);
        operDS.init(revNow);
        revAwaiter.update(revNow);
        // start watching for changes one revision AFTER what we got
        watcher.start(revNow + 1);
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
    }

    public DOMDataBroker getDOMDataBroker() {
        return domDataBroker;
    }

    public TestTool getTestTool() {
        return etcdWatcherConsumer;
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
