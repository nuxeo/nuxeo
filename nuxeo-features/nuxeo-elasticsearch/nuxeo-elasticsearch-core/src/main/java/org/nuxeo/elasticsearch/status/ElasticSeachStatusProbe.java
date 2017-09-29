/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.elasticsearch.status;

import java.util.List;

import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.nuxeo.ecm.core.management.api.Probe;
import org.nuxeo.ecm.core.management.api.ProbeStatus;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;

/**
 * Probe to check the status of the ES cluster. Returns success if the cluster is GREEN or YELLOW, Failure otherwise
 * 
 * @since 9.3
 */
public class ElasticSeachStatusProbe implements Probe {

    @Override
    public ProbeStatus run() {
        String[] indices = getIndexNames();
        ClusterHealthStatus clusterStatus = Framework.getService(ElasticSearchAdmin.class)
                                                     .getClient()
                                                     .getHealthStatus(indices);
        switch (clusterStatus) {
        case GREEN:
        case YELLOW:
            return ProbeStatus.newSuccess(clusterStatus.toString());
        default:
            return ProbeStatus.newFailure(clusterStatus.toString());
        }
    }

    protected String[] getIndexNames() {
        List<String> repositoryNames = Framework.getService(ElasticSearchAdmin.class).getRepositoryNames();
        String indices[] = new String[repositoryNames.size()];
        int i = 0;
        for (String repo : repositoryNames) {
            indices[i++] = Framework.getService(ElasticSearchAdmin.class).getIndexNameForRepository(repo);
        }
        return indices;
    }

}
