/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.restapi.jaxrs.io.capabilities;

import static org.nuxeo.common.Environment.DISTRIBUTION_HOTFIX;
import static org.nuxeo.common.Environment.DISTRIBUTION_NAME;
import static org.nuxeo.common.Environment.DISTRIBUTION_SERVER;
import static org.nuxeo.common.Environment.DISTRIBUTION_VERSION;

import java.util.Optional;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;

/**
 * @since 11.5
 */
public class ServerInfo {

    protected String distributionName;

    protected String distributionVersion;

    protected String distributionServer;

    protected String hotfixVersion;

    protected boolean clusterEnabled;

    protected String clusterNodeId;

    protected ServerInfo() {
    }

    public String getDistributionName() {
        return distributionName;
    }

    public String getDistributionVersion() {
        return distributionVersion;
    }

    public String getDistributionServer() {
        return distributionServer;
    }

    public Optional<String> getHotfixVersion() {
        return Optional.ofNullable(hotfixVersion);
    }

    public boolean isClusterEnabled() {
        return clusterEnabled;
    }

    public String getClusterNodeId() {
        return clusterNodeId;
    }

    public static ServerInfo get() {
        ClusterService clusterService = Framework.getService(ClusterService.class);

        ServerInfo info = new ServerInfo();
        info.distributionName = Framework.getProperty(DISTRIBUTION_NAME);
        info.distributionVersion = Framework.getProperty(DISTRIBUTION_VERSION);
        info.distributionServer = Framework.getProperty(DISTRIBUTION_SERVER);
        info.hotfixVersion = Framework.getProperty(DISTRIBUTION_HOTFIX);
        info.clusterEnabled = clusterService.isEnabled();
        info.clusterNodeId = clusterService.getNodeId();
        return info;
    }
}
