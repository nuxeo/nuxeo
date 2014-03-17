/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.api.adapters;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;

public class PictureBookBlobHolder extends DocumentBlobHolder {

    private CoreSession session;

    public PictureBookBlobHolder(DocumentModel doc, String xPath) {
        super(doc, xPath);
    }

    @Override
    public Blob getBlob() throws ClientException {
        CoreSession session = getSession();
        boolean sessionOpened = false;
        if (session == null) {
            sessionOpened = true;
            session = CoreInstance.openCoreSession(doc.getRepositoryName());
        }
        try {
            DocumentModelList docs = session.getChildren(doc.getRef(),
                    "Picture");
            if (docs.isEmpty()) {
                return null;
            }
            DocumentModel documentModel = docs.get(0);
            if (documentModel == null) {
                return null;
            }
            PictureResourceAdapter picture = documentModel.getAdapter(PictureResourceAdapter.class);
            return picture.getPictureFromTitle("Original");
        } finally {
            if (sessionOpened) {
                session.close();
            }
        }

    }

    @Override
    public List<Blob> getBlobs() throws ClientException {
        return getBlobs("Original");
    }

    public List<Blob> getBlobs(String title) throws ClientException {
        CoreSession session = getSession();
        boolean sessionOpened = false;
        if (session == null) {
            sessionOpened = true;
            session = CoreInstance.openCoreSession(doc.getRepositoryName());
        }
        try {
            DocumentModelList docList = session.getChildren(doc.getRef(),
                    "Picture");
            List<Blob> blobList = new ArrayList<Blob>(docList.size());
            for (DocumentModel documentModel : docList) {
                PictureResourceAdapter picture = documentModel.getAdapter(PictureResourceAdapter.class);
                blobList.add(picture.getPictureFromTitle(title));
            }
            return blobList;
        } finally {
            if (sessionOpened) {
                session.close();
            }
        }
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

    protected CoreSession getSession() throws ClientException {
        if (session == null && doc != null) {
            session = doc.getCoreSession();
        }
        return session;
    }

}
