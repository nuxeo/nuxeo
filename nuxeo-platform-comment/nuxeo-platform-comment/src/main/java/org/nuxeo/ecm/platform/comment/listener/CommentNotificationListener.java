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
 */
package org.nuxeo.ecm.platform.comment.listener;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerHook;
import org.nuxeo.runtime.api.Framework;

/**
 * @description the related thread of comments is retrieved for sending to its
 *              subscribers
 * @since 5.5
 * @author vpasquier
 *
 */
public class CommentNotificationListener implements NotificationListenerHook {

    @Override
    public void handleNotifications(Event event)
            throws Exception {
        EventContext ctx = event.getContext();
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        if (docCtx.getSourceDocument().getType().equals("Post")
                || docCtx.getSourceDocument().getType().equals(CommentsConstants.COMMENT_DOC_TYPE)) {
            CommentManager commentManager = Framework.getService(CommentManager.class);
            DocumentModel thread = commentManager.getThreadForComment(docCtx.getSourceDocument());
            if (thread !=null) {
                Object[] args = { thread, null };
                docCtx.setArgs(args);
            }
        }
    }
}
