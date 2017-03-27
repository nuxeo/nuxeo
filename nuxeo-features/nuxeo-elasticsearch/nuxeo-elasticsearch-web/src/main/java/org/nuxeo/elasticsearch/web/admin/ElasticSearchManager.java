/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.elasticsearch.web.admin;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@Name("esAdmin")
@Scope(CONVERSATION)
public class ElasticSearchManager implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ElasticSearchManager.class);

    private static final String DEFAULT_NXQL_QUERY = "SELECT * FROM Document";

    private static final String JSON_DELETE_CMD = "{\"id\":\"IndexingCommand-reindex\",\"type\":\"DELETE\",\"docId\":\"%s\",\"repo\":\"%s\",\"recurse\":true,\"sync\":true}";

    private static final String ES_CLUSTER_INFO_PROPERTY = "elasticsearch.adminCenter.displayClusterInfo";

    @In(create = true)
    protected ElasticSearchAdmin esa;

    @In(create = true)
    protected ElasticSearchIndexing esi;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected List<PageProviderStatus> ppStatuses = null;

    protected Timer indexTimer;

    protected Timer bulkIndexTimer;

    private String rootId;

    private String nxql = DEFAULT_NXQL_QUERY;

    private List<String> repositoryNames;

    private String repositoryName;

    private Boolean dropIndex = false;

    public String getNodesInfo() {
        NodesInfoResponse nodesInfo = esa.getClient().admin().cluster().prepareNodesInfo().execute().actionGet();
        return nodesInfo.toString();
    }

    public String getNodesStats() {
        NodesStatsResponse stats = esa.getClient().admin().cluster().prepareNodesStats().execute().actionGet();
        return stats.toString();
    }

    public String getNodesHealth() {
        String[] indices = getIndexNames();
        ClusterHealthResponse health = esa.getClient().admin().cluster().prepareHealth(indices).get();
        return health.toString();
    }

    public void startReindexAll() {
        String repositoryName = getRepositoryName();
        log.warn("Re-indexing the entire repository: " + repositoryName);
        esa.dropAndInitRepositoryIndex(repositoryName);
        esi.runReindexingWorker(repositoryName, "SELECT ecm:uuid FROM Document");
    }

    public void startReindexNxql() {
        String repositoryName = getRepositoryName();
        log.warn(String.format("Re-indexing from a NXQL query: %s on repository: %s", getNxql(), repositoryName));
        esi.runReindexingWorker(repositoryName, getNxql());
    }

    public void startReindexFrom() {
        String repositoryName = getRepositoryName();
        try (CoreSession session = CoreInstance.openCoreSessionSystem(repositoryName)) {
            log.warn(String.format("Try to remove %s and its children from %s repository index", rootId,
                    repositoryName));
            String jsonCmd = String.format(JSON_DELETE_CMD, rootId, repositoryName);
            IndexingCommand rmCmd = IndexingCommand.fromJSON(jsonCmd);
            esi.indexNonRecursive(rmCmd);

            DocumentRef ref = new IdRef(rootId);
            if (session.exists(ref)) {
                DocumentModel doc = session.getDocument(ref);
                log.warn(String.format("Re-indexing document: %s and its children on repository: %s", doc,
                        repositoryName));
                IndexingCommand cmd = new IndexingCommand(doc, Type.INSERT, false, true);
                esi.runIndexingWorker(Arrays.asList(cmd));
            }
        }
    }

    public void flush() {
        esa.flush();
    }

    public void optimize() {
        esa.optimize();
    }

    protected void introspectPageProviders() {

        ppStatuses = new ArrayList<>();

        PageProviderService pps = Framework.getLocalService(PageProviderService.class);
        for (String ppName : pps.getPageProviderDefinitionNames()) {
            PageProviderDefinition def = pps.getPageProviderDefinition(ppName);
            // Create an instance so class replacer is taken in account
            PageProvider<?> pp = pps.getPageProvider(ppName, def, null, null, 0L, 0L, null);
            String klass = pp.getClass().getCanonicalName();
            ppStatuses.add(new PageProviderStatus(ppName, klass));
        }
        Collections.sort(ppStatuses);
    }

    public List<PageProviderStatus> getContentViewStatus() {
        if (ppStatuses == null) {
            introspectPageProviders();
        }
        return ppStatuses;
    }

    public Boolean isIndexingInProgress() {
        return esa.isIndexingInProgress();
    }

    public Boolean displayClusterInfo() {
        if (esa.isEmbedded()) {
            return true;
        }
        return Boolean.parseBoolean(Framework.getProperty(ES_CLUSTER_INFO_PROPERTY, "false"));
    }

    public String getPendingWorkerCount() {
        return Long.valueOf(esa.getPendingWorkerCount()).toString();
    }

    public String getRunningWorkerCount() {
        return Long.valueOf(esa.getRunningWorkerCount()).toString();
    }

    public String getTotalCommandProcessed() {
        return Integer.valueOf(esa.getTotalCommandProcessed()).toString();
    }

    public String getNumberOfDocuments() {
        String[] indices = getIndexNames();
        CountResponse ret = esa.getClient().prepareCount(indices).setQuery(QueryBuilders.matchAllQuery()).get();
        return Long.valueOf(ret.getCount()).toString();
    }

    private String[] getIndexNames() {
        List<String> repositoryNames = getRepositoryNames();
        String indices[] = new String[repositoryNames.size()];
        int i = 0;
        for (String repo : repositoryNames) {
            indices[i++] = esa.getIndexNameForRepository(repo);
        }
        return indices;
    }

    public String getIndexingRates() {
        if (indexTimer == null) {
            MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
            indexTimer = registry.timer(MetricRegistry.name("nuxeo", "elasticsearch", "service", "index"));

        }
        return String.format("%.2f, %.2f, %.2f", indexTimer.getOneMinuteRate(), indexTimer.getFiveMinuteRate(),
                indexTimer.getFifteenMinuteRate());
    }

    public String getBulkIndexingRates() {
        if (bulkIndexTimer == null) {
            MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
            bulkIndexTimer = registry.timer(MetricRegistry.name("nuxeo", "elasticsearch", "service", "bulkIndex"));

        }
        return String.format("%.2f, %.2f, %.2f", bulkIndexTimer.getOneMinuteRate(), bulkIndexTimer.getFiveMinuteRate(),
                bulkIndexTimer.getFifteenMinuteRate());
    }

    public String getRootId() {
        return rootId;
    }

    public List<String> getRepositoryNames() {
        if (repositoryNames == null) {
            repositoryNames = esa.getRepositoryNames();
        }
        return repositoryNames;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }

    public String getNxql() {
        return nxql;
    }

    public void setNxql(String nxql) {
        this.nxql = nxql;
    }

    public String getRepositoryName() {
        if (repositoryName == null) {
            List<String> repositoryNames = getRepositoryNames();
            if (!repositoryNames.isEmpty()) {
                repositoryName = repositoryNames.get(0);
            }
        }
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public Boolean getDropIndex() {
        return dropIndex;
    }

    public void setDropIndex(Boolean dropIndex) {
        this.dropIndex = dropIndex;
    }
}
