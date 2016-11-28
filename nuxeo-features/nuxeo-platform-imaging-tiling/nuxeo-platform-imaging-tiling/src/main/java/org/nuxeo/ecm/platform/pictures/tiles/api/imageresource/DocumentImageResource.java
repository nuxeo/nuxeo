/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.pictures.tiles.api.imageresource;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;

/**
 * DocumentModel-based implementation of ImageResource. Supports clean digest and modification date to have a clean
 * invalidation system.
 *
 * @author tiry
 */
public class DocumentImageResource implements ImageResource {

    private static final long serialVersionUID = 1L;

    protected Blob blob;

    protected String hash;

    protected Calendar modified;

    protected DocumentModel doc;

    protected String xPath;

    /**
     * @deprecated since 9.1 as filename is now hold by the blob and no longer exist beside it
     */
    @Deprecated
    protected String fileName;

    public DocumentImageResource(DocumentModel doc, String xPath) {
        this.doc = doc;
        this.xPath = xPath;
    }

    protected String getEscapedxPath(String xPath) {
        String clean = xPath.replace(":", "_");
        clean = clean.replace("/", "_");
        clean = clean.replace("[", "");
        clean = clean.replace("]", "");
        return clean;
    }

    protected void compute() throws PropertyException {

        blob = (Blob) doc.getProperty(xPath).getValue();
        modified = (Calendar) doc.getProperty("dublincore", "modified");

        hash = blob.getDigest();
        if (hash == null) {
            hash = doc.getRepositoryName() + "_" + doc.getId() + "_" + getEscapedxPath(xPath);
            if (modified != null) {
                hash = hash + "_" + modified.getTimeInMillis();
            }
        }
    }

    public Blob getBlob() {
        if (blob == null) {
            compute();
        }
        if (fileName != null) {
            blob.setFilename(fileName);
        }
        return blob;
    }

    public String getHash() {
        if (hash == null) {
            compute();
        }
        return hash;
    }

    public Calendar getModificationDate() {
        if (modified == null) {
            compute();
        }
        return modified;
    }

    /**
     * @deprecated since 9.1 as filename is now hold by the blob and no longer exist beside it
     */
    @Deprecated
    public void setFileName(String name) {
        this.fileName = name;
    }

}
