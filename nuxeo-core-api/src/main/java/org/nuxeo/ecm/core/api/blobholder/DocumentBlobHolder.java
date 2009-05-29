/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * {@link BlobHolder} implementation based on a {@link DocumentModel} and a
 * XPath.
 *
 * @author tiry
 */
public class DocumentBlobHolder extends AbstractBlobHolder {

    protected final DocumentModel doc;

    protected final String xPath;

    protected final String xPathFilename;

    public DocumentBlobHolder(DocumentModel doc, String xPath) {
        this.doc = doc;
        this.xPath = xPath;
        this.xPathFilename = null;
    }

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
                //  compatibility when filename was not stored on blob
                blob.setFilename((String) doc.getPropertyValue(xPathFilename));
            }
        }
        return blob;
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

    public Serializable getProperty(String name) throws ClientException {
        return doc.getPropertyValue(name);
    }

    public Map<String, Serializable> getProperties() {
        return doc.getPrefetch();
    }

}
