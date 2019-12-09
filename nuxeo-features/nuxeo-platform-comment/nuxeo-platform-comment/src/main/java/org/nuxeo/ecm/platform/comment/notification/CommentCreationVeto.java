/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tmartins
 */
package org.nuxeo.ecm.platform.comment.notification;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;

/**
 * This veto prevents from sending notifications for document creation when the target document is a Comment or a Post.
 *
 * @author Thierry Martins <tmartins@nuxeo.com>
 * @since 5.7
 * @deprecated since 11.1. Use {@link CommentNotificationVeto} instead.
 */
@Deprecated(since = "11.1", forRemoval = true)
public class CommentCreationVeto implements NotificationListenerVeto {

    @Override
    public boolean accept(Event event) {
        DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
        if ("documentCreated".equals(event.getName()) && (docCtx.getSourceDocument().getType().equals("Post")
                || docCtx.getSourceDocument().getType().equals("Comment"))) {
            return false;
        }
        return true;
    }

}
