/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import static org.nuxeo.ecm.core.api.event.CoreEventConstants.CHANGED_ACL_NAME;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.DOCUMENT_REFS;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.REPOSITORY_NAME;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ACE_STATUS_UPDATED;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_COMMENT;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DIRECTORY;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_NOTIFY;
import static org.nuxeo.ecm.permissions.Constants.ACE_KEY;
import static org.nuxeo.ecm.permissions.Constants.ACL_NAME_KEY;
import static org.nuxeo.ecm.permissions.Constants.COMMENT_KEY;
import static org.nuxeo.ecm.permissions.Constants.PERMISSION_NOTIFICATION_EVENT;
import static org.nuxeo.ecm.permissions.PermissionHelper.computeDirectoryId;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener listening for {@code ACEStatusUpdated} event to send a notification for ACEs becoming effective.
 *
 * @since 7.4
 */
public class ACEStatusUpdatedListener implements PostCommitFilteringEventListener {

    @Override
    public void handleEvent(EventBundle events) {
        for (Event event : events) {
            handleEvent(event);
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        String repositoryName = (String) ctx.getProperty(REPOSITORY_NAME);
        Map<DocumentRef, List<ACE>> refsToACEs = (Map<DocumentRef, List<ACE>>) ctx.getProperty(DOCUMENT_REFS);
        if (repositoryName == null || refsToACEs == null) {
            return;
        }

        try (CloseableCoreSession session = CoreInstance.openCoreSessionSystem(repositoryName)) {
            refsToACEs.keySet().stream().filter(session::exists).forEach(ref -> {
                DocumentModel doc = session.getDocument(ref);
                checkForEffectiveACE(session, doc, refsToACEs.get(ref));
            });
        }
    }

    protected void checkForEffectiveACE(CoreSession session, DocumentModel doc, List<ACE> aces) {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);

        for (ACE ace : aces) {
            if (!ace.isGranted()) {
                continue;
            }

            switch (ace.getStatus()) {
            case EFFECTIVE:
                String aclName = (String) ace.getContextData(CHANGED_ACL_NAME);
                if (aclName == null) {
                    continue;
                }
                Framework.doPrivileged(() -> {
                    try (Session dirSession = directoryService.open(ACE_INFO_DIRECTORY)) {
                        String id = computeDirectoryId(doc, aclName, ace.getId());
                        DocumentModel entry = dirSession.getEntry(id);
                        if (entry != null) {
                            Boolean notify = (Boolean) entry.getPropertyValue(ACE_INFO_NOTIFY);
                            String comment = (String) entry.getPropertyValue(ACE_INFO_COMMENT);
                            if (Boolean.TRUE.equals(notify)) {
                                // send the event for the notification
                                ace.putContextData(COMMENT_KEY, comment);
                                PermissionHelper.firePermissionNotificationEvent(session, doc, aclName, ace);
                            }
                        }
                    }
                });
                break;
            case ARCHIVED:
                TransientUserPermissionHelper.revokeToken(ace.getUsername(), doc);
                break;
            }
        }
    }

    @Override
    public boolean acceptEvent(Event event) {
        return ACE_STATUS_UPDATED.equals(event.getName());
    }
}
