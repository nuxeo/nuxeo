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

import static org.nuxeo.ecm.core.api.event.CoreEventConstants.NEW_ACP;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.OLD_ACP;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_SECURITY_UPDATED;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DIRECTORY;
import static org.nuxeo.ecm.permissions.Constants.ACE_KEY;
import static org.nuxeo.ecm.permissions.Constants.ACL_NAME_KEY;
import static org.nuxeo.ecm.permissions.Constants.COMMENT_KEY;
import static org.nuxeo.ecm.permissions.Constants.NOTIFY_KEY;
import static org.nuxeo.ecm.permissions.Constants.PERMISSION_NOTIFICATION_EVENT;
import static org.nuxeo.ecm.permissions.PermissionHelper.computeDirectoryId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener filling the 'aceinfo' directory when an ACP is updated.
 *
 * @since 7.4
 */
public class PermissionListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        if (DOCUMENT_SECURITY_UPDATED.equals(event.getName())) {
            updateDirectory((DocumentEventContext) ctx);
        }
    }

    protected void updateDirectory(DocumentEventContext docCtx) {
        ACP oldACP = (ACP) docCtx.getProperty(OLD_ACP);
        ACP newACP = (ACP) docCtx.getProperty(NEW_ACP);
        if (oldACP != null && newACP != null) {
            handleUpdateACP(docCtx, oldACP, newACP);
        }
    }

    protected void handleUpdateACP(DocumentEventContext docCtx, ACP oldACP, ACP newACP) {
        Framework.doPrivileged(() -> {
            DocumentModel doc = docCtx.getSourceDocument();
            List<ACLDiff> aclDiffs = extractACLDiffs(oldACP, newACP);
            DirectoryService directoryService = Framework.getService(DirectoryService.class);
            for (ACLDiff diff : aclDiffs) {
                try (Session session = directoryService.open(ACE_INFO_DIRECTORY)) {
                    for (ACE ace : diff.removedACEs) {
                        String id = computeDirectoryId(doc, diff.aclName, ace.getId());
                        session.deleteEntry(id);

                        removeToken(doc, ace);
                    }

                    for (ACE ace : diff.addedACEs) {
                        String id = computeDirectoryId(doc, diff.aclName, ace.getId());
                        // remove it if it exists
                        if (session.hasEntry(id)) {
                            session.deleteEntry(id);
                        }

                        Boolean notify = (Boolean) ace.getContextData(NOTIFY_KEY);
                        String comment = (String) ace.getContextData(Constants.COMMENT_KEY);
                        notify = notify != null ? notify : false;
                        Map<String, Object> m = PermissionHelper.createDirectoryEntry(doc, diff.aclName, ace, notify,
                                comment);
                        session.createEntry(m);

                        addToken(doc, ace);

                        if (notify && ace.isGranted() && ace.isEffective()) {
                            firePermissionNotificationEvent(docCtx, diff.aclName, ace);
                        }
                    }
                }
            }
        });
    }

    /**
     * @deprecated since 8.1. Not used anymore.
     */
    @Deprecated
    protected void handleReplaceACE(DocumentEventContext docCtx, String changedACLName, ACE oldACE, ACE newACE) {
        Framework.doPrivileged(() -> {
            DocumentModel doc = docCtx.getSourceDocument();

            DirectoryService directoryService = Framework.getService(DirectoryService.class);
            try (Session session = directoryService.open(ACE_INFO_DIRECTORY)) {
                Boolean notify = (Boolean) newACE.getContextData(NOTIFY_KEY);
                String comment = (String) newACE.getContextData(COMMENT_KEY);

                String oldId = computeDirectoryId(doc, changedACLName, oldACE.getId());
                DocumentModel oldEntry = session.getEntry(oldId);
                if (oldEntry != null) {
                    // remove the old entry
                    session.deleteEntry(oldId);
                }

                // add the new entry
                notify = notify != null ? notify : false;
                Map<String, Object> m = PermissionHelper.createDirectoryEntry(doc, changedACLName, newACE, notify,
                        comment);
                session.createEntry(m);

                if (notify && newACE.isGranted() && newACE.isEffective()) {
                    firePermissionNotificationEvent(docCtx, changedACLName, newACE);
                }
            }
        });
    }

    protected List<ACLDiff> extractACLDiffs(ACP oldACP, ACP newACP) {
        List<ACLDiff> aclDiffs = new ArrayList<>();

        List<String> oldACLNames = toACLNames(oldACP);
        List<String> newACLNames = toACLNames(newACP);
        List<String> addedACLNames = toACLNames(newACP);
        List<String> removedACLNames = toACLNames(oldACP);

        addedACLNames.removeAll(oldACLNames);
        removedACLNames.removeAll(newACLNames);

        for (String name : addedACLNames) {
            aclDiffs.add(new ACLDiff(name, new ArrayList<>(newACP.getACL(name)), null));
        }

        for (String name : removedACLNames) {
            aclDiffs.add(new ACLDiff(name, null, new ArrayList<>(oldACP.getACL(name))));
        }

        for (ACL newACL : newACP.getACLs()) {
            ACL oldACL = oldACP.getACL(newACL.getName());
            if (oldACL != null) {
                List<ACE> addedACEs = new ArrayList<>(newACL);
                List<ACE> removedACEs = new ArrayList<>(oldACL);

                addedACEs.removeAll(oldACL);
                removedACEs.removeAll(newACL);
                aclDiffs.add(new ACLDiff(newACL.getName(), addedACEs, removedACEs));
            }
        }
        return aclDiffs;
    }

    protected List<String> toACLNames(ACP acp) {
        List<String> aclNames = new ArrayList<>();
        for (ACL acl : acp.getACLs()) {
            aclNames.add(acl.getName());
        }
        return aclNames;
    }

    protected void firePermissionNotificationEvent(DocumentEventContext docCtx, String aclName, ACE ace) {
        docCtx.setProperty(ACE_KEY, ace);
        docCtx.setProperty(ACL_NAME_KEY, aclName);
        EventService eventService = Framework.getService(EventService.class);
        eventService.fireEvent(PERMISSION_NOTIFICATION_EVENT, docCtx);
    }

    protected void addToken(DocumentModel doc, ACE ace) {
        if (!ace.isArchived()) {
            TransientUserPermissionHelper.acquireToken(ace.getUsername(), doc, ace.getPermission());
        }
    }

    protected void removeToken(DocumentModel doc, ACE deletedAce) {
        TransientUserPermissionHelper.revokeToken(deletedAce.getUsername(), doc);
    }

    private static class ACLDiff {
        public final String aclName;

        public final List<ACE> addedACEs;

        public final List<ACE> removedACEs;

        private ACLDiff(String aclName, List<ACE> addedACEs, List<ACE> removedACEs) {
            this.aclName = aclName;
            this.addedACEs = addedACEs != null ? addedACEs : Collections.emptyList();
            this.removedACEs = removedACEs != null ? removedACEs : Collections.emptyList();
        }
    }
}
