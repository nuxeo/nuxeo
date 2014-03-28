package org.nuxeo.elasticsearch.web;

import static org.jboss.seam.ScopeType.EVENT;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsRequest;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.cluster.repositories.get.GetRepositoriesResponse;
import org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksResponse;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.commands.IndexingCommand;

@Name("esAdmin")
@Scope(EVENT)
public class ElasticSearchManager {

    @In(create = true)
    protected ElasticSearchAdmin esa;

    @In(create = true)
    protected ElasticSearchIndexing esi;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    public String getNodesInfo() {
        NodesInfoResponse nodesInfo = esa.getClient().admin().cluster().prepareNodesInfo().execute().actionGet();
        return nodesInfo.toString();
    }

    public String getNodesStats() {
        NodesStatsResponse stats = esa.getClient().admin().cluster().prepareNodesStats().execute().actionGet();
        return stats.toString();
    }

    public String getNodesTasks() {
        PendingClusterTasksResponse tasks = esa.getClient().admin().cluster().preparePendingClusterTasks().execute().actionGet();
        return tasks.pendingTasks().toString();
    }

    public String getNodesHealth() {
        ClusterHealthResponse health = esa.getClient().admin().cluster().prepareHealth().execute().actionGet();
        return health.toString();
    }


    public void startReindex() throws Exception {
       IndexingCommand cmd = new IndexingCommand(documentManager.getRootDocument(), false, true);
       esi.scheduleIndexing(cmd);
    }

    public void listIndexes() {

    }
}
