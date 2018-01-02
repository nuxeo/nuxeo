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

import org.apache.commons.lang3.StringUtils;
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

    /**
     * Move member1Id right after the member2Id of at first position if member2Id is null.
     *
     * @param member1Id
     * @param member2Id
     * @return true if successful
     * @since 8.4
     */
    public boolean moveMembers(String member1Id, String member2Id) {
        List<String> documentIds = getCollectedDocumentIds();
        int member1IdIndex = documentIds.indexOf(member1Id);
        if (member1IdIndex < 0) {
            return false;
        }
        if (StringUtils.isBlank(member2Id)) {
            documentIds.remove(member1IdIndex);
            documentIds.add(0, member1Id);
            setDocumentIds(documentIds);
            return true;
        }
        else {
            int member2IdIndex = documentIds.indexOf(member2Id);
            if (member2IdIndex < 0) {
                return false;
            }
            if (member1IdIndex == member2IdIndex) {
               return false;
            }
            if (member2IdIndex > member1IdIndex) {
                documentIds.remove(member1IdIndex);
                int newMember2IdIndex = documentIds.indexOf(member2Id);
                documentIds.add(newMember2IdIndex + 1, member1Id);
            } else {
                documentIds.remove(member2IdIndex);
                int newMember1IdIndex = documentIds.indexOf(member1Id);
                documentIds.add(newMember1IdIndex + 1, member2Id);
            }
            setDocumentIds(documentIds);
            return true;
        }
    }

    /**
     * @since 8.3
     */
    public int size() {
        return getCollectedDocumentIds().size();
    }

}
