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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 * {@link BlobHolder} implemention based on a {@link DocumentModel} and a Xpath pointing to a String fields.
 * (Typical use case is the Note DocType).
 *
 * @author tiry
 */
public class DocumentStringBlobHolder extends DocumentBlobHolder {

    protected String mt=null;

    public DocumentStringBlobHolder(DocumentModel doc, String path) {
        super(doc, path);
    }

    public DocumentStringBlobHolder(DocumentModel doc, String path, String mime_type) {
        super(doc, path);
        this.mt=mime_type;
    }

    @Override
    public Blob getBlob() throws ClientException {
        Blob blob =  new StringBlob((String) doc.getProperty(xPath).getValue(), mt);
        String ext = ".txt";
        if ("text/html".equals(mt)) {
            ext = ".html";
        }
        else if ("text/xml".equals(mt)) {
            ext = ".xml";
        }
        blob.setFilename(doc.getTitle()+ ext);
        return blob;
    }

}
