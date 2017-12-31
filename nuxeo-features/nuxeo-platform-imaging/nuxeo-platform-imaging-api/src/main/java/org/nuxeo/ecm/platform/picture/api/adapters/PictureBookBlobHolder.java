/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.api.adapters;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;

public class PictureBookBlobHolder extends DocumentBlobHolder {

    private CoreSession session;

    public PictureBookBlobHolder(DocumentModel doc, String xPath) {
        super(doc, xPath);
    }

    @Override
    public Blob getBlob() {
        CoreSession session = getSession();
        boolean sessionOpened = false;
        if (session == null) {
            sessionOpened = true;
            session = CoreInstance.openCoreSession(doc.getRepositoryName());
        }
        try {
            DocumentModelList docs = session.getChildren(doc.getRef(), "Picture");
            if (docs.isEmpty()) {
                return null;
            }
            DocumentModel documentModel = docs.get(0);
            if (documentModel == null) {
                return null;
            }
            BlobHolder bh = documentModel.getAdapter(BlobHolder.class);
            return bh.getBlob();
        } finally {
            if (sessionOpened) {
                ((CloseableCoreSession) session).close();
            }
        }

    }

    @Override
    public List<Blob> getBlobs() {
        return getBlobs("Original");
    }

    public List<Blob> getBlobs(String title) {
        CoreSession session = getSession();
        boolean sessionOpened = false;
        if (session == null) {
            sessionOpened = true;
            session = CoreInstance.openCoreSession(doc.getRepositoryName());
        }
        try {
            DocumentModelList docList = session.getChildren(doc.getRef(), "Picture");
            List<Blob> blobList = new ArrayList<Blob>(docList.size());
            for (DocumentModel documentModel : docList) {
                if ("Original".equals(title)) {
                    BlobHolder bh = documentModel.getAdapter(BlobHolder.class);
                    blobList.add(bh.getBlob());
                } else {
                    PictureResourceAdapter picture = documentModel.getAdapter(PictureResourceAdapter.class);
                    blobList.add(picture.getPictureFromTitle(title));
                }
            }
            return blobList;
        } finally {
            if (sessionOpened) {
                ((CloseableCoreSession) session).close();
            }
        }
    }

    @Override
    public String getHash() {
        Blob blob = getBlob();
        if (blob != null) {
            String h = blob.getDigest();
            if (h != null) {
                return h;
            }
        }
        return doc.getId() + xPath + getModificationDate().toString();
    }

    protected CoreSession getSession() {
        if (session == null && doc != null) {
            session = doc.getCoreSession();
        }
        return session;
    }

}
