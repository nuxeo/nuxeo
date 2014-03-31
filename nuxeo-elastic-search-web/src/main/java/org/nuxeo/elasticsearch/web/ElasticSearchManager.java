package org.nuxeo.elasticsearch.web;

import static org.jboss.seam.ScopeType.EVENT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.cluster.node.stats.NodesStatsResponse;
import org.elasticsearch.action.admin.cluster.tasks.PendingClusterTasksResponse;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.commands.IndexingCommand;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewDescriptor;

@Name("esAdmin")
@Scope(EVENT)
public class ElasticSearchManager {

    @In(create = true)
    protected ElasticSearchAdmin esa;

    @In(create = true)
    protected ElasticSearchIndexing esi;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected List<PageProviderDefinition> elasticSearchPageProviders = null;

    protected List<PageProviderDefinition> corePageProviders = null;

    protected List<PageProviderDefinition> otherProviders = null;

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
        IndexingCommand cmd = new IndexingCommand(
                documentManager.getRootDocument(), false, true);
        esi.scheduleIndexing(cmd);
    }

    public void flush() throws Exception {
        esi.flush(true);

    }

    protected void introspectPageProviders() throws Exception {

        elasticSearchPageProviders = new ArrayList<>();
        corePageProviders = new ArrayList<>();
        otherProviders = new ArrayList<>();

        Collection<RegistrationInfo> infos = Framework.getRuntime().getComponentManager().getRegistrations();
        for (RegistrationInfo info : infos) {
            if (info.getExtensions()==null) {
                continue;
            }
            for (Extension extension : info.getExtensions()) {
                if (extension.getExtensionPoint().equals("providers")) {
                    if (extension.getContributions()==null) {
                        continue;
                    }
                    for (Object contrib : extension.getContributions()) {
                        if (contrib instanceof GenericPageProviderDescriptor) {
                            GenericPageProviderDescriptor gppd = (GenericPageProviderDescriptor) contrib;
                            if (gppd.getPageProviderClass().getName().contains(
                                    "elasticsearch")) {
                                elasticSearchPageProviders.add(gppd);
                            } else {
                                otherProviders.add(gppd);
                            }
                        } else if (contrib instanceof CoreQueryPageProviderDescriptor) {
                            corePageProviders.add((CoreQueryPageProviderDescriptor) contrib);
                        }
                    }
                }
                else if (extension.getExtensionPoint().equals("contentViews")) {
                    if (extension.getContributions()==null) {
                        continue;
                    }
                    for (Object contrib : extension.getContributions()) {
                        if (contrib instanceof ContentViewDescriptor) {
                            ContentViewDescriptor cv = (ContentViewDescriptor) contrib;
                            if (cv.getCoreQueryPageProvider()!=null) {
                                corePageProviders.add(cv.getCoreQueryPageProvider());
                            } else {
                                GenericPageProviderDescriptor gppd = cv.getGenericPageProvider();
                                if (gppd==null) {
                                    continue;
                                }
                                if (gppd.getPageProviderClass().getName().contains(
                                        "elasticsearch")) {
                                    elasticSearchPageProviders.add(gppd);
                                } else {
                                    otherProviders.add(gppd);
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    public List<PageProviderDefinition> getElasticSearchPageProviders()
            throws Exception {
        if (elasticSearchPageProviders == null) {
            introspectPageProviders();
        }
        return elasticSearchPageProviders;
    }

    public List<PageProviderDefinition> getCorePageProviders() throws Exception {
        if (corePageProviders == null) {
            introspectPageProviders();
        }
        return corePageProviders;
    }

    public List<PageProviderDefinition> getOtherProviders() throws Exception {
        if (otherProviders == null) {
            introspectPageProviders();
        }
        return otherProviders;
    }

}
