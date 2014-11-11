/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.richfaces.model.UploadItem;

/**
 * Holds FileUpload data at CONVERSATION scope level (Can not be part of
 * FileManager that can not be CONVERSATION Scoped because of Seam remoting)
 */
@Name("fileUploadHolder")
@Scope(ScopeType.CONVERSATION)
public class UploadItemHolder implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Collection<UploadItem> uploadedFiles = new ArrayList<UploadItem>();

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

    public Collection<UploadItem> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(Collection<UploadItem> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public InputStream getFileUpload() {
        return fileUpload;
    }

    public void setFileUpload(InputStream fileUpload) {
        this.fileUpload = fileUpload;
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.DOCUMENT_CHANGED }, create = false)
    @BypassInterceptors
    public void reset() {
        uploadedFiles = new ArrayList<UploadItem>();
        fileUpload = null;
        fileName = null;
        if (tempFile != null) {
            tempFile.delete();
        }
        tempFile = null;
    }

}
