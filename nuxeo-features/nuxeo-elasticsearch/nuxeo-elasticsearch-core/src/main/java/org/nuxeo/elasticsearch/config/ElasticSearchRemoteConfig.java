/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.elasticsearch.config;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor to configure a remote Elasticsearch connection
 */
@XObject(value = "elasticSearchRemote")
public class ElasticSearchRemoteConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@enabled")
    protected boolean isEnabled = true;

    @XNode("@clusterName")
    protected String clusterName;

    @XNode("@addressList")
    protected String addressList;

    @XNode("@clientTransportSniff")
    protected boolean clientTransportSniff = false;

    @XNode("@clientTransportIgnoreClusterName")
    protected boolean clientTransportIgnoreClusterName = false;

    @XNode("@clientTransportPingTimeout")
    protected String clientTransportPingTimeout = "5s";

    @XNode("@clientTransportNodesSamplerInterval")
    protected String clientTransportNodesSamplerInterval = "5s";

    // @since 8.3
    @XNode("@useExternalVersion")
    protected boolean externalVersion = true;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String[] getAddresses() {
        if (addressList != null) {
            return addressList.split(",");
        }
        return null;
    }

    public boolean isIgnoreClusterName() {
        return clientTransportIgnoreClusterName;
    }

    public boolean isClusterSniff() {
        return clientTransportSniff;
    }

    public String getPingTimeout() {
        return clientTransportPingTimeout;
    }

    public String getSamplerInterval() {
        return clientTransportNodesSamplerInterval;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean useExternalVersion() {
        return externalVersion;
    }

    @Override
    public String toString() {
        if (isEnabled()) {
            return String.format("EsRemoteConfig(%s, [%s])", getClusterName(), addressList);
        }
        return "EsRemoteConfig disabled";
    }
}
