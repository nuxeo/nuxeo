/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ATTACHED_DOCUMENTS_PROPERTY_NAME;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.DOCUMENT_ROUTE_DOCUMENT_TYPE;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.ROUTING_TASK_DOC_TYPE;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Listener that deletes orphan DocumentRoutes.
 *
 * @since 11.2
 */
public class DocumentRouteOrphanedListener implements PostCommitEventListener {

    protected static final String QUERY_GET_RELATED_DOCUMENT_ROUTES = "SELECT * FROM " + DOCUMENT_ROUTE_DOCUMENT_TYPE
            + " WHERE " + ATTACHED_DOCUMENTS_PROPERTY_NAME + " = '%s'";

    @Override
    public void handleEvent(EventBundle events) {
        for (Event event : events) {
            DocumentEventContext context = (DocumentEventContext) event.getContext();
            DocumentModel docModel = context.getSourceDocument();
            if (!DOCUMENT_ROUTE_DOCUMENT_TYPE.equals(docModel.getType())
                    && !ROUTING_TASK_DOC_TYPE.equals(docModel.getType())) {
                deleteOrphanDocumentRoutes(context.getCoreSession(), docModel.getId());
            }
        }
    }

    /**
     * Removes the given id from DocumentRoutes attached documents list and deletes the DocumentRoutes if they are not
     * attached to a document anymore.
     */
    protected void deleteOrphanDocumentRoutes(CoreSession session, String id) {
        String query = String.format(QUERY_GET_RELATED_DOCUMENT_ROUTES, id);
        for (DocumentModel doc : session.query(query)) {
            @SuppressWarnings("unchecked")
            List<String> attachedDocuments = (List<String>) doc.getPropertyValue(ATTACHED_DOCUMENTS_PROPERTY_NAME);
            attachedDocuments.remove(id);
            if (attachedDocuments.isEmpty()) {
                session.removeDocument(doc.getRef());
            } else {
                doc.setPropertyValue(ATTACHED_DOCUMENTS_PROPERTY_NAME, (Serializable) attachedDocuments);
                session.saveDocument(doc);
            }
        }
    }

}
