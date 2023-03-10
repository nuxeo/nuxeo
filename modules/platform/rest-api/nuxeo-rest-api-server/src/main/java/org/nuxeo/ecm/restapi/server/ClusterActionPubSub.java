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

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.pubsub.AbstractPubSubBroker;
import org.nuxeo.runtime.stream.StreamService;

/**
 * PubSub to propagate an action to all nodes in the cluster.
 *
 * @since 2021.25
 * @deprecated since 2023.0, use {@link org.nuxeo.runtime.pubsub.ClusterActionService} instead
 */
@Deprecated
public class ClusterActionPubSub extends AbstractPubSubBroker<ClusterAction> {
    private static final Logger log = LogManager.getLogger(ClusterActionPubSub.class);

    public static final String CLUSTER_ACTION_PUB_SUB_TOPIC = "clusterAction";

    public static final String START_CONSUMER_ACTION = "startConsumer";

    public static final String STOP_CONSUMER_ACTION = "stopConsumer";

    @Override
    public ClusterAction deserialize(InputStream in) throws IOException {
        return ClusterAction.deserialize(in);
    }

    public void sendClusterAction(String action, String param) {
        ClusterAction message = new ClusterAction(action, param);
        log.debug("Sending message: {}", message);
        sendMessage(message);
    }

    @Override
    public void receivedMessage(ClusterAction message) {
        log.debug("Receiving message: {}", message);
        if (START_CONSUMER_ACTION.equals(message.action)) {
            Framework.getService(StreamService.class).restartComputation(Name.ofUrn(message.param));
        } else if (STOP_CONSUMER_ACTION.equals(message.action)) {
            Framework.getService(StreamService.class).stopComputation(Name.ofUrn(message.param));
        }
    }
}
