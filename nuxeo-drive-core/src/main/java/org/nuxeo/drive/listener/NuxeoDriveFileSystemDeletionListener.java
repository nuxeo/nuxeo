/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.drive.listener;

import static org.nuxeo.ecm.core.api.trash.TrashService.DOCUMENT_TRASHED;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.NuxeoDriveContribException;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveEvents;
import org.nuxeo.drive.service.impl.NuxeoDriveManagerImpl;
import org.nuxeo.ecm.core.api.CoreSession;
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
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;

/**
 * Synchronous event listener to track events that cannot be directly handled by the {@link FileSystemChangeFinder}
 * because the document bound to the event is either no more adaptable as a {@link FileSystemItem} after the transaction
 * has been committed (e.g. deletion) or not a descendant of a synchronization root (e.g. security update on any
 * document). In particular this includes:
 * <ul>
 * <li>Synchronization root unregistration (user specific).</li>
 * <li>Simple document or synchronization root lifecycle change to the 'deleted' state.</li>
 * <li>Simple document or synchronization root physical removal from the directory.</li>
 * <li>Update of a document after which it has no blob.</li>
 * <li>Move of a document to a non synchronized folder.</li>
 * <li>Security update.</li>
 * <li>Group change.</li>
 * </ul>
 * <p>
 * The listener injects virtual entries in the audit logs with the {@link NuxeoDriveEvents#EVENT_CATEGORY} category to
 * be handled by the {@link FileSystemChangeFinder}. These entries are set in the context of a
 * {@link NuxeoDriveEvents#VIRTUAL_EVENT_CREATED} event handled by the post-commit asynchronous
 * {@link NuxeoDriveVirtualEventLogger} to ensure that the transaction is committed before the log entries are actually
 * added.
 */
public class NuxeoDriveFileSystemDeletionListener implements EventListener {

