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
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
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

    @In(create = true)
    protected ElasticSearchAdmin esa;

    @In(create = true)
    protected ElasticSearchIndexing esi;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected List<ContentViewStatus> cvStatuses = null;

    protected Timer indexTimer;

    private String rootId;

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

    public String getNodesHealth() {
        ClusterHealthResponse health = esa.getClient().admin().cluster()
                .prepareHealth().execute().actionGet();
        return health.toString();
    }

    public void startReindex() throws Exception {
        log.warn("Start re-indexing repository");
        IndexingCommand cmd = new IndexingCommand(
                documentManager.getRootDocument(), false, true);
        esi.scheduleIndexing(cmd);
    }

    public void startReindexFrom() throws Exception {
        if (rootId == null) {
            startReindex();
            return;
        }
        log.warn(String.format("Start re-indexing from %s repository", rootId));
        IndexingCommand cmd = new IndexingCommand(
                documentManager.getDocument(new IdRef(rootId)), false, true);
        esi.scheduleIndexing(cmd);
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

    public String getPendingCommands() {
        return Integer.valueOf(esa.getPendingCommands()).toString();
    }

    public String getRunningCommands() {
        return Integer.valueOf(esa.getRunningCommands()).toString();
    }

    public String getTotalCommandProcessed() {
        return Integer.valueOf(esa.getTotalCommandProcessed()).toString();
    }

    public String getNumberOfDocuments() {
        NodesStatsResponse stats = esa.getClient().admin().cluster()
                .prepareNodesStats().execute().actionGet();
        return Long.valueOf(
                stats.getNodes()[0].getIndices().getDocs().getCount())
                .toString();
    }

    public String getNumberOfDeletedDocuments() {
        NodesStatsResponse stats = esa.getClient().admin().cluster()
                .prepareNodesStats().execute().actionGet();
        return Long.valueOf(
                stats.getNodes()[0].getIndices().getDocs().getDeleted())
                .toString();
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

    public String getRootId() {
        return rootId;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }
}
