/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.permissions;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DIRECTORY;

import java.util.Collections;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener cleaning the 'aceinfo' directory when a document is removed.
 *
 * @since 1010-HF70
 */
public class PermissionGarbageCollectorListener implements PostCommitEventListener {

    @Override
    public void handleEvent(EventBundle events) {
        if (!events.containsEventName(DOCUMENT_REMOVED)) {
            return;
        }
        for (Event e : events) { // NOSONAR
            if (!DOCUMENT_REMOVED.equals(e.getName())) {
                continue;
            }
            EventContext eventContext = e.getContext();
            if (!(eventContext instanceof DocumentEventContext)) {
                continue;
            }
            DocumentEventContext ctx = (DocumentEventContext) eventContext;
            Framework.doPrivileged(() -> {
                DocumentModel doc = ctx.getSourceDocument();
                DirectoryService directoryService = Framework.getService(DirectoryService.class);
                try (Session session = directoryService.open(ACE_INFO_DIRECTORY)) {
                    session.query(Collections.singletonMap("docId", doc.getId())).forEach(session::deleteEntry);
                }
            });
        }
    }
}
