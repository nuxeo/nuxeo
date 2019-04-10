/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.comment.api.CommentEvents;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/***
 * Updates the number of comments stored on the {@link DocumentRouteStep}. This is used to avoid unnecessary jena calls
 * when displaying the number of comments on each step.
 *
 * @author mcedica
 */
public class DocumentRoutingUpdateCommentsInfoListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        String eventId = event.getName();

        if (!eventId.equals(CommentEvents.COMMENT_ADDED) && !eventId.equals(CommentEvents.COMMENT_REMOVED)) {
            return;
        }
        DocumentEventContext docCtx;
        if (event.getContext() instanceof DocumentEventContext) {
            docCtx = (DocumentEventContext) event.getContext();
        } else {
            return;
        }
        DocumentModel doc = docCtx.getSourceDocument();
        if (!doc.hasFacet(DocumentRoutingConstants.COMMENTS_INFO_HOLDER_FACET)) {
            return;
        }
        Long comments = (Long) doc.getPropertyValue(DocumentRoutingConstants.COMMENTS_NO_PROPERTY_NAME);
        // else increase or decrease the number of comments on the doc
        if (eventId.equals(CommentEvents.COMMENT_ADDED)) {
            doc.setPropertyValue(DocumentRoutingConstants.COMMENTS_NO_PROPERTY_NAME, ++comments);
        }
        if (eventId.equals(CommentEvents.COMMENT_REMOVED)) {
            doc.setPropertyValue(DocumentRoutingConstants.COMMENTS_NO_PROPERTY_NAME, --comments);
        }
        event.getContext().getCoreSession().saveDocument(doc);
    }
}
