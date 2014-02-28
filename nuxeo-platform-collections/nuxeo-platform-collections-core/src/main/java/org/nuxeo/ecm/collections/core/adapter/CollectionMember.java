/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.adapter;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * @since 5.9.3
 */
public class CollectionMember {

    protected DocumentModel document;

    protected CoreSession getCoreSession() throws ClientException {
        CoreSession session = document.getCoreSession();
        if (session == null) {
            throw new ClientException(
                    "Trying to resolve classified document with an offline document");
        }
        return session;
    }

    public CollectionMember(DocumentModel doc) {
        document = doc;
    }

    public void addToCollection(String collectionId) throws ClientException {
        List<String> collectionIds = getCollectionIds();
        if (!collectionIds.contains(collectionId)) {
            collectionIds.add(collectionId);
        }
        setCollectionIds(collectionIds);
    }

    public void setCollectionIds(List<String> collectionIds)
            throws ClientException {
        document.setPropertyValue(
                CollectionConstants.DOCUMENT_COLLECTION_IDS_PROPERTY_NAME,
                (Serializable) collectionIds);
    }

    public DocumentModelList getCollectionss() throws ClientException {
        DocumentModelList targets = new DocumentModelListImpl();
        CoreSession session = getCoreSession();

        for (String docId : getCollectionIds()) {
            DocumentRef documentRef = new IdRef(docId);
            if (session.exists(documentRef)
                    && session.hasPermission(documentRef, READ)) {
                targets.add(session.getDocument(documentRef));
            }
        }
        return targets;
    }

    public List<String> getCollectionIds() throws ClientException {
        @SuppressWarnings("unchecked")
        List<String> collectionIds = (List<String>) document.getPropertyValue(CollectionConstants.DOCUMENT_COLLECTION_IDS_PROPERTY_NAME);
        return collectionIds;
    }

    public DocumentModel getDocument() {
        return document;
    }

}
