/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.drive.listener;

import java.security.Principal;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.NuxeoDriveEvents;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;

/**
 * Event listener to track events that should be mapped to file system item
 * deletions in the the ChangeSummary computation.
 *
 * In particular this includes
 *
 * <li>Synchronization root unregistration (user specific)</li>
 *
 * <li>Simple document or root document lifecycle change to the 'deleted' state</li>
 *
 * <li>Simple document or root physical removal from the directory.</li>
 */
public class NuxeoDriveFileSystemDeletionListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        DocumentEventContext ctx;
        if (event.getContext() instanceof DocumentEventContext) {
            ctx = (DocumentEventContext) event.getContext();
        } else {
            // not interested in event that are not related to documents
            return;
        }
        DocumentModel doc = ctx.getSourceDocument();
        if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())
                && LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
            // Document deletion of document that are already in deleted state
            // should not be marked as FS deletion to avoid duplicates
            return;
        }
        String transition = (String) ctx.getProperty(LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION);
        if (transition != null
                && !LifeCycleConstants.DELETE_TRANSITION.equals(transition)) {
            // not interested in lifecycle transitions that are not related to
            // document deletion
            return;
        }
        // Some events will only impact a specific user (e.g. root
        // unregistration)
        String impactedUserName = (String) ctx.getProperty(NuxeoDriveEvents.IMPACTED_USERNAME_PROPERTY);
        try {
            logDeletionEvent(doc, ctx.getPrincipal(), impactedUserName);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    protected void logDeletionEvent(DocumentModel doc, Principal principal,
            String impactedUserName) throws ClientException {

        AuditLogger logger = Framework.getLocalService(AuditLogger.class);
        if (logger == null) {
            // The log is not deployed (probably in unittest
            return;
        }
        FileSystemItem fsItem = doc.getAdapter(FileSystemItem.class);
        if (fsItem == null) {
            return;
        }

        LogEntry entry = logger.newLogEntry();
        entry.setEventId(NuxeoDriveEvents.DELETED_EVENT);
        // XXX: shall we use the server local for the event date or UTC?
        entry.setEventDate(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());
        entry.setCategory((String) NuxeoDriveEvents.EVENT_CATEGORY);
        entry.setDocUUID(doc.getId());
        entry.setDocPath(doc.getPathAsString());
        entry.setPrincipalName(principal.getName());
        entry.setDocType(doc.getType());
        entry.setRepositoryId(doc.getRepositoryName());
        entry.setDocLifeCycle(doc.getCurrentLifeCycleState());

        Map<String, ExtendedInfo> extendedInfos = new HashMap<String, ExtendedInfo>();
        if (impactedUserName != null) {
            extendedInfos.put("impactedUserName",
                    logger.newExtendedInfo(impactedUserName));
        }
        extendedInfos.put("fileSystemItem", logger.newExtendedInfo(fsItem));
        entry.setExtendedInfos(extendedInfos);
        logger.addLogEntries(Collections.singletonList(entry));
    }

}
