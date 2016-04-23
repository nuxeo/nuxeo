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

import static org.nuxeo.ecm.core.api.event.CoreEventConstants.CHANGED_ACL_NAME;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.NEW_ACE;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.NEW_ACP;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.OLD_ACE;
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
        ACE oldACE = (ACE) docCtx.getProperty(OLD_ACE);
        ACE newACE = (ACE) docCtx.getProperty(NEW_ACE);
        String changedACLName = (String) docCtx.getProperty(CHANGED_ACL_NAME);
        if (oldACE != null && newACE != null && changedACLName != null) {
            handleReplaceACE(docCtx, changedACLName, oldACE, newACE);
        } else {
            ACP oldACP = (ACP) docCtx.getProperty(OLD_ACP);
            ACP newACP = (ACP) docCtx.getProperty(NEW_ACP);
            if (oldACP != null && newACP != null) {
                handleUpdateACP(docCtx, oldACP, newACP);
            }
        }
    }

    protected void doAsSystemUser(Runnable runnable) {
        LoginContext loginContext;
        try {
            loginContext = Framework.login();
        } catch (LoginException e) {
            throw new NuxeoException(e);
        }

        try {
            runnable.run();
        } finally {
            try {
                // Login context may be null in tests
                if (loginContext != null) {
                    loginContext.logout();
                }
            } catch (LoginException e) {
                throw new NuxeoException("Cannot log out system user", e);
            }
        }
    }

    protected void handleUpdateACP(DocumentEventContext docCtx, ACP oldACP, ACP newACP) {
        doAsSystemUser(() -> {
            DocumentModel doc = docCtx.getSourceDocument();

            List<ACLDiff> aclDiffs = extractACLDiffs(oldACP, newACP);
            DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
            for (ACLDiff diff : aclDiffs) {
                try (Session session = directoryService.open(ACE_INFO_DIRECTORY)) {
                    for (ACE ace : diff.removedACEs) {
                        String id = computeDirectoryId(doc, diff.aclName, ace.getId());
                        session.deleteEntry(id);
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

                        if (notify && ace.isGranted() && ace.isEffective()) {
                            firePermissionNotificationEvent(docCtx, diff.aclName, ace);
                        }
                    }
                }
            }
        });
    }

    protected void handleReplaceACE(DocumentEventContext docCtx, String changedACLName, ACE oldACE, ACE newACE) {
      doAsSystemUser(() -> {
          DocumentModel doc = docCtx.getSourceDocument();

          DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
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
              Map<String, Object> m = PermissionHelper.createDirectoryEntry(doc, changedACLName, newACE, notify, comment);
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
