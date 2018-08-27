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
 *     Thomas Roger
 */
package org.nuxeo.ecm.webapp.collections;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.collections.api.CollectionConstants.MAGIC_PREFIX_ID;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 6.0
 */
@Name("collectionBulkEditActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class CollectionBulkEditActions implements Serializable {

    public static final String SELECTION_EDITED = "selectionEdited";

    public static final String DOCUMENTS_IMPORTED = "documentImported";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @SuppressWarnings("unchecked")
    @Observer({ SELECTION_EDITED, DOCUMENTS_IMPORTED })
    public void addCollectionsOnEvent(List<DocumentModel> documents, DocumentModel doc) {
        List<String> collectionIds = (List<String>) doc.getContextData("bulk_collections");
        if (collectionIds != null && !collectionIds.isEmpty()) {
            CollectionManager collectionManager = Framework.getService(CollectionManager.class);
            for (String collectionId : collectionIds) {
                if (collectionId.startsWith(MAGIC_PREFIX_ID)) {
                    String title = collectionId.replaceAll("^" + MAGIC_PREFIX_ID, "");
                    collectionManager.addToNewCollection(title, "", documents, documentManager);
                } else {
                    IdRef idRef = new IdRef(collectionId);
                    if (documentManager.exists(idRef)) {
                        DocumentModel collection = documentManager.getDocument(idRef);
                        collectionManager.addToCollection(collection, documents, documentManager);
                    }
                }

            }
        }
    }

}
