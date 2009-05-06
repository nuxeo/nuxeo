package org.nuxeo.ecm.webapp.filemanager;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.richfaces.model.UploadItem;

@Name("fileUploadHolder")
@Scope(ScopeType.PAGE)
@BypassInterceptors
/**
 *
 * Holds FileUpload data at page scope level
 * (Can not be part of FileManager that can not be Page Scoped because of Seam remoting)
 *
 */
public class UploadItemHolder implements Serializable {

    /**
     *
     */
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

}