    private static final Logger log = LogManager.getLogger(NuxeoDriveFileSystemDeletionListener.class);

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
        if (!DOCUMENT_TRASHED.equals(event.getName())) {
            // Fallback on the transition event check
            if (LifeCycleConstants.TRANSITION_EVENT.equals(event.getName()) && !handleLifeCycleTransition(ctx)) {
                return;
            }
        }
        if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName()) && !handleAboutToRemove(doc)) {
            return;
        }
        log.debug("NuxeoDriveFileSystemDeletionListener handling {} event for {}", event::getName, () -> doc);
        // Virtual event name
        String virtualEventName;
        if (DocumentEventTypes.BEFORE_DOC_SECU_UPDATE.equals(event.getName())
                || NuxeoDriveEvents.GROUP_UPDATED.equals(event.getName())) {
            virtualEventName = NuxeoDriveEvents.SECURITY_UPDATED_EVENT;
        } else if (DocumentEventTypes.ABOUT_TO_MOVE.equals(event.getName())) {
            virtualEventName = NuxeoDriveEvents.MOVED_EVENT;
        } else {
            virtualEventName = NuxeoDriveEvents.DELETED_EVENT;
        }
        // Some events will only impact a specific user (e.g. root
        // unregistration)
        String impactedUserName = (String) ctx.getProperty(NuxeoDriveEvents.IMPACTED_USERNAME_PROPERTY);
        fireVirtualEventLogEntries(docForLogEntry, virtualEventName, ctx.getPrincipal(), impactedUserName,
                ctx.getCoreSession());
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
        // Document deletion of document that are already in the trash should not be marked as FS deletion to avoid
        // duplicates
        return !doc.isTrashed();
    }

    protected void fireVirtualEventLogEntries(DocumentModel doc, String eventName, Principal principal,
            String impactedUserName, CoreSession session) {

        if (Framework.getService(AuditLogger.class) == null) {
            // The log is not deployed (probably in unittest)
            return;
        }

        List<LogEntry> entries = new ArrayList<>();
        // XXX: shall we use the server local for the event date or UTC?
        Date currentDate = Calendar.getInstance(NuxeoDriveManagerImpl.UTC).getTime();
        FileSystemItem fsItem = getFileSystemItem(doc, eventName);
        if (fsItem == null) {
            // NXP-21373: Let's check if we need to propagate the securityUpdated virtual event to child synchronization
            // roots in order to make Drive add / remove them if needed
            if (NuxeoDriveEvents.SECURITY_UPDATED_EVENT.equals(eventName)) {
                for (DocumentModel childSyncRoot : getChildSyncRoots(doc, session)) {
                    FileSystemItem childSyncRootFSItem = getFileSystemItem(childSyncRoot, eventName);
                    if (childSyncRootFSItem != null) {
                        entries.add(computeLogEntry(eventName, currentDate, childSyncRoot.getId(),
                                childSyncRoot.getPathAsString(), principal.getName(), childSyncRoot.getType(),
                                childSyncRoot.getRepositoryName(), childSyncRoot.getCurrentLifeCycleState(),
                                impactedUserName, childSyncRootFSItem));
                    }
                }
            }
        } else {
            entries.add(computeLogEntry(eventName, currentDate, doc.getId(), doc.getPathAsString(), principal.getName(),
                    doc.getType(), doc.getRepositoryName(), doc.getCurrentLifeCycleState(), impactedUserName, fsItem));
        }

        if (!entries.isEmpty()) {
            EventContext eventContext = new EventContextImpl(entries.toArray());
            Event event = eventContext.newEvent(NuxeoDriveEvents.VIRTUAL_EVENT_CREATED);
            Framework.getService(EventProducer.class).fireEvent(event);
        }
    }

    protected FileSystemItem getFileSystemItem(DocumentModel doc, String eventName) {
        try {
            // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
            return Framework.getService(FileSystemItemAdapterService.class).getFileSystemItem(doc, true, true, false);
        } catch (RootlessItemException e) {
            // can happen when deleting a folder under and unregistered root:
            // nothing to do
            return null;
        } catch (NuxeoDriveContribException e) {
            // Nuxeo Drive contributions missing or component not ready
            log.debug(
                    "Either Nuxeo Drive contributions are missing or the FileSystemItemAdapterService component is not ready (application has nor started yet) => ignoring event '{}'.",
                    eventName);
            return null;
        }
    }

    protected List<DocumentModel> getChildSyncRoots(DocumentModel doc, CoreSession session) {
        String nxql = "SELECT * FROM Document WHERE ecm:mixinType = '" + NuxeoDriveManagerImpl.NUXEO_DRIVE_FACET
                + "' AND ecm:isTrashed = 0 AND ecm:isVersion = 0 AND ecm:path STARTSWITH "
                + NXQL.escapeString(doc.getPathAsString());
        return session.query(nxql);
    }

    protected LogEntry computeLogEntry(String eventName, Date eventDate, String docId, String docPath, String principal,
            String docType, String repositoryName, String currentLifeCycleState, String impactedUserName,
            FileSystemItem fsItem) {

        AuditLogger logger = Framework.getService(AuditLogger.class);
        LogEntry entry = logger.newLogEntry();
        entry.setEventId(eventName);
        entry.setEventDate(eventDate);
        entry.setCategory(NuxeoDriveEvents.EVENT_CATEGORY);
        entry.setDocUUID(docId);
        entry.setDocPath(docPath);
        entry.setPrincipalName(principal);
        entry.setDocType(docType);
        entry.setRepositoryId(repositoryName);
        entry.setDocLifeCycle(currentLifeCycleState);

        Map<String, ExtendedInfo> extendedInfos = new HashMap<String, ExtendedInfo>();
        if (impactedUserName != null) {
            extendedInfos.put("impactedUserName", logger.newExtendedInfo(impactedUserName));
        }
        // We do not serialize the whole object as it's too big to fit in a
        // StringInfo column
        extendedInfos.put("fileSystemItemId", logger.newExtendedInfo(fsItem.getId()));
        extendedInfos.put("fileSystemItemName", logger.newExtendedInfo(fsItem.getName()));
        entry.setExtendedInfos(extendedInfos);

        return entry;
    }

}
