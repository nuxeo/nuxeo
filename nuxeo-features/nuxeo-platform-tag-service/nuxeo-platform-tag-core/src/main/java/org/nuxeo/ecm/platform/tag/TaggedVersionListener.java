/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.tag;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_PROXY_PUBLISHED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_RESTORED;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener that copy tags applied on the live document to a version or proxy of
 * this document or replace the existing tags on a live document by the ones on
 * the version being restored.
 *
 * @since 5.7.3
 */
public class TaggedVersionListener implements PostCommitFilteringEventListener {

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        for (Event event : events) {
            if (acceptEvent(event)) {
                handleEvent(event);
            }
        }
    }

    protected void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            String name = event.getName();
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            CoreSession session = docCtx.getCoreSession();
            DocumentModel doc = docCtx.getSourceDocument();
            if (doc == null || doc instanceof DeletedDocumentModel) {
                return;
            }
            String docId = doc.getId();

            TagService tagService = Framework.getLocalService(TagService.class);
            switch (name) {
            case DOCUMENT_CHECKEDIN:
                DocumentRef versionRef = (DocumentRef) ctx.getProperty("checkedInVersionRef");
                if (versionRef instanceof IdRef) {
                    tagService.copyTags(session, docId, versionRef.toString());
                    session.save();
                }
                break;
            case DOCUMENT_PROXY_PUBLISHED:
                if (doc.isProxy()) {
                    DocumentModel version = session.getSourceDocument(doc.getRef());
                    tagService.copyTags(session, version.getId(), docId);
                    session.save();
                }
                break;
            case DOCUMENT_RESTORED:
                String versionUUID = (String) ctx.getProperty(VersioningDocument.RESTORED_VERSION_UUID_KEY);
                tagService.replaceTags(session, versionUUID, docId);
                session.save();
                break;
            default:
                break;
            }
        }
    }

    @Override
    public boolean acceptEvent(Event event) {
        String name = event.getName();
        return DOCUMENT_CHECKEDIN.equals(name)
                || DOCUMENT_PROXY_PUBLISHED.equals(name)
                || DOCUMENT_RESTORED.equals(name);
    }
}
