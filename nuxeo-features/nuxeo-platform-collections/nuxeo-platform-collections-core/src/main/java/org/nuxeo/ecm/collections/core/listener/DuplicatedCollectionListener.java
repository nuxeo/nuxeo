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
package org.nuxeo.ecm.collections.core.listener;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Event handler to duplicate the collection members of a duplicated collection. The handler is synchronous because it
 * is important to capture the collection member ids of the duplicated collection at the exact moment of duplication. We
 * don't want to duplicate a collection member that was indeed added to the duplicated collection after the duplication.
 * The handler will then launch asynchronous tasks to duplicate the collection members.
 *
 * @since 5.9.3
 */
public class DuplicatedCollectionListener implements EventListener {

    private static final Log log = LogFactory.getLog(DuplicatedCollectionListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        final String eventId = event.getName();

        final DocumentEventContext docCxt = (DocumentEventContext) event.getContext();

        DocumentModel doc = null;
        if (eventId.equals(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY)) {
            doc = docCxt.getSourceDocument();
        } else if (eventId.equals(DocumentEventTypes.DOCUMENT_CHECKEDIN)) {
            DocumentRef checkedInVersionRef = (DocumentRef) ctx.getProperties().get("checkedInVersionRef");
            doc = ctx.getCoreSession().getDocument(checkedInVersionRef);
            if (!doc.isVersion()) {
                return;
            }
        } else {
            return;
        }

        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);

        if (collectionManager.isCollection(doc)) {

            if (eventId.equals(DocumentEventTypes.DOCUMENT_CREATED_BY_COPY)) {
                log.trace(String.format("Collection %s copied", doc.getId()));
            } else if (eventId.equals(DocumentEventTypes.DOCUMENT_CHECKEDIN)) {
                log.trace(String.format("Collection %s checked in", doc.getId()));
            }

            collectionManager.processCopiedCollection(doc);

        }

        if (collectionManager.isCollected(doc)) {
            processCopiedMember(doc, ctx.getCoreSession());
        }

        if (doc.isFolder()) {
            // We just copied a folder, maybe among the descendants there are collections that have been copied too
            // proceed to the deep collection copy
            int offset = 0;
            DocumentModelList deepCopiedCollections;
            do {
                deepCopiedCollections = ctx.getCoreSession().query(
                        "SELECT * FROM Collection WHERE ecm:path STARTSWITH '" + doc.getPathAsString() + "'", null,
                        CollectionAsynchrnonousQuery.MAX_RESULT, offset, false);
                offset += deepCopiedCollections.size();
                for (DocumentModel deepCopiedCollection : deepCopiedCollections) {
                    collectionManager.processCopiedCollection(deepCopiedCollection);
                }
            } while (deepCopiedCollections.size() >= CollectionAsynchrnonousQuery.MAX_RESULT);

            // Maybe among the descendants there are collection members that have been copied too
            // Let's make sure they don't belong to their original document's collections
            offset = 0;
            DocumentModelList deepCopiedMembers;
            do {
                deepCopiedMembers = ctx.getCoreSession()
                                       .query("SELECT * FROM Document WHERE ecm:mixinType = 'CollectionMember' AND ecm:path STARTSWITH '"
                                               + doc.getPathAsString() + "'", null,
                                               CollectionAsynchrnonousQuery.MAX_RESULT, offset, false);
                offset += deepCopiedMembers.size();
                for (DocumentModel deepCopiedMember : deepCopiedMembers) {
                    processCopiedMember(deepCopiedMember, ctx.getCoreSession());
                }
            } while (deepCopiedMembers.size() >= CollectionAsynchrnonousQuery.MAX_RESULT);
        }
    }

    /**
     * @since 8.4
     */
    private void processCopiedMember(DocumentModel doc, CoreSession session) {
        doc.getAdapter(CollectionMember.class).setCollectionIds(null);
        if (doc.isVersion()) {
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        session.saveDocument(doc);
        doc.removeFacet(CollectionConstants.COLLECTABLE_FACET);
    }

}
