/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.scanimporter.service;

import org.nuxeo.common.utils.Path;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap Descriptor for importer config
 *
 * @author Thierry Delprat
 */
@XObject("importerConfig")
public class ImporterConfig {

    @XNode("sourcePath")
    protected String sourcePath;

    @XNode("processedPath")
    protected String processedPath;

    @XNode("nbThreads")
    protected Integer nbThreads = 1;

    @XNode("batchSize")
    protected Integer batchSize = 10;

    @XNode("transactionTimeout")
    protected Integer transactionTimeout = 0;

    @XNode("targetPath")
    protected String targetPath = "/";

    @XNode("useXMLMapping")
    protected boolean useXMLMapping = true;

    @XNode("createInitialFolder")
    protected boolean createInitialFolder = true;

    @XNode("mergeInitialFolder")
    protected boolean mergeInitialFolder = false;

    @XNode("update")
    protected boolean update = true;

    public String getSourcePath() {
        if (sourcePath == null) {
            return null;
        }
        return new Path(sourcePath).removeTrailingSeparator().toString();
    }

    public String getProcessedPath() {
        return processedPath;
    }

    public Integer getNbThreads() {
        return nbThreads;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    /**
     * @since 5.9.4
     */
    public Integer getTransactionTimeout() {
        return transactionTimeout;
    }

    public String getTargetPath() {
        if (targetPath == null) {
            return null;
        }
        return new Path(targetPath).removeTrailingSeparator().toString();
    }

    public boolean useXMLMapping() {
        return useXMLMapping;
    }

    public boolean isCreateInitialFolder() {
        return createInitialFolder;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setCreateInitialFolder(boolean createInitialFolder) {
        this.createInitialFolder = createInitialFolder;
    }

    public boolean isMergeInitialFolder() {
        return mergeInitialFolder;
    }

    public void setMergeInitialFolder(boolean mergeInitialFolder) {
        this.mergeInitialFolder = mergeInitialFolder;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public void setProcessedPath(String processedPath) {
        this.processedPath = processedPath;
    }

    public void setNbThreads(Integer nbThreads) {
        this.nbThreads = nbThreads;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * @since 5.9.4
     */
    public void setTransactionTimeout(Integer transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }

    public void setTargetPath(String tagetPath) {
        this.targetPath = tagetPath;
    }

    public void setUseXMLMapping(boolean useXMLMapping) {
        this.useXMLMapping = useXMLMapping;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

}
