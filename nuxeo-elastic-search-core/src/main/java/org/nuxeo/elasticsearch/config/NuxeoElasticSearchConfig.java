/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.config;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

/**
 * XMap descriptor used to configure Elastic Search integration
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
 */
@XObject(value = "elasticSearchConfig")
public class NuxeoElasticSearchConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@inProcess")
    protected boolean inProcess = false;

    @XNode("@autostartLocalNode")
    protected boolean autostartLocalNode = false;

    @XNode("clusterName")
    protected String clusterName;

    @XNode("startupScript")
    protected String startupScript;

    @XNode("dataPath")
    protected String dataPath;

    @XNode("logPath")
    protected String logPath;

    @XNode("indexStorageType")
    protected String indexStorageType = "memory";

    @XNode("hostIp")
    protected String hostIp;

    // Watch out the default tcp transport port is 9300 not 9200
    @XNode("hostPort")
    protected String hostPort = "9300";

    @XNode("nodeName")
    protected String nodeName = "Nuxeo";

    @XNode("@enableHttp")
    protected boolean enableHttp = false;

    @XNodeList(value = "remoteNodes/node", type = ArrayList.class, componentType = String.class)
    protected List<String> remoteNodes;

    public boolean isInProcess() {
        return inProcess;
    }

    public String getDataPath() {
        File home = Framework.getRuntime().getHome();
        File esDirectory = new File(home, "elasticsearch");
        if (!esDirectory.exists()) {
            esDirectory.mkdir();
        }
        dataPath = esDirectory.getPath() + "/data";
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getLogPath() {
        File home = Framework.getRuntime().getHome();
        File esDirectory = new File(home, "elasticsearch");
        if (!esDirectory.exists()) {
            esDirectory.mkdir();
        }
        logPath = esDirectory.getPath() + "/logs";
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getIndexStorageType() {
        return indexStorageType;
    }

    public void setIndexStorageType(String indexStorageType) {
        this.indexStorageType = indexStorageType;
    }

    public boolean enableHttp() {
        return enableHttp;
    }

    public void enableHttp(boolean enableHttp) {
        this.enableHttp = enableHttp;
    }

    public String getClusterName() {
        return clusterName;
    }

    public List<String> getRemoteNodes() {
        if (remoteNodes.size() == 0 && ! autostartLocalNode) {
            remoteNodes.add(getHostIp() + ":" + getHostPort());
        }
        return remoteNodes;
    }

    public boolean autostartLocalNode() {
        return autostartLocalNode;
    }

    public String getStartupScript() {
        return startupScript;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String asCommandLineArg() {
        StringBuffer sb = new StringBuffer();

        sb.append(getStartupScript());

        if (indexStorageType != null) {
            sb.append(" -Des.index.store.type=" + indexStorageType);
        }
        if (getHostIp() != null) {
            sb.append(" -Des.network.host=" + getHostIp());
        }

        if (getHostPort() != null) {
            sb.append(" -Des.http.port=" + getHostPort());
        }

        if (getDataPath() != null) {
            sb.append(" -Des.path.data=" + getDataPath());
        }

        if (getLogPath() != null) {
            sb.append(" -Des.path.logs=" + getLogPath());
        }
        if (clusterName != null) {
            sb.append(" -Des.cluster.name=" + clusterName);
        }
        if (nodeName != null) {
            sb.append(" -Des.node.name=" + nodeName);
        }

        return sb.toString();
    }

    public void setInProcess(boolean inProcess) {
        this.inProcess = inProcess;
    }

    public String getHostIp() {
        if (hostIp == null && autostartLocalNode) {
            hostIp = "127.0.0.1";
        }
        return hostIp;
    }

    public String getHostPort() {
        if (hostPort == null && autostartLocalNode) {
            hostPort = "9200";
        }
        return hostPort;
    }

}
