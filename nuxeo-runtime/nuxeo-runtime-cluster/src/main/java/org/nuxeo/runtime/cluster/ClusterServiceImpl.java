/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.cluster;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.runtime.model.Descriptor.UNIQUE_DESCRIPTOR_ID;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation for the Cluster Service.
 *
 * @since 11.1
 */
public class ClusterServiceImpl extends DefaultComponent implements ClusterService {

    private static final Logger log = LogManager.getLogger(ClusterServiceImpl.class);

    /** Very early as other services depend on us. */
    public static final int APPLICATION_STARTED_ORDER = -1000;

    public static final String XP_CONFIG = "configuration";

    public static final String CLUSTERING_ENABLED_OLD_PROP = "repository.clustering.enabled";

    public static final String NODE_ID_OLD_PROP = "repository.clustering.id";

    protected static final Random RANDOM = new Random(); // NOSONAR (doesn't need cryptographic strength)

    protected boolean enabled;

    protected String nodeId;

    @Override
    public int getApplicationStartedOrder() {
        return APPLICATION_STARTED_ORDER;
    }

    @Override
    public void start(ComponentContext context) {
        ClusterNodeDescriptor descr = getDescriptor(XP_CONFIG, UNIQUE_DESCRIPTOR_ID);

        // enabled
        Boolean enabledProp = descr == null ? null : descr.getEnabled();
        if (enabledProp != null) {
            enabled = enabledProp.booleanValue();
        } else {
            // compat with old framework property
            enabled = Framework.isBooleanPropertyTrue(CLUSTERING_ENABLED_OLD_PROP);
        }

        // node id
        String id = descr == null ? null : defaultIfBlank(descr.getName(), null);
        if (id != null) {
            nodeId = id.trim();
        } else {
            // compat with old framework property
            id = Framework.getProperty(NODE_ID_OLD_PROP);
            if (isNotBlank(id)) {
                nodeId = id.trim();
            } else {
                // use a random node id
                long l;
                do {
                    l = RANDOM.nextLong();
                } while (l < 0); // keep a positive value to avoid weird node ids
                nodeId = String.valueOf(l);
                if (enabled) {
                    log.warn("Missing cluster node id configuration, please define it explicitly. "
                            + "Using random cluster node id instead: {}", nodeId);
                } else {
                    log.info("Using random cluster node id: {}", nodeId);
                }
            }
        }
        super.start(context);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    /** Allows tests to set the node id without a reload. */
    public void setNodeId(String nodeId) {
        if (!Framework.isTestModeSet()) {
            throw new UnsupportedOperationException("test mode only");
        }
        this.nodeId = nodeId;
    }

}
