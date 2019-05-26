/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.webapp.filemanager;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Holds FileUpload data at PAGE scope level, useful for {@link FileManageActions}.
 */
@Name("fileUploadHolder")
@Scope(ScopeType.PAGE)
public class UploadItemHolder implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Collection<NxUploadedFile> uploadedFiles = new ArrayList<>();

    protected InputStream fileUpload;

    protected File tempFile;

    protected String fileName;

    public File getTempFile() {
        return tempFile;
    }

    public void setTempFile(File tempFile) {
        this.tempFile = tempFile;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Collection<NxUploadedFile> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(Collection<NxUploadedFile> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public InputStream getFileUpload() {
        return fileUpload;
    }

    public void setFileUpload(InputStream fileUpload) {
        this.fileUpload = fileUpload;
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED, EventNames.DOCUMENT_CHANGED }, create = false)
    @BypassInterceptors
    public void reset() {
        uploadedFiles = new ArrayList<>();
        fileUpload = null;
        fileName = null;
        if (tempFile != null) {
            tempFile.delete();
        }
        tempFile = null;
    }

}
