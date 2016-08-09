/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Tiry
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.core;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.ALL_FIELDS;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.config.ElasticSearchIndexConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchLocalConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchRemoteConfig;
import org.nuxeo.runtime.api.Framework;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * @since 6.0
 */
public class ElasticSearchAdminImpl implements ElasticSearchAdmin {
    private static final Log log = LogFactory.getLog(ElasticSearchAdminImpl.class);

    private static final String TIMEOUT_WAIT_FOR_CLUSTER = "30s";

    final AtomicInteger totalCommandProcessed = new AtomicInteger(0);

    private final Map<String, String> indexNames = new HashMap<>();

    private final Map<String, String> repoNames = new HashMap<>();

    private final Map<String, ElasticSearchIndexConfig> indexConfig;

    private Node localNode;

    private Client client;

    private boolean indexInitDone = false;

    private final ElasticSearchLocalConfig localConfig;

    private final ElasticSearchRemoteConfig remoteConfig;

    private String[] includeSourceFields;

    private String[] excludeSourceFields;

    private boolean embedded = true;

    private List<String> repositoryInitialized = new ArrayList<>();

    /**
     * Init the admin service, remote configuration if not null will take precedence over local embedded configuration.
     */
    public ElasticSearchAdminImpl(ElasticSearchLocalConfig localConfig, ElasticSearchRemoteConfig remoteConfig,
            Map<String, ElasticSearchIndexConfig> indexConfig) {
        this.remoteConfig = remoteConfig;
        this.localConfig = localConfig;
        this.indexConfig = indexConfig;
        connect();
        initializeIndexes();
    }

    private void connect() {
        if (client != null) {
            return;
        }
        if (remoteConfig != null) {
            client = connectToRemote(remoteConfig);
            embedded = false;
        } else {
            localNode = createEmbeddedNode(localConfig);
            client = connectToEmbedded();
            embedded = true;
        }
        checkClusterHealth();
        log.info("ES Connected");
    }

    public void disconnect() {
        if (client != null) {
            client.close();
            client = null;
            indexInitDone = false;
            log.info("ES Disconnected");
        }
        if (localNode != null) {
            localNode.close();
            localNode = null;
            log.info("ES embedded Node Stopped");
        }
    }

    private Node createEmbeddedNode(ElasticSearchLocalConfig conf) {
        log.info("ES embedded Node Initializing (local in JVM)");
        if (conf == null) {
            throw new IllegalStateException("No embedded configuration defined");
        }
        if (!Framework.isTestModeSet()) {
            log.warn("Elasticsearch embedded configuration is ONLY for testing"
                    + " purpose. You need to create a dedicated Elasticsearch" + " cluster for production.");
        }
        Builder sBuilder = Settings.settingsBuilder();
        sBuilder.put("http.enabled", conf.httpEnabled())
                .put("network.host", conf.getNetworkHost())
                .put("path.home", conf.getHomePath())
                .put("path.data", conf.getDataPath())
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
                .put("cluster.name", conf.getClusterName())
                .put("node.name", conf.getNodeName())
                .put("http.netty.worker_count", 4)
                .put("http.cors.enabled", true)
                .put("cluster.routing.allocation.disk.threshold_enabled", false)
                .put("http.port", conf.getHttpPort());
        if (conf.getIndexStorageType() != null) {
            sBuilder.put("index.store.type", conf.getIndexStorageType());
        }
        Settings settings = sBuilder.build();
        log.debug("Using settings: " + settings.toDelimitedString(','));
        Node ret = NodeBuilder.nodeBuilder().local(true).settings(settings).node();
        assert ret != null : "Can not create an embedded ES Node";
        return ret;
    }

    private Client connectToEmbedded() {
        log.info("Connecting to embedded ES");
        Client ret = localNode.start().client();
        assert ret != null : "Can not connect to embedded ES Node";
        return ret;
    }

    private Client connectToRemote(ElasticSearchRemoteConfig config) {
        log.info("Connecting to remote ES cluster: " + config);
        Builder builder = Settings.settingsBuilder()
                                  .put("cluster.name", config.getClusterName())
                                  .put("client.transport.nodes_sampler_interval", config.getSamplerInterval())
                                  .put("client.transport.ping_timeout", config.getPingTimeout())
                                  .put("client.transport.ignore_cluster_name", config.isIgnoreClusterName())
                                  .put("client.transport.sniff", config.isClusterSniff());
        Settings settings = builder.build();
        if (log.isDebugEnabled()) {
            log.debug("Using settings: " + settings.toDelimitedString(','));
        }
        TransportClient ret = TransportClient.builder().settings(settings).build();
        String[] addresses = config.getAddresses();
        if (addresses == null) {
            log.error("You need to provide an addressList to join a cluster");
        } else {
            for (String item : config.getAddresses()) {
                String[] address = item.split(":");
                log.debug("Add transport address: " + item);
                try {
                    InetAddress inet = InetAddress.getByName(address[0]);
                    ret.addTransportAddress(new InetSocketTransportAddress(inet, Integer.parseInt(address[1])));
                } catch (UnknownHostException e) {
                    log.error("Unable to resolve host " + address[0], e);
                }
            }
        }
        assert ret != null : "Unable to create a remote client";
        return ret;
    }

