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

import static org.nuxeo.ecm.core.api.event.CoreEventConstants.NEW_ACP;
import static org.nuxeo.ecm.core.api.event.CoreEventConstants.OLD_ACP;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_SECURITY_UPDATED;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DIRECTORY;
import static org.nuxeo.ecm.permissions.Constants.ACE_KEY;
import static org.nuxeo.ecm.permissions.Constants.NOTIFY_KEY;
import static org.nuxeo.ecm.permissions.Constants.PERMISSION_NOTIFICATION_EVENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
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
    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        if (DOCUMENT_SECURITY_UPDATED.equals(event.getName())) {
            updateDirectory((DocumentEventContext) ctx);
        }
    }

    protected void updateDirectory(DocumentEventContext docCtx) {
        DocumentModel doc = docCtx.getSourceDocument();

        ACP oldACP = (ACP) docCtx.getProperty(OLD_ACP);
        ACP newACP = (ACP) docCtx.getProperty(NEW_ACP);
        List<ACLDiff> aclDiffs = extractACLDiffs(oldACP, newACP);

        DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
        for (ACLDiff diff : aclDiffs) {
            Session session = null;
            try {
                session = directoryService.open(ACE_INFO_DIRECTORY);
                for (ACE ace : diff.removedACEs) {
                    String id = computeDirectoryId(doc, diff.aclName, ace.getId());
                    session.deleteEntry(id);
                }

                for (ACE ace : diff.addedACEs) {
                    String id = computeDirectoryId(doc, diff.aclName, ace.getId());
                    Map<String, Object> m = new HashMap<>();
                    m.put("aceinfo:id", id);
                    m.put("aceinfo:repositoryName", doc.getRepositoryName());
                    m.put("aceinfo:docId", doc.getId());
                    m.put("aceinfo:aclName", diff.aclName);
                    m.put("aceinfo:aceId", ace.getId());
                    Boolean notify = (Boolean) ace.getContextData(NOTIFY_KEY);
                    m.put("aceinfo:notify", notify != null ? notify : false);
                    m.put("aceinfo:comment", ace.getContextData(Constants.COMMENT_KEY));
                    session.createEntry(m);

                    sendNotification(docCtx, ace);
                }
            } finally {
                if (session != null) {
                    session.close();
                }
            }
        }
    }

    protected String computeDirectoryId(DocumentModel doc, String aclName, String aceId) {
        return String.format("%s:%s:%s:%s", doc.getId(), doc.getRepositoryName(), aclName, aceId);
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

    protected void sendNotification(DocumentEventContext docCtx, ACE ace) {
        Boolean notify = (Boolean) ace.getContextData(NOTIFY_KEY);
        if (notify != null && notify && ace.isGranted()) {
            Date now = new Date();
            Date beginDate = ace.getBegin() == null ? null : ace.getBegin()
                                                                .getTime();
            if (beginDate == null || now.after(beginDate) || now.equals(beginDate)) {
                docCtx.setProperty(ACE_KEY, ace);
                Event event = docCtx.newEvent(PERMISSION_NOTIFICATION_EVENT);
                EventService eventService = Framework.getService(EventService.class);
                eventService.fireEvent(event);
            }
        }
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
