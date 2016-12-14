/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
