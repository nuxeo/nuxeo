/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.comment.listener;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerHook;
import org.nuxeo.runtime.api.Framework;

/**
 * @description the related thread of comments is retrieved for sending to its subscribers
 * @since 5.5
 * @author vpasquier
 */
public class CommentNotificationListener implements NotificationListenerHook {

    @Override
    public void handleNotifications(Event event) {
        EventContext ctx = event.getContext();
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        if (docCtx.getSourceDocument().getType().equals("Post")
                || docCtx.getSourceDocument().getType().equals(CommentsConstants.COMMENT_DOC_TYPE)) {
            CommentManager commentManager = Framework.getService(CommentManager.class);
            DocumentRef topLevelCommentAncestor = commentManager.getTopLevelCommentAncestor(docCtx.getCoreSession(), docCtx.getSourceDocument().getRef());

            if (topLevelCommentAncestor != null) {
                Object[] args = { docCtx.getCoreSession().getDocument(topLevelCommentAncestor), null };
                docCtx.setArgs(args);
            }
        }
    }
}
