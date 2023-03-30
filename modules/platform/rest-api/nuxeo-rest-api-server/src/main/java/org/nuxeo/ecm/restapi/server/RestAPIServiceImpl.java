/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.restapi.server;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 2021.25
 * @deprecated since 2023.0, use {@link org.nuxeo.runtime.pubsub.ClusterActionService} instead
 */
@Deprecated
public class RestAPIServiceImpl extends DefaultComponent implements RestAPIService {
    protected ClusterActionPubSub clusterStreamActionPubSub;

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        ClusterService clusterService = Framework.getService(ClusterService.class);
        if (clusterService.isEnabled()) {
            String nodeId = clusterService.getNodeId();
            clusterStreamActionPubSub = new ClusterActionPubSub();
            clusterStreamActionPubSub.initialize(ClusterActionPubSub.CLUSTER_ACTION_PUB_SUB_TOPIC, nodeId);
        }

    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        if (clusterStreamActionPubSub != null) {
            clusterStreamActionPubSub.close();
            clusterStreamActionPubSub = null;
        }
    }

    @Override
    public void propagateAction(String action, String param) {
        if (clusterStreamActionPubSub != null) {
            clusterStreamActionPubSub.sendClusterAction(action, param);
        }
    }
}
