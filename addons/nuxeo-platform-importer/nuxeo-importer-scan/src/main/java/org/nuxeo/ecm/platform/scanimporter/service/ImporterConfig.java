/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.scanimporter.service;

import java.io.Serializable;

import org.nuxeo.common.utils.Path;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *
 * XMap Descriptor for importer config
 *
 * @author Thierry Delprat
 *
 */
@XObject("importerConfig")
public class ImporterConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("sourcePath")
    protected String sourcePath;

    @XNode("processedPath")
    protected String processedPath;

    @XNode("nbThreads")
    protected Integer nbThreads = 1;

    @XNode("batchSize")
    protected Integer batchSize = 10;

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
