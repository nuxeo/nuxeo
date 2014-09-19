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

import java.io.File;
import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

/**
 * XMap descriptor used to configure a local in JVM Elasticsearch instance
 *
 */
@XObject(value = "elasticSearchLocal")
public class ElasticSearchLocalConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@clusterName")
    protected String clusterName;

    @XNode("@nodeName")
    protected String nodeName = "Nuxeo";

    @XNode("@pathData")
    protected String dataPath;

    @XNode("@indexStoreType")
    protected String indexStoreType;

    @XNode("@httpEnabled")
    protected boolean httpEnabled = false;

    public String getDataPath() {
        if (dataPath == null) {
            File dir = new File(Framework.getRuntime().getHome(), "data/elasticsearch");
            dataPath = dir.getPath();
        }
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getIndexStorageType() {
        if (indexStoreType == null) {
            if (Framework.isTestModeSet()) {
                indexStoreType = "memory";
            } else {
                indexStoreType = "mmapfs";
            }
        }
        return indexStoreType;
    }

    public void setIndexStorageType(String indexStorageType) {
        this.indexStoreType = indexStorageType;
    }

    public boolean httpEnabled() {
        return httpEnabled;
    }

    public void setHttpEnabled(boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override public String toString() {
        return String.format("localConfig(%s, %s, %s, %s)", getClusterName(),
                getDataPath(), httpEnabled(), getIndexStorageType());

    }
}
