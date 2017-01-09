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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.NuxeoDriveContribException;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveEvents;
import org.nuxeo.drive.service.impl.NuxeoDriveManagerImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;

/**
 * Event listener to track events that should be mapped to file system item deletions in the the ChangeSummary
 * computation. In particular this includes
 * <ul>
 * <li>Synchronization root unregistration (user specific)</li>
 * <li>Simple document or root document lifecycle change to the 'deleted' state</li>
 * <li>Simple document or root physical removal from the directory.</li>
 * </ul>
 */
public class NuxeoDriveFileSystemDeletionListener implements EventListener {

    private static final Log log = LogFactory.getLog(NuxeoDriveFileSystemDeletionListener.class);

    @Override
    public void handleEvent(Event event) {
        DocumentEventContext ctx;
        if (event.getContext() instanceof DocumentEventContext) {
            ctx = (DocumentEventContext) event.getContext();
        } else {
            // Not interested in events that are not related to documents
            return;
        }
        DocumentModel doc = ctx.getSourceDocument();
        if (doc.hasFacet(FacetNames.SYSTEM_DOCUMENT)) {
            // Not interested in system documents
            return;
        }
        DocumentModel docForLogEntry = doc;
        if (DocumentEventTypes.BEFORE_DOC_UPDATE.equals(event.getName())) {
            docForLogEntry = handleBeforeDocUpdate(ctx, doc);
            if (docForLogEntry == null) {
                return;
            }
        }
        if (LifeCycleConstants.TRANSITION_EVENT.equals(event.getName()) && !handleLifeCycleTransition(ctx)) {
            return;
        }
        if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName()) && !handleAboutToRemove(doc)) {
            return;
        }
        // Virtual event name
        String virtualEventName;
        if (DocumentEventTypes.BEFORE_DOC_SECU_UPDATE.equals(event.getName())) {
            virtualEventName = NuxeoDriveEvents.SECURITY_UPDATED_EVENT;
        } else if (DocumentEventTypes.ABOUT_TO_MOVE.equals(event.getName())) {
            virtualEventName = NuxeoDriveEvents.MOVED_EVENT;
        } else {
            virtualEventName = NuxeoDriveEvents.DELETED_EVENT;
        }
        // Some events will only impact a specific user (e.g. root
        // unregistration)
        String impactedUserName = (String) ctx.getProperty(NuxeoDriveEvents.IMPACTED_USERNAME_PROPERTY);
        fireVirtualEventLogEntry(docForLogEntry, virtualEventName, ctx.getPrincipal(), impactedUserName);
    }

    protected DocumentModel handleBeforeDocUpdate(DocumentEventContext ctx, DocumentModel doc) {
        // Interested in update of a BlobHolder whose blob has been removed
        boolean blobRemoved = false;
        DocumentModel previousDoc = (DocumentModel) ctx.getProperty(CoreEventConstants.PREVIOUS_DOCUMENT_MODEL);
        if (previousDoc != null) {
            BlobHolder previousBh = previousDoc.getAdapter(BlobHolder.class);
            if (previousBh != null) {
                BlobHolder bh = doc.getAdapter(BlobHolder.class);
                if (bh != null) {
                    blobRemoved = previousBh.getBlob() != null && bh.getBlob() == null;
                }
            }
        }
        if (blobRemoved) {
            // Use previous doc holding a Blob for it to be adaptable as a
            // FileSystemItem
            return previousDoc;
        } else {
            return null;
        }
    }

    protected boolean handleLifeCycleTransition(DocumentEventContext ctx) {
        String transition = (String) ctx.getProperty(LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION);
        // Interested in 'deleted' life cycle transition only
        return transition != null && LifeCycleConstants.DELETE_TRANSITION.equals(transition);

    }

    protected boolean handleAboutToRemove(DocumentModel doc) {
        // Document deletion of document that are already in deleted
        // state should not be marked as FS deletion to avoid duplicates
        return !LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState());
    }

    protected void fireVirtualEventLogEntry(DocumentModel doc, String eventName, Principal principal,
            String impactedUserName) {

        AuditLogger logger = Framework.getLocalService(AuditLogger.class);
        if (logger == null) {
            // The log is not deployed (probably in unittest)
            return;
        }
        FileSystemItem fsItem = null;
        try {
            // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
            fsItem = Framework.getLocalService(FileSystemItemAdapterService.class).getFileSystemItem(doc, true, true,
                    false);
        } catch (RootlessItemException e) {
            // can happen when deleting a folder under and unregistered root:
            // nothing to do
            return;
        } catch (NuxeoDriveContribException e) {
            // Nuxeo Drive contributions missing or component not ready
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Either Nuxeo Drive contributions are missing or the FileSystemItemAdapterService component is not ready (application has nor started yet) => ignoring event '%s'.",
                        eventName));
            }
            return;
        }
        if (fsItem == null) {
            return;
        }

        LogEntry entry = logger.newLogEntry();
        entry.setEventId(eventName);
        // XXX: shall we use the server local for the event date or UTC?
        entry.setEventDate(Calendar.getInstance(NuxeoDriveManagerImpl.UTC).getTime());
        entry.setCategory((String) NuxeoDriveEvents.EVENT_CATEGORY);
        entry.setDocUUID(doc.getId());
        entry.setDocPath(doc.getPathAsString());
        entry.setPrincipalName(principal.getName());
        entry.setDocType(doc.getType());
        entry.setRepositoryId(doc.getRepositoryName());
        entry.setDocLifeCycle(doc.getCurrentLifeCycleState());

        Map<String, ExtendedInfo> extendedInfos = new HashMap<String, ExtendedInfo>();
        if (impactedUserName != null) {
            extendedInfos.put("impactedUserName", logger.newExtendedInfo(impactedUserName));
        }
        // We do not serialize the whole object as it's too big to fit in a
        // StringInfo column
        extendedInfos.put("fileSystemItemId", logger.newExtendedInfo(fsItem.getId()));
        extendedInfos.put("fileSystemItemName", logger.newExtendedInfo(fsItem.getName()));
        entry.setExtendedInfos(extendedInfos);

        EventContext eventContext = new EventContextImpl(entry);
        Event event = eventContext.newEvent(NuxeoDriveEvents.VIRTUAL_EVENT_CREATED);
        Framework.getService(EventProducer.class).fireEvent(event);
    }

}
