/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.core.api.blobholder;

import java.io.IOException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * {@link BlobHolder} implemention based on a {@link DocumentModel} and a Xpath pointing to a String fields. (Typical
 * use case is the Note DocType).
 *
 * @author tiry
 */
public class DocumentStringBlobHolder extends DocumentBlobHolder {

    protected String mt;

    public DocumentStringBlobHolder(DocumentModel doc, String path) {
        super(doc, path);
    }

    public DocumentStringBlobHolder(DocumentModel doc, String path, String mime_type) {
        super(doc, path);
        this.mt = mime_type;
    }

    @Override
    public Blob getBlob() {
        String string = (String) doc.getProperty(xPath).getValue();
        if (string == null) {
            return null;
        }
        Blob blob = Blobs.createBlob(string, mt);
        String ext = ".txt";
        if ("text/html".equals(mt)) {
            ext = ".html";
        } else if ("text/xml".equals(mt)) {
            ext = ".xml";
        } else if ("text/x-web-markdown".equals(mt)) {
            ext = ".md";
        }
        String title = doc.getTitle();
        if (!title.endsWith(ext)) {
            title = title.concat(ext);
        }
        blob.setFilename(title);
        return blob;
    }

    @Override
    public void setBlob(Blob blob) {
        if (blob == null) {
            doc.getProperty(xPath).setValue(null);
            mt = null;
        } else {
            String string;
            try {
                string = blob.getString();
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
            // strip '\0 chars from text
            if (string.indexOf('\0') >= 0) {
                string = string.replace("\0", "");
            }
            doc.getProperty(xPath).setValue(string);
            mt = blob.getMimeType();
        }
    }

}
