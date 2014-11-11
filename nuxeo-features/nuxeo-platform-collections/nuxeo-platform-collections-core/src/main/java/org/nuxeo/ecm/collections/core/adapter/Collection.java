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

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 5.9.3
 */
public class Collection {

    private static final Log log = LogFactory.getLog(Collection.class);

    protected DocumentModel document;

    public Collection(DocumentModel doc) {
        document = doc;
    }

    public List<String> getCollectedDocumentIds() throws ClientException {
        @SuppressWarnings("unchecked")
        List<String> collected = (List<String>) document.getPropertyValue(CollectionConstants.COLLECTION_DOCUMENT_IDS_PROPERTY_NAME);
        return collected;
    }

    public void addDocument(final String documentId) throws ClientException {
        List<String> documentIds = getCollectedDocumentIds();
        if (!documentIds.contains(documentId)) {
            documentIds.add(documentId);
        }
        setDocumentIds(documentIds);
    }

    public void removeDocument(final String documentId) throws ClientException {
        List<String> documentIds = getCollectedDocumentIds();
        if (!documentIds.remove(documentId)) {
            log.warn(String.format("Element '%s' is not present in the specified collection.", documentId));
        }
        setDocumentIds(documentIds);
    }

    public void setDocumentIds(final List<String> documentIds)
            throws ClientException {
        document.setPropertyValue(
                CollectionConstants.COLLECTION_DOCUMENT_IDS_PROPERTY_NAME,
                (Serializable) documentIds);
    }

    public DocumentModel getDocument() {
        return document;
    }

}
