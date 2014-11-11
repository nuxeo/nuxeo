/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.utils.BlobsExtractor;

/**
 * {@link BlobHolder} implementation based on a {@link DocumentModel} and a
 * XPath.
 *
 * @author tiry
 */
public class
        DocumentBlobHolder extends AbstractBlobHolder {

    protected final DocumentModel doc;

    protected final String xPath;

    protected String xPathFilename;

    /**
     * Constructor with filename property for compatibility (when filename was
     * not stored on blob object)
     */
    public DocumentBlobHolder(DocumentModel doc, String xPath,
            String xPathFilename) {
        this.doc = doc;
        this.xPath = xPath;
        this.xPathFilename = xPathFilename;
    }

    public DocumentBlobHolder(DocumentModel doc, String xPath) {
        this(doc, xPath, null);
    }

    @Override
    protected String getBasePath() {
        return doc.getPathAsString();
    }

    @Override
    public Blob getBlob() throws ClientException {
        Blob blob = (Blob) doc.getPropertyValue(xPath);
        if (blob != null && xPathFilename != null) {
            String filename = blob.getFilename();
            if (filename == null || "".equals(filename)) {
                // compatibility when filename was not stored on blob
                blob.setFilename((String) doc.getPropertyValue(xPathFilename));
            }
        }
        return blob;
    }

    @Override
    public void setBlob(Blob blob) throws ClientException {
        doc.getProperty(xPath).setValue(blob);
        if (xPathFilename != null) {
            String filename = blob == null ? null : blob.getFilename();
            doc.setPropertyValue(xPathFilename, filename);
        }
    }

    @Override
    public Calendar getModificationDate() throws ClientException {
        return (Calendar) doc.getProperty("dublincore", "modified");
    }

    @Override
    public String getHash() throws ClientException {
        Blob blob = getBlob();
        if (blob != null) {
            String h = blob.getDigest();
            if (h != null) {
                return h;
            }
        }
        return doc.getId() + xPath + getModificationDate().toString();
    }

    @Override
    public Serializable getProperty(String name) throws ClientException {
        return null;
    }

    @Override
    public Map<String, Serializable> getProperties() {
        return null;
    }

    @Override
    public List<Blob> getBlobs() throws ClientException {
        return new BlobsExtractor().getBlobs(doc);
    }

}
