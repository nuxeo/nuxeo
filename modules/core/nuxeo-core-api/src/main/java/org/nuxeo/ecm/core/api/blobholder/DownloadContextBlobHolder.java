/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/** @since 2021.25 */
public class DownloadContextBlobHolder extends AbstractBlobHolder {

    protected final Blob blob;

    protected DocumentModel doc;

    protected Map<String, Serializable> extendedInfos;

    protected String filename;

    protected String reason;

    protected Boolean inline;

    public DownloadContextBlobHolder(Blob blob) {
        if (blob == null) {
            throw new IllegalArgumentException("Invalid null blob for download.");
        }
        this.blob = blob;
    }

    @Override
    protected String getBasePath() {
        return doc == null ? null : doc.getPathAsString();
    }

    @Override
    public Calendar getModificationDate() {
        return doc == null ? null : (Calendar) doc.getPropertyValue("dc:modified");
    }

    @Override
    public Serializable getProperty(String name) {
        return null;
    }

    @Override
    public Map<String, Serializable> getProperties() {
        return null;
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public void setDocument(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public Blob getBlob() {
        return blob;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Map<String, Serializable> getExtendedInfos() {
        return extendedInfos;
    }

    public void setExtendedInfos(Map<String, Serializable> extendedInfos) {
        this.extendedInfos = extendedInfos;
    }

    public Boolean isInline() {
        return inline;
    }

    public void setInline(Boolean inline) {
        this.inline = inline;
    }
}
