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
public class CollectionMember {

    private static final Log log = LogFactory.getLog(CollectionMember.class);

    protected DocumentModel document;

    public CollectionMember(final DocumentModel doc) {
        document = doc;
    }

    public void addToCollection(final String collectionId) {
        List<String> collectionIds = getCollectionIds();
        if (!collectionIds.contains(collectionId)) {
            collectionIds.add(collectionId);
        }
        setCollectionIds(collectionIds);
    }

    public void setCollectionIds(final List<String> collectionIds) {
        document.setPropertyValue(CollectionConstants.DOCUMENT_COLLECTION_IDS_PROPERTY_NAME,
                (Serializable) collectionIds);
    }

    public List<String> getCollectionIds() {
        @SuppressWarnings("unchecked")
        List<String> collectionIds = (List<String>) document.getPropertyValue(CollectionConstants.DOCUMENT_COLLECTION_IDS_PROPERTY_NAME);
        return collectionIds;
    }

    public DocumentModel getDocument() {
        return document;
    }

    public void removeFromCollection(final String documentId) {
        List<String> collectionIds = getCollectionIds();
        if (!collectionIds.remove(documentId)) {
            log.warn(String.format("Element '%s' is not present in the specified collection.", documentId));
        }
        setCollectionIds(collectionIds);
    }

}
