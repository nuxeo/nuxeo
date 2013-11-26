/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.core.listener;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.comment.api.CommentEvents;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteStep;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;

/***
 *
 * Updates the number of comments stored on the {@link DocumentRouteStep}. This
 * is used to avoid unnecessary jena calls when displaying the number of comments
 * on each step.
 *
 * @author mcedica
 *
 */
public class DocumentRoutingUpdateCommentsInfoListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        String eventId = event.getName();

        if (!eventId.equals(CommentEvents.COMMENT_ADDED)
                && !eventId.equals(CommentEvents.COMMENT_REMOVED)) {
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
            doc.setPropertyValue(
                    DocumentRoutingConstants.COMMENTS_NO_PROPERTY_NAME,
                    ++comments);
        }
        if (eventId.equals(CommentEvents.COMMENT_REMOVED)) {
            doc.setPropertyValue(
                    DocumentRoutingConstants.COMMENTS_NO_PROPERTY_NAME,
                    --comments);
        }
        event.getContext().getCoreSession().saveDocument(doc);
    }
}
