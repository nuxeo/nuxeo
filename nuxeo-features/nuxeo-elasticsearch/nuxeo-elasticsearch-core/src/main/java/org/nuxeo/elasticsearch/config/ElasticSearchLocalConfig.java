/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.File;
import java.io.Serializable;

import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

/**
 * XMap descriptor used to configure a local in JVM Elasticsearch instance
 */
@XObject(value = "elasticSearchLocal")
public class ElasticSearchLocalConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@enabled")
    protected boolean isEnabled = true;

    @XNode("@clusterName")
    protected String clusterName;

    @XNode("@nodeName")
    protected String nodeName = "Nuxeo";

    // @since 8.4
    @XNode("@pathHome")
    private String homePath;

    @XNode("@pathData")
    protected String dataPath;

    @XNode("@indexStoreType")
    protected String indexStoreType;

    @XNode("@httpEnabled")
    protected boolean httpEnabled = false;

    @XNode("@httpPort")
    protected String httpPort = "9200";

    @XNode("@networkHost")
    protected String networkHost = "127.0.0.1";

    // @since 8.3
    @XNode("@useExternalVersion")
    protected boolean externalVersion = true;

    public String getClusterName() {
        return clusterName;
    }

    /**
     * @since 8.4
     */
    public String getHomePath() {
        if (homePath == null) {
            // Since ES 2.X we need to set a home path for embedded node, but it is not used by the bundle
            File dir = new File(Environment.getDefault().getTemp(), "elasticsearch");
            homePath = dir.getPath();
        }
        return homePath;
    }

    public String getDataPath() {
        if (dataPath == null) {
            File dir = new File(Framework.getRuntime().getHome(), "data/elasticsearch");
            dataPath = dir.getPath();
        }
        return dataPath;
    }

    public String getIndexStorageType() {
        if (indexStoreType == null) {
            indexStoreType = "mmapfs";
        }
        return indexStoreType;
    }

    /**
     * @since 7.4
     */
    public String getNetworkHost() {
        return networkHost;
    }

    public String getNodeName() {
        return nodeName;
    }

    public boolean httpEnabled() {
        return httpEnabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    /**
     * @since 8.4
     */
    public void setHomePath(String homePath) {
        this.homePath = homePath;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setHttpEnabled(boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
    }

    public void setIndexStorageType(String indexStorageType) {
        this.indexStoreType = indexStorageType;
    }

    /**
     * @since 7.4
     */
    public void setNetworkHost(String networkHost) {
        this.networkHost = networkHost;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public boolean useExternalVersion() {
        return externalVersion;
    }

    @Override
    public String toString() {
        if (isEnabled()) {
            return String.format("EsLocalConfig(%s, %s, %s, %s)", getClusterName(), getDataPath(), httpEnabled(),
                    getIndexStorageType());
        }
        return "EsLocalConfig disabled";
    }

    public String getHttpPort() {
        return httpPort;
    }
}
