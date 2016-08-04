/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import static org.nuxeo.ecm.core.api.event.CoreEventConstants.DOCUMENT_REFS;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.REPOSITORY_NAME;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ACE_STATUS_UPDATED;
import static org.nuxeo.ecm.core.api.security.ACL.INHERITED_ACL;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_COMMENT;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DIRECTORY;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_NOTIFY;
import static org.nuxeo.ecm.permissions.Constants.ACE_KEY;
import static org.nuxeo.ecm.permissions.Constants.ACL_NAME_KEY;
import static org.nuxeo.ecm.permissions.Constants.COMMENT_KEY;
import static org.nuxeo.ecm.permissions.Constants.PERMISSION_NOTIFICATION_EVENT;
import static org.nuxeo.ecm.permissions.PermissionHelper.computeDirectoryId;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
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
        List<DocumentRef> docRefs = (List<DocumentRef>) ctx.getProperty(DOCUMENT_REFS);
        if (repositoryName == null || docRefs == null) {
            return;
        }

        try (CoreSession session = CoreInstance.openCoreSessionSystem(repositoryName)) {
            docRefs.stream().filter(session::exists).forEach(ref -> {
                DocumentModel doc = session.getDocument(ref);
                checkForEffectiveACE(session, doc);
            });
        }
    }

    protected void checkForEffectiveACE(CoreSession session, DocumentModel doc) {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        ACP acp = doc.getACP();
        for (ACL acl : acp.getACLs()) {
            String aclName = acl.getName();
            if (INHERITED_ACL.equals(aclName)) {
                // ignore inherited acl
                continue;
            }

            for (ACE ace : acl) {
                if (ace.isGranted() && ace.isEffective()) {
                    Framework.doPrivileged(() -> {
                        try (Session dirSession = directoryService.open(ACE_INFO_DIRECTORY)) {
                            String id = computeDirectoryId(doc, acl.getName(), ace.getId());
                            DocumentModel entry = dirSession.getEntry(id);
                            if (entry != null) {
                                Boolean notify = (Boolean) entry.getPropertyValue(ACE_INFO_NOTIFY);
                                String comment = (String) entry.getPropertyValue(ACE_INFO_COMMENT);
                                if (Boolean.TRUE.equals(notify)) {
                                    // send the event for the notification
                                    ace.putContextData(COMMENT_KEY, comment);
                                    firePermissionNotificationEvent(session, doc, aclName, ace);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    protected void firePermissionNotificationEvent(CoreSession session, DocumentModel doc, String aclName, ACE ace) {
        DocumentEventContext docCtx = new DocumentEventContext(session, session.getPrincipal(), doc);
        docCtx.setProperty(ACE_KEY, ace);
        docCtx.setProperty(ACL_NAME_KEY, aclName);
        EventService eventService = Framework.getService(EventService.class);
        eventService.fireEvent(PERMISSION_NOTIFICATION_EVENT, docCtx);
    }

    @Override
    public boolean acceptEvent(Event event) {
        return ACE_STATUS_UPDATED.equals(event.getName());
    }
}
