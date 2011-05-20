/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.tag;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Core listener that removes associated tags when a document is removed or
 * trashed.
 *
 * @since 5.4
 * @since 5.4.2 for the removal on trash
 */
public class TaggedDocumentRemovedListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(TaggedDocumentRemovedListener.class);

    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        if (events.containsEventName(DocumentEventTypes.DOCUMENT_REMOVED)
                || events.containsEventName(LifeCycleConstants.TRANSITION_EVENT)) {
            for (Event event : events) {
                String eventName = event.getName();
                if (DocumentEventTypes.DOCUMENT_REMOVED.equals(eventName)) {
                    handleEvent(event);
                } else if (LifeCycleConstants.TRANSITION_EVENT.equals(eventName)
                        && LifeCycleConstants.DELETED_STATE.equals(event.getContext().getProperty(
                                LifeCycleConstants.TRANSTION_EVENT_OPTION_TO))) {
                    handleEvent(event);
                }
            }
        }
    }

    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel doc = docCtx.getSourceDocument();
            if (doc == null) {
                return;
            }
            String docId = doc.getId();
            CoreSession coreSession = docCtx.getCoreSession();
            try {
                TagService tagService = Framework.getService(TagService.class);
                if (tagService != null) {
                    List<Tag> tags = tagService.getDocumentTags(coreSession,
                            docId, null);
                    if (tags != null) {
                        for (Tag tag : tags) {
                            tagService.untag(coreSession, doc.getId(),
                                    tag.getLabel(), null);
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e, e);
            }
            return;
        }
    }

}
