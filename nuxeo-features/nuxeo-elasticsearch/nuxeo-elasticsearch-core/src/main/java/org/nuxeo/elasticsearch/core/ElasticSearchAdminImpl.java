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

import java.io.IOException;
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
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ESClientFactory;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.config.ElasticSearchClientConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchEmbeddedServerConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchIndexConfig;
import org.nuxeo.runtime.api.Framework;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * @since 6.0
 */
public class ElasticSearchAdminImpl implements ElasticSearchAdmin {
    protected static final int TIMEOUT_WAIT_FOR_CLUSTER_SECOND = 30;

    protected static final int TIMEOUT_DELETE_SECOND = 300;

    private static final Log log = LogFactory.getLog(ElasticSearchAdminImpl.class);

    final AtomicInteger totalCommandProcessed = new AtomicInteger(0);

    private final Map<String, String> indexNames = new HashMap<>();

    private final Map<String, String> repoNames = new HashMap<>();

    private final Map<String, ElasticSearchIndexConfig> indexConfig;

    private final ElasticSearchEmbeddedServerConfig embeddedServerConfig;

    private final ElasticSearchClientConfig clientConfig;

    private ElasticSearchEmbeddedNode embeddedServer;

    private ESClient client;

    private boolean indexInitDone = false;

    private String[] includeSourceFields;

    private String[] excludeSourceFields;

    private List<String> repositoryInitialized = new ArrayList<>();

    /**
     * Init the admin service, remote configuration if not null will take precedence over local embedded configuration.
     * The transport client initialization can be customized.
     *
     * @since 9.1
     */
    public ElasticSearchAdminImpl(ElasticSearchEmbeddedServerConfig embeddedServerConfig,
            ElasticSearchClientConfig clientConfig, Map<String, ElasticSearchIndexConfig> indexConfig) {
        this.embeddedServerConfig = embeddedServerConfig;
        this.indexConfig = indexConfig;
        this.clientConfig = clientConfig;
        checkConfig();
        connect();
        initializeIndexes();
    }

    protected void checkConfig() {
        if (clientConfig == null) {
            throw new IllegalStateException("No Elasticsearch Client configuration provided, aborting");
        }
    }

    protected void connect() {
        if (client != null) {
            return;
        }
        if (embeddedServerConfig != null) {
            embeddedServer = new ElasticSearchEmbeddedNode(embeddedServerConfig);
            embeddedServer.start();
        }
        client = createClient(embeddedServer);
        checkClusterHealth();
        log.info("Elasticsearch Connected");
    }

    public void disconnect() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.error("Failed to close client: " + e.getMessage(), e);
            }
            client = null;
            indexInitDone = false;
            log.info("Elasticsearch Disconnected");
        }
        if (embeddedServer != null) {
            try {
                embeddedServer.close();
            } catch (IOException e) {
                log.error("Failed to close embedded node: " + e.getMessage(), e);
            }
            embeddedServer = null;
            log.info("Elasticsearch embedded Node Stopped");
        }
    }

    private ESClient createClient(ElasticSearchEmbeddedNode node) {
        log.info("Connecting to Elasticsearch");
        ESClient ret;
        try {
            ESClientFactory clientFactory = clientConfig.getKlass().newInstance();
            ret = clientFactory.create(node, clientConfig);
        } catch (ReflectiveOperationException e) {
            log.error("Cannot instantiate Elasticsearch Client from class: " + clientConfig.getKlass());
            throw new NuxeoException(e);
        }
        return ret;
    }

    private void checkClusterHealth(String... indexNames) {
        if (client == null) {
            throw new IllegalStateException("No Elasticsearch Client available");
        }
        client.waitForYellowStatus(indexNames, TIMEOUT_WAIT_FOR_CLUSTER_SECOND);
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
        getClient().refresh(getIndexNameForRepository(repositoryName));
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
        getClient().flush(getIndexNameForRepository(repositoryName));
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
                getClient().optimize(indexName);
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
    public ESClient getClient() {
        return client;
    }

    @Override
    public void initIndexes(boolean dropIfExists) {
        indexInitDone = false;
        for (ElasticSearchIndexConfig conf : indexConfig.values()) {
            initIndex(conf, dropIfExists);
        }
        log.info("Elasticsearch Service ready");
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
        boolean indexExists = getClient().indexExists(conf.getName());
        if (indexExists) {
            if (!dropIfExists) {
                log.debug("Index " + conf.getName() + " already exists");
                mappingExists = getClient().mappingExists(conf.getName(), conf.getType());
            } else {
                if (!Framework.isTestModeSet()) {
                    log.warn(String.format(
                            "Initializing index: %s, type: %s with " + "dropIfExists flag, deleting an existing index",
                            conf.getName(), conf.getType()));
                }
                getClient().deleteIndex(conf.getName(), TIMEOUT_DELETE_SECOND);
                indexExists = false;
            }
        }
        if (!indexExists) {
            log.info(String.format("Creating index: %s", conf.getName()));
            if (log.isDebugEnabled()) {
                log.debug("Using settings: " + conf.getSettings());
            }
            getClient().createIndex(conf.getName(), conf.getSettings());
        }
        if (!mappingExists) {
            log.info(String.format("Creating mapping type: %s on index: %s", conf.getType(), conf.getName()));
            if (log.isDebugEnabled()) {
                log.debug("Using mapping: " + conf.getMapping());
            }
            getClient().createMapping(conf.getName(), conf.getType(), conf.getMapping());
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
        return embeddedServer != null;
    }

    @Override
    public boolean useExternalVersion() {
        return clientConfig.useExternalVersion();
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
