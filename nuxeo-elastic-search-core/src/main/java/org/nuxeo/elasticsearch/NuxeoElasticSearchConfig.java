package org.nuxeo.elasticsearch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;


@XObject(value = "elasticSearchConfig")
public class NuxeoElasticSearchConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@local")
    protected boolean local = true;

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

    @XNode("nodeName")
    protected String nodeName = "Nuxeo";

    @XNode("@enableHttp")
    protected boolean enableHttp = false;

    @XNodeList(value = "remoteNodes/node", type = ArrayList.class, componentType = String.class)
    protected List<String> remoteNodes;

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getLogPath() {
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

        if (indexStorageType!=null) {
            sb.append(" -Des.index.store.type=" + indexStorageType);
        }
        if (hostIp!=null) {
            sb.append(" -Des.network.host=" + hostIp);
        }
        if (dataPath!=null) {
            sb.append(" -Des.path.data=" + dataPath);
        }
        if (logPath!=null) {
            sb.append(" -Des.path.logs=" + dataPath);
        }
        if (clusterName!=null) {
            sb.append(" -Des.cluster.name="  + clusterName);
        }
        if (nodeName!=null) {
            sb.append(" -Des.node.name="  + nodeName);
        }

        return sb.toString();
    }

}
