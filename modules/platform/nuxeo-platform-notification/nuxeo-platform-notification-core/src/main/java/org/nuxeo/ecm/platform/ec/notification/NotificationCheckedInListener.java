/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.platform.ec.notification;

import static org.nuxeo.ecm.platform.ec.notification.SubscriptionAdapter.NOTIFIABLE_FACET;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Listener used to intercept {@link DocumentEventTypes#DOCUMENT_CHECKEDIN} events in order to clean notification from
 * version.
 *
 * @since 10.2
 */
public class NotificationCheckedInListener implements EventListener {

    private static final Log log = LogFactory.getLog(NotificationCheckedInListener.class);

    @Override
    public void handleEvent(Event event) {
        if (!DocumentEventTypes.DOCUMENT_CHECKEDIN.equals(event.getName())) {
            return;
        }
        if (!(event.getContext() instanceof DocumentEventContext)) {
            log.warn("Can not handle event that is not bound to a DocumentEventContext");
            return;
        }
        DocumentEventContext context = (DocumentEventContext) event.getContext();
        CoreSession session = context.getCoreSession();
        DocumentModel docModel = context.getSourceDocument();
        if (docModel.hasFacet(NOTIFIABLE_FACET)) {
            // remove notifications from version
            DocumentRef versionRef = (DocumentRef) context.getProperty("checkedInVersionRef");
            DocumentModel version = session.getDocument(versionRef);
            version.getAdapter(SubscriptionAdapter.class).clearNotification();
            session.saveDocument(version);
        }
    }
}
