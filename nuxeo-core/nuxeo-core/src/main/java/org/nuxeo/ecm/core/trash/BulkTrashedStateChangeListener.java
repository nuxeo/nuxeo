/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.trash;

import static org.nuxeo.ecm.core.trash.TrashService.DOCUMENT_TRASHED;
import static org.nuxeo.ecm.core.trash.TrashService.DOCUMENT_UNTRASHED;
import static org.nuxeo.ecm.core.trash.TrashService.DISABLE_TRASH_RENAMING;

import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener for trashed state change events.
 * <p>
 * If event occurs on a folder, it will recurse on children to perform the same change if possible.
 *
 * @since 10.1
 */
public class BulkTrashedStateChangeListener implements PostCommitFilteringEventListener {

    public static final String PROCESS_CHILDREN_KEY = "processChildrenForTrashedStageChange";

    private static final Log log = LogFactory.getLog(BulkTrashedStateChangeListener.class);

    @Override
    public boolean acceptEvent(Event event) {
        EventContext ctx = event.getContext();
        return ctx instanceof DocumentEventContext && ((DocumentEventContext) ctx).getSourceDocument().isFolder()
                && Boolean.parseBoolean(String.valueOf(ctx.getProperty(PROCESS_CHILDREN_KEY)));
    }

    @Override
    public void handleEvent(EventBundle events) {
        if (events.containsEventName(DOCUMENT_TRASHED) || events.containsEventName(DOCUMENT_UNTRASHED)) {
            for (Event event : events) {
                if (DOCUMENT_TRASHED.equals(event.getName()) || DOCUMENT_UNTRASHED.equals(event.getName())) {
                    handleEvent(event);
                }
            }
        }
    }

    protected void handleEvent(Event event) {
        log.debug("Processing trashed state change in async listener");
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        DocumentModel doc = ctx.getSourceDocument();
        CoreSession session = ctx.getCoreSession();
        if (session == null) {
            log.error("Can not process trashed state change since session is null");
            return;
        }
        DocumentModelList children = session.getChildren(doc.getRef());
        TrashService trashService = Framework.getService(TrashService.class);
        // TODO review the way we disable renaming when bulk operation will be done
        Consumer<DocumentModel> trashOperation = DOCUMENT_TRASHED.equals(event.getName()) ? trashService::trashDocument
                : trashService::untrashDocument;
        for (DocumentModel child : children) {
            // skip renaming
            child.putContextData(DISABLE_TRASH_RENAMING, Boolean.TRUE);
            trashOperation.accept(child);

        }
    }

}
