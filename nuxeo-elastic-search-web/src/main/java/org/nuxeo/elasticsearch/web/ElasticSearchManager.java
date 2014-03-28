package org.nuxeo.elasticsearch.web;

import static org.jboss.seam.ScopeType.EVENT;

import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;

@Name("esAdmin")
@Scope(EVENT)
public class ElasticSearchManager {

    @In(create = true)
    protected ElasticSearchAdmin esa;

    public String getNodesInfo() {

        NodesInfoResponse nodesInfo = esa.getClient().admin().cluster().prepareNodesInfo().execute().actionGet();
        return nodesInfo.toString();

    }

}
