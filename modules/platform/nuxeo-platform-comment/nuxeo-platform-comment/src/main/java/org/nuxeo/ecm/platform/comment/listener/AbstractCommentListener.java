/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.comment.service.CommentServiceHelper;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractCommentListener {

    private static final Log log = LogFactory.getLog(AbstractCommentListener.class);

    public void handleEvent(EventBundle events) {
        if (events.containsEventName(DocumentEventTypes.DOCUMENT_REMOVED)) {
            for (Event event : events) {
                handleEvent(event);
            }
        }
    }

    public void handleEvent(Event event) {
        if (DocumentEventTypes.DOCUMENT_REMOVED.equals(event.getName())) {
            EventContext ctx = event.getContext();
            if (ctx instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) ctx;
                DocumentModel doc = docCtx.getSourceDocument();
                CoreSession coreSession = docCtx.getCoreSession();
                CommentServiceConfig config = CommentServiceHelper.getCommentService().getConfig();
                RelationManager relationManager = Framework.getService(RelationManager.class);
                doProcess(coreSession, relationManager, config, doc);
                return;
            }
        }
    }

    protected void deleteCommentChildren(CoreSession coreSession, CommentManager commentManager,
            DocumentModel documentModel) {
        commentManager.getComments(coreSession, documentModel.getId())
                      .forEach(comment -> coreSession.removeDocument(new IdRef(comment.getId())));
    }

    protected abstract void doProcess(CoreSession coreSession, RelationManager relationManager,
            CommentServiceConfig config, DocumentModel docMessage);

}
