/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */
package org.nuxeo.elasticsearch.web.admin;

import static org.jboss.seam.ScopeType.EVENT;

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
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

import javax.faces.model.SelectItem;

/**
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@Name("esAdmin")
@Scope(EVENT)
public class ElasticSearchManager {

    private static final Log log = LogFactory
            .getLog(ElasticSearchManager.class);

    private static final String DEFAULT_NXQL_QUERY = "SELECT * FROM Document";
    private static final String JSON_DELETE_CMD = "{\"id\":\"IndexingCommand-reindex\",\"type\":\"DELETE\",\"docId\":\"%s\",\"repo\":\"%s\",\"recurse\":true,\"sync\":true}";

    private static final String ES_CLUSTER_INFO_PROPERTY = "elasticsearch.adminCenter.displayClusterInfo";

    @In(create = true)
    protected ElasticSearchAdmin esa;

    @In(create = true)
    protected ElasticSearchIndexing esi;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected List<ContentViewStatus> cvStatuses = null;

    protected Timer indexTimer;

    protected Timer bulkIndexTimer;

    private String rootId;

    private String nxql = DEFAULT_NXQL_QUERY;

    private String repositoryName;

    private Boolean dropIndex = false;

    public String getNodesInfo() {
        NodesInfoResponse nodesInfo = esa.getClient().admin().cluster()
                .prepareNodesInfo().execute().actionGet();
        return nodesInfo.toString();
    }

    public String getNodesStats() {
        NodesStatsResponse stats = esa.getClient().admin().cluster()
                .prepareNodesStats().execute().actionGet();
        return stats.toString();
    }

    public String getNodesHealth() throws Exception {
        String[] indices = getIndexNames();
        ClusterHealthResponse health = esa.getClient().admin().cluster().prepareHealth(indices).get();
        return health.toString();
    }

   public void startReindexAll() throws Exception {
        log.warn("Re-indexing the entire repository: " + repositoryName);
        esa.dropAndInitRepositoryIndex(repositoryName);
        esi.runReindexingWorker(repositoryName, "SELECT ecm:uuid FROM Document");
    }

    public void startReindexNxql() throws Exception {
        log.warn(String.format("Re-indexing from a NXQL query: %s on repository: %s",
                getNxql(), repositoryName));
        esi.runReindexingWorker(repositoryName, getNxql());
    }

    public void startReindexFrom() throws Exception {
        RepositoryManager rm = Framework
                .getLocalService(RepositoryManager.class);
        CoreSession session = rm.getRepository(repositoryName).open();
        try {
            log.warn(String.format(
                    "Try to remove %s and its children from %s repository index", rootId,
                    repositoryName));
            String jsonCmd = String.format(JSON_DELETE_CMD, rootId, repositoryName);
            IndexingCommand rmCmd = IndexingCommand.fromJSON(jsonCmd);
            esi.runIndexingWorker(Arrays.asList(rmCmd));

            DocumentRef ref = new IdRef(rootId);
            if (session.exists(ref)) {
                DocumentModel doc = session.getDocument(ref);
                log.warn(String.format(
                        "Re-indexing document: %s and its children on repository: %s",
                        doc, repositoryName));
                IndexingCommand cmd = new IndexingCommand(doc, IndexingCommand.Type.UPDATE, false, true);
                esi.runIndexingWorker(Arrays.asList(cmd));
            }
        } finally {
            CoreInstance.getInstance().close(session);
        }
    }

    public void flush() {
        esa.flush();
    }

    protected void introspectPageProviders() throws Exception {

        cvStatuses = new ArrayList<>();

        ContentViewService cvs = Framework
                .getLocalService(ContentViewService.class);

        for (String cvName : cvs.getContentViewNames()) {
            ContentView cv = cvs.getContentView(cvName);
            PageProviderDefinition def = cv.getPageProvider().getDefinition();
            if (def instanceof GenericPageProviderDescriptor) {
                GenericPageProviderDescriptor gppd = (GenericPageProviderDescriptor) def;
                if (gppd.getPageProviderClass().getName()
                        .contains("elasticsearch")) {
                    cvStatuses.add(new ContentViewStatus(cvName,
                            gppd.getName(), "elasticsearch"));
                } else {
                    cvStatuses.add(new ContentViewStatus(cvName,
                            gppd.getName(), gppd.getPageProviderClass()
                                    .getName()));
                }
            } else if (def instanceof CoreQueryPageProviderDescriptor) {
                cvStatuses.add(new ContentViewStatus(cvName, def.getName(),
                        "core"));
            }
        }

        Collections.sort(cvStatuses);
    }

    public List<ContentViewStatus> getContentViewStatus() throws Exception {
        if (cvStatuses == null) {
            introspectPageProviders();
        }
        return cvStatuses;
    }

    public Boolean isIndexingInProgress() {
        return esa.isIndexingInProgress();
    }

    public Boolean displayClusterInfo() {
        return Boolean.parseBoolean(Framework.getProperty(ES_CLUSTER_INFO_PROPERTY, "false"));
    }

    public String getPendingWorkerCount() {
        return Integer.valueOf(esa.getPendingWorkerCount()).toString();
    }

    public String getRunningCommands() {
        return Integer.valueOf(esa.getRunningWorkerCount()).toString();
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
        String indices[]  = new String[esa.getRepositoryNames().size()];
        int i=0;
        for (String repo : esa.getRepositoryNames()) {
            indices[i++] = esa.getIndexNameForRepository(repo);
        }
        return indices;
    }

    public String getIndexingRates() {
        if (indexTimer == null) {
            MetricRegistry registry = SharedMetricRegistries
                    .getOrCreate(MetricsService.class.getName());
            indexTimer = registry.timer(MetricRegistry.name("nuxeo",
                    "elasticsearch", "service", "index"));

        }
        return String.format("%.2f, %.2f, %.2f", indexTimer.getOneMinuteRate(),
                indexTimer.getFiveMinuteRate(),
                indexTimer.getFifteenMinuteRate());
    }

    public String getBulkIndexingRates() {
        if (bulkIndexTimer == null) {
            MetricRegistry registry = SharedMetricRegistries
                    .getOrCreate(MetricsService.class.getName());
            bulkIndexTimer = registry.timer(MetricRegistry.name("nuxeo",
                    "elasticsearch", "service", "bulkIndex"));

        }
        return String.format("%.2f, %.2f, %.2f",
                bulkIndexTimer.getOneMinuteRate(),
                bulkIndexTimer.getFiveMinuteRate(),
                bulkIndexTimer.getFifteenMinuteRate());
    }

    public String getRootId() {
        return rootId;
    }

    public List<SelectItem> getRepositoryNames() {
        List<SelectItem> ret = new ArrayList<>();
        for (String repoName : esa.getRepositoryNames()) {
            ret.add(new SelectItem(repoName));
        }
        return ret;
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
