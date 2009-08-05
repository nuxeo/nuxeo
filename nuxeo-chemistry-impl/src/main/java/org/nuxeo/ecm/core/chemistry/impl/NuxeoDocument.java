/*
 * Copyright 2009 Nuxeo SA <http://nuxeo.com>
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
 * Authors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.impl;

import java.io.IOException;
import java.util.Collection;

import org.apache.chemistry.ContentStream;
import org.apache.chemistry.ContentStreamPresence;
import org.apache.chemistry.Document;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;

public class NuxeoDocument extends NuxeoObject implements Document {

    private static final Log log = LogFactory.getLog(NuxeoDocument.class);

    public NuxeoDocument(DocumentModel doc, NuxeoConnection connection) {
        super(doc, connection);
    }

    public Document checkOut() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void cancelCheckOut() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Document checkIn(boolean major, String comment) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Document getLatestVersion(boolean major) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public Collection<Document> getAllVersions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    public void deleteAllVersions() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    protected Blob getBlob() {
        if (!doc.hasSchema("file")) {
            return null;
        }
        try {
            return (Blob) doc.getProperty("file", "content");
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e); // TODO
        }
    }

    // TODO put in API?
    public boolean hasContentStream() {
        if (getType().getContentStreamAllowed() == ContentStreamPresence.NOT_ALLOWED) {
            return false;
        }
        if (!doc.hasSchema("file")) {
            return false;
        }
        try {
            return (Blob) doc.getProperty("file", "content") != null;
        } catch (ClientException e) {
            log.error("Could not check blob presence", e);
            return false;
        }
    }

    public ContentStream getContentStream() {
        Blob blob = getBlob();
        if (blob == null) {
            return null;
        }
        return new NuxeoContentStream(blob);
    }

    public void setContentStream(ContentStream contentStream)
            throws IOException {
        ContentStreamPresence csa = getType().getContentStreamAllowed();
        if (csa == ContentStreamPresence.NOT_ALLOWED && contentStream != null) {
            throw new RuntimeException("Content stream not allowed"); // TODO
        } else if (csa == ContentStreamPresence.REQUIRED
                && contentStream == null) {
            throw new RuntimeException("Content stream required"); // TODO
        }
        Blob blob;
        if (contentStream == null) {
            blob = null;
        } else {
            blob = new InputStreamBlob(contentStream.getStream(),
                    contentStream.getMimeType(), null,
                    contentStream.getFileName(), null);
        }
        try {
            doc.setProperty("file", "content", blob);
        } catch (ClientException e) {
            throw new RuntimeException(e.toString(), e); // TODO
        }
    }

}
