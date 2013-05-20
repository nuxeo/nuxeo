/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     tmartins
 */
package org.nuxeo.ecm.platform.comment.notification;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;

/**
 * This veto prevents from sending notifications for document creation when the
 * target document is a Comment or a Post.
 *
 * @author Thierry Martins <tmartins@nuxeo.com>
 *
 * @since 5.7
 */
public class CommentCreationVeto implements NotificationListenerVeto {

    @Override
    public boolean accept(Event event) {
        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        if ("documentCreated".equals(event.getName())
                && (docCtx.getSourceDocument().getType().equals("Post") || docCtx.getSourceDocument().getType().equals(
                        "Comment"))) {
            return false;
        }
        return true;
    }

}
