/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.core.adapter;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionConstants;
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

    public List<String> getCollectedDocumentIds() {
        @SuppressWarnings("unchecked")
        List<String> collected = (List<String>) document.getPropertyValue(CollectionConstants.COLLECTION_DOCUMENT_IDS_PROPERTY_NAME);
        return collected;
    }

    public void addDocument(final String documentId) {
        List<String> documentIds = getCollectedDocumentIds();
        if (!documentIds.contains(documentId)) {
            documentIds.add(documentId);
        }
        setDocumentIds(documentIds);
    }

    public void removeDocument(final String documentId) {
        List<String> documentIds = getCollectedDocumentIds();
        if (!documentIds.remove(documentId)) {
            log.warn(String.format("Element '%s' is not present in the specified collection.", documentId));
        }
        setDocumentIds(documentIds);
    }

    public void setDocumentIds(final List<String> documentIds) {
        document.setPropertyValue(CollectionConstants.COLLECTION_DOCUMENT_IDS_PROPERTY_NAME, (Serializable) documentIds);
    }

    public DocumentModel getDocument() {
        return document;
    }

}
