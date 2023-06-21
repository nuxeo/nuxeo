/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.runtime.pubsub;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.stream.StreamService;

/**
 * @since 2023.0
 */
public class ClusterActionServiceImpl extends DefaultComponent implements ClusterActionService {

    private static final Logger log = LogManager.getLogger(ClusterActionServiceImpl.class);

    public static final String STREAM_START_PROCESSOR_ACTION = "streamStartProcessor";

    public static final String STREAM_STOP_PROCESSOR_ACTION = "streamStopProcessor";

    public static final String STREAM_START_CONSUMER_ACTION = "streamStartConsumer";

    public static final String STREAM_STOP_CONSUMER_ACTION = "streamStopConsumer";

    protected final Map<String, Consumer<ClusterActionMessage>> clusterActions = new HashMap<>();

    protected ClusterActionPubSub clusterActionPubSub;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        new ComponentManager.Listener() {
            @Override
            public void beforeStart(ComponentManager mgr, boolean isResume) {
                clusterActions.clear();
            }

            @Override
            public void afterDeactivation(ComponentManager mgr) {
                uninstall();
            }
        }.install();
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        // service initialization
        ClusterService clusterService = Framework.getService(ClusterService.class);
        if (clusterService.isEnabled()) {
            String nodeId = clusterService.getNodeId();
            clusterActionPubSub = new ClusterActionPubSub();
            clusterActionPubSub.initialize(ClusterActionPubSub.CLUSTER_ACTION_PUB_SUB_TOPIC, nodeId);
        }
        // basic cluster action registration
        registerAction(STREAM_START_PROCESSOR_ACTION,
                message -> Framework.getService(StreamService.class).startProcessor(message.param));
        registerAction(STREAM_STOP_PROCESSOR_ACTION,
                message -> Framework.getService(StreamService.class).stopProcessor(message.param));
        registerAction(STREAM_START_CONSUMER_ACTION,
                message -> Framework.getService(StreamService.class).restartComputation(Name.ofUrn(message.param)));
        registerAction(STREAM_STOP_CONSUMER_ACTION,
                message -> Framework.getService(StreamService.class).stopComputation(Name.ofUrn(message.param)));
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        if (clusterActionPubSub != null) {
            clusterActionPubSub.close();
            clusterActionPubSub = null;
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.CLUSTER_ACTIONS;
    }

    @Override
    public void registerAction(String action, Consumer<ClusterActionMessage> consumer) {
        if (clusterActions.put(action, consumer) != null) {
            throw new IllegalArgumentException("A cluster action: " + action + " has already been registered");
        }
    }

    protected Consumer<ClusterActionMessage> getAction(String action) {
        return clusterActions.getOrDefault(action,
                msg -> log.warn("No cluster action registered for action: {}", action));
    }

    @Override
    public void executeAction(ClusterActionMessage message) {
        var clusterAction = getAction(message.action);
        clusterAction.accept(message);
        if (clusterActionPubSub != null) {
            clusterActionPubSub.sendMessage(message);
        }
    }

    /**
     * PubSub to propagate an action to all nodes in the cluster.
     *
     * @since 2023.0
     */
    public class ClusterActionPubSub extends AbstractPubSubBroker<ClusterActionMessage> {

        public static final String CLUSTER_ACTION_PUB_SUB_TOPIC = "clusterWideAction";

        @Override
        public ClusterActionMessage deserialize(InputStream in) throws IOException {
            return ClusterActionMessage.deserialize(in);
        }

        public void sendMessage(ClusterActionMessage message) {
            log.debug("Sending message: {}", message);
            super.sendMessage(message);
        }

        @Override
        public void receivedMessage(ClusterActionMessage message) {
            log.debug("Receiving message: {}", message);
            getAction(message.action).accept(message);
        }

    }
}
