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

import java.io.IOException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 * {@link BlobHolder} implemention based on a {@link DocumentModel} and a Xpath
 * pointing to a String fields. (Typical use case is the Note DocType).
 *
 * @author tiry
 */
public class DocumentStringBlobHolder extends DocumentBlobHolder {

    protected String mt;

    public DocumentStringBlobHolder(DocumentModel doc, String path) {
        super(doc, path);
    }

    public DocumentStringBlobHolder(DocumentModel doc, String path,
            String mime_type) {
        super(doc, path);
        this.mt = mime_type;
    }

    @Override
    public Blob getBlob() throws ClientException {
        String string = (String) doc.getProperty(xPath).getValue();
        if (string == null) {
            return null;
        }
        Blob blob = new StringBlob(string, mt);
        String ext = ".txt";
        if ("text/html".equals(mt)) {
            ext = ".html";
        } else if ("text/xml".equals(mt)) {
            ext = ".xml";
        }
        String title = doc.getTitle();
        if (!title.endsWith(ext)) {
            title = title.concat(ext);
        }
        blob.setFilename(title);
        return blob;
    }

    @Override
    public void setBlob(Blob blob) throws ClientException {
        if (blob == null) {
            doc.getProperty(xPath).setValue(null);
            mt = null;
        } else {
            String string;
            try {
                string = blob.getString();
            } catch (IOException e) {
                throw new ClientException(e);
            }
            doc.getProperty(xPath).setValue(string);
            mt = blob.getMimeType();
        }
    }

}
