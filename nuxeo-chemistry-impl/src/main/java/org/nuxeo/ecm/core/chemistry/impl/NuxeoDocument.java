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

import org.apache.chemistry.CMISRuntimeException;
import org.apache.chemistry.ConstraintViolationException;
import org.apache.chemistry.ContentAlreadyExistsException;
import org.apache.chemistry.ContentStream;
import org.apache.chemistry.ContentStreamPresence;
import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;
import org.apache.chemistry.NameConstraintViolationException;
import org.apache.chemistry.StreamNotSupportedException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

public class NuxeoDocument extends NuxeoObject implements Document {

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

    // TODO put in API?
    public boolean hasContentStream() {
        if (getType().getContentStreamAllowed() == ContentStreamPresence.NOT_ALLOWED) {
            return false;
        }
        try {
            return NuxeoProperty.getContentStream(doc) != null;
        } catch (StreamNotSupportedException e) {
            return false;
        }
    }

    public ContentStream getContentStream() {
        return NuxeoProperty.getContentStream(doc);
    }

    public void setContentStream(ContentStream contentStream)
            throws IOException, CMISRuntimeException {
        ContentStreamPresence csa = getType().getContentStreamAllowed();
        if (csa == ContentStreamPresence.NOT_ALLOWED && contentStream != null) {
            throw new ConstraintViolationException("Content stream not allowed");
        } else if (csa == ContentStreamPresence.REQUIRED
                && contentStream == null) {
            throw new ConstraintViolationException("Content stream required");
        }
        try {
            NuxeoProperty.setContentStream(doc, contentStream, true);
        } catch (ContentAlreadyExistsException e) {
            // cannot happen, overwrite = true
        }
    }

    public Document copy(Folder folder) throws NameConstraintViolationException {
        if (folder == null) {
            throw new ConstraintViolationException("Unfiling not supported");
        }
        try {
            DocumentModel newdoc = connection.session.copy(doc.getRef(),
                    ((NuxeoFolder) folder).doc.getRef(), null);
            connection.session.save();
            return new NuxeoDocument(newdoc, connection);
        } catch (ClientException e) {
            throw new CMISRuntimeException(e.toString(), e);
        }
    }

}