    private void checkClusterHealth(String... indexNames) {
        if (client == null) {
            throw new IllegalStateException("No es client available");
        }
        String errorMessage = null;
        try {
            log.debug("Waiting for cluster yellow health status, indexes: " + Arrays.toString(indexNames));
            ClusterHealthResponse ret = client.admin()
                                              .cluster()
                                              .prepareHealth(indexNames)
                                              .setTimeout(TIMEOUT_WAIT_FOR_CLUSTER)
                                              .setWaitForYellowStatus()
                                              .get();
            if (ret.isTimedOut()) {
                errorMessage = "ES Cluster health status not Yellow after " + TIMEOUT_WAIT_FOR_CLUSTER + " give up: "
                        + ret;
            } else {
                if ((indexNames.length > 0) && ret.getStatus() != ClusterHealthStatus.GREEN) {
                    log.warn("Es Cluster ready but not GREEN: " + ret);
                } else {
                    log.info("ES Cluster ready: " + ret);
                }
            }
        } catch (NoNodeAvailableException e) {
            errorMessage = "Failed to connect to elasticsearch, check addressList and clusterName: " + e.getMessage();
        }
        if (errorMessage != null) {
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    private void initializeIndexes() {
        for (ElasticSearchIndexConfig conf : indexConfig.values()) {
            if (conf.isDocumentIndex()) {
                log.info("Associate index " + conf.getName() + " with repository: " + conf.getRepositoryName());
                indexNames.put(conf.getRepositoryName(), conf.getName());
                repoNames.put(conf.getName(), conf.getRepositoryName());
                Set<String> set = new LinkedHashSet<>();
                if (includeSourceFields != null) {
                    set.addAll(Arrays.asList(includeSourceFields));
                }
                set.addAll(Arrays.asList(conf.getIncludes()));
                if (set.contains(ALL_FIELDS)) {
                    set.clear();
                    set.add(ALL_FIELDS);
                }
                includeSourceFields = set.toArray(new String[set.size()]);
                set.clear();
                if (excludeSourceFields != null) {
                    set.addAll(Arrays.asList(excludeSourceFields));
                }
                set.addAll(Arrays.asList(conf.getExcludes()));
                excludeSourceFields = set.toArray(new String[set.size()]);
            }

        }
        initIndexes(false);
    }

    // Admin Impl =============================================================
    @Override
    public void refreshRepositoryIndex(String repositoryName) {
        if (log.isDebugEnabled()) {
            log.debug("Refreshing index associated with repo: " + repositoryName);
        }
        getClient().admin().indices().prepareRefresh(getIndexNameForRepository(repositoryName)).execute().actionGet();
        if (log.isDebugEnabled()) {
            log.debug("Refreshing index done");
        }
    }

    @Override
    public String getIndexNameForRepository(String repositoryName) {
        String ret = indexNames.get(repositoryName);
        if (ret == null) {
            throw new NoSuchElementException("No index defined for repository: " + repositoryName);
        }
        return ret;
    }

    @Override
    public List<String> getIndexNamesForType(String type) {
        List<String> indexNames = new ArrayList<>();
        for (ElasticSearchIndexConfig conf : indexConfig.values()) {
            if (type.equals(conf.getType())) {
                indexNames.add(conf.getName());
            }
        }
        return indexNames;
    }

    @Override
    public String getIndexNameForType(String type) {
        List<String> indexNames = getIndexNamesForType(type);
        if (indexNames.isEmpty()) {
            throw new NoSuchElementException("No index defined for type: " + type);
        }
        return indexNames.get(0);
    }

    @Override
    public void flushRepositoryIndex(String repositoryName) {
        log.warn("Flushing index associated with repo: " + repositoryName);
        getClient().admin().indices().prepareFlush(getIndexNameForRepository(repositoryName)).execute().actionGet();
        log.info("Flushing index done");
    }

    @Override
    public void refresh() {
        for (String repositoryName : indexNames.keySet()) {
            refreshRepositoryIndex(repositoryName);
        }
    }

    @Override
    public void flush() {
        for (String repositoryName : indexNames.keySet()) {
            flushRepositoryIndex(repositoryName);
        }
    }

    @Override
    public void optimizeIndex(String indexName) {
        log.warn("Optimizing index: " + indexName);
        for (ElasticSearchIndexConfig conf : indexConfig.values()) {
            if (conf.getName().equals(indexName)) {
                getClient().admin().indices().prepareForceMerge(indexName).get();
            }
        }
        log.info("Optimize done");
    }

    @Override
    public void optimizeRepositoryIndex(String repositoryName) {
        optimizeIndex(getIndexNameForRepository(repositoryName));
    }

    @Override
    public void optimize() {
        for (ElasticSearchIndexConfig conf : indexConfig.values()) {
            optimizeIndex(conf.getName());
        }
    }

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public void initIndexes(boolean dropIfExists) {
        indexInitDone = false;
        for (ElasticSearchIndexConfig conf : indexConfig.values()) {
            initIndex(conf, dropIfExists);
        }
        log.info("ES Service ready");
        indexInitDone = true;
    }

    @Override
    public void dropAndInitIndex(String indexName) {
        log.info("Drop and init index: " + indexName);
        indexInitDone = false;
        for (ElasticSearchIndexConfig conf : indexConfig.values()) {
            if (conf.getName().equals(indexName)) {
                initIndex(conf, true);
            }
        }
        indexInitDone = true;
    }

    @Override
    public void dropAndInitRepositoryIndex(String repositoryName) {
        log.info("Drop and init index of repository: " + repositoryName);
        indexInitDone = false;
        for (ElasticSearchIndexConfig conf : indexConfig.values()) {
            if (conf.isDocumentIndex() && repositoryName.equals(conf.getRepositoryName())) {
                initIndex(conf, true);
            }
        }
        indexInitDone = true;
    }

    @Override
    public List<String> getRepositoryNames() {
        return Collections.unmodifiableList(new ArrayList<>(indexNames.keySet()));
    }

    void initIndex(ElasticSearchIndexConfig conf, boolean dropIfExists) {
        if (!conf.mustCreate()) {
            return;
        }
        log.info(String.format("Initialize index: %s, type: %s", conf.getName(), conf.getType()));
        boolean mappingExists = false;
        boolean indexExists = getClient().admin()
                                         .indices()
                                         .prepareExists(conf.getName())
                                         .execute()
                                         .actionGet()
                                         .isExists();
        if (indexExists) {
            if (!dropIfExists) {
                log.debug("Index " + conf.getName() + " already exists");
                mappingExists = getClient().admin()
                                           .indices()
                                           .prepareGetMappings(conf.getName())
                                           .execute()
                                           .actionGet()
                                           .getMappings()
                                           .get(conf.getName())
                                           .containsKey(conf.getType());
            } else {
                if (!Framework.isTestModeSet()) {
                    log.warn(String.format(
                            "Initializing index: %s, type: %s with " + "dropIfExists flag, deleting an existing index",
                            conf.getName(), conf.getType()));
                }
                getClient().admin().indices().delete(new DeleteIndexRequest(conf.getName())).actionGet();
                indexExists = false;
            }
        }
        if (!indexExists) {
            log.info(String.format("Creating index: %s", conf.getName()));
            if (log.isDebugEnabled()) {
                log.debug("Using settings: " + conf.getSettings());
            }
            getClient().admin()
                       .indices()
                       .prepareCreate(conf.getName())
                       .setSettings(conf.getSettings())
                       .execute()
                       .actionGet();
        }
        if (!mappingExists) {
            log.info(String.format("Creating mapping type: %s on index: %s", conf.getType(), conf.getName()));
            if (log.isDebugEnabled()) {
                log.debug("Using mapping: " + conf.getMapping());
            }
            getClient().admin()
                       .indices()
                       .preparePutMapping(conf.getName())
                       .setType(conf.getType())
                       .setSource(conf.getMapping())
                       .execute()
                       .actionGet();
            if (!dropIfExists && conf.getRepositoryName() != null) {
                repositoryInitialized.add(conf.getRepositoryName());
            }
        }
        // make sure the index is ready before returning
        checkClusterHealth(conf.getName());
    }

    @Override
    public long getPendingWorkerCount() {
        // impl of scheduling is left to the ESService
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public long getRunningWorkerCount() {
        // impl of scheduling is left to the ESService
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getTotalCommandProcessed() {
        return totalCommandProcessed.get();
    }

    @Override
    public boolean isEmbedded() {
        return embedded;
    }

    @Override
    public boolean useExternalVersion() {
        if (isEmbedded()) {
            return localConfig.useExternalVersion();
        }
        return remoteConfig.useExternalVersion();
    }

    @Override
    public boolean isIndexingInProgress() {
        // impl of scheduling is left to the ESService
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ListenableFuture<Boolean> prepareWaitForIndexing() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Get the elastic search indexes for searches
     */
    String[] getSearchIndexes(List<String> searchRepositories) {
        if (searchRepositories.isEmpty()) {
            Collection<String> values = indexNames.values();
            return values.toArray(new String[values.size()]);
        }
        String[] ret = new String[searchRepositories.size()];
        int i = 0;
        for (String repo : searchRepositories) {
            ret[i++] = getIndexNameForRepository(repo);
        }
        return ret;
    }

    public boolean isReady() {
        return indexInitDone;
    }

    String[] getIncludeSourceFields() {
        return includeSourceFields;
    }

    String[] getExcludeSourceFields() {
        return excludeSourceFields;
    }

    Map<String, String> getRepositoryMap() {
        return repoNames;
    }

    /**
     * Get the list of repository names that have their index created.
     */
    public List<String> getInitializedRepositories() {
        return repositoryInitialized;
    }
}
