/*
 * (C) Copyright 2012-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.drive.service.impl;

import static org.nuxeo.audit.api.LogEntryConstants.LOG_CATEGORY;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_PATH;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_DATE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EXTENDED;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_LOG_DATE;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_REPOSITORY_ID;
import static org.nuxeo.audit.service.AuditBackend.Capability.EXTENDED_INFO_SEARCH;
import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;
import static org.nuxeo.drive.service.NuxeoDriveEvents.EVENT_CATEGORY;
import static org.nuxeo.drive.service.NuxeoDriveEvents.IMPACTED_USERNAME_PROPERTY;
import static org.nuxeo.ecm.core.query.sql.model.OrderByExprs.asc;
import static org.nuxeo.ecm.core.query.sql.model.OrderByExprs.desc;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.and;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.eq;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.gt;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.in;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.isnull;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.lte;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.noteq;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.or;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.startsWith;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.audit.service.AuditService;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemChange;
import org.nuxeo.drive.service.NuxeoDriveEvents;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.drive.service.TooManyChangesException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of {@link FileSystemChangeFinder} using the {@link AuditBackend}.
 *
 * @author Antoine Taillefer
 * @since 2025.0
 * @implNote before 2025.0 this finder only handles SQL audit, now it can work with any implementation
 */
public class AuditChangeFinder implements FileSystemChangeFinder {

    private static final Logger log = LogManager.getLogger(AuditChangeFinder.class);

    protected Map<String, String> parameters = new HashMap<>();

    @Override
    public void handleParameters(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    @Override
    public List<FileSystemItemChange> getFileSystemChanges(CoreSession session, Set<IdRef> lastActiveRootRefs,
            SynchronizationRoots activeRoots, Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound,
            int limit) {
        String principalName = session.getPrincipal().getName();
        List<FileSystemItemChange> changes = new ArrayList<>();

        // Note: lastActiveRootRefs is not used: we could remove it from the
        // public API
        // and from the client as well but it might be useful to optimize future
        // alternative implementations FileSystemChangeFinder component so it
        // might
        // be better to leave it part of the public API as currently.

        // Find changes from the log under active roots or events that are
        // linked to the un-registration or deletion of formerly synchronized
        // roots
        List<LogEntry> entries = queryAuditEntries(session, activeRoots, collectionSyncRootMemberIds, lowerBound,
                upperBound, limit);

        // First pass over the entries to check if a "NuxeoDrive" event has
        // occurred during that period.
        // This event can be:
        // - a root registration
        // - a root unregistration
        // - a document trashing
        // - a document untrashing
        // - a removal
        // - a move to an non synchronization root
        // - a security update
        // Thus the list of active roots may have changed and the cache might
        // need to be invalidated: let's make sure we perform a
        // query with the actual active roots.
        for (LogEntry entry : entries) {
            if (EVENT_CATEGORY.equals(entry.getCategory())) {
                log.debug("Detected sync root change for user '{}' in audit log:"
                        + " invalidating the root cache and refetching the changes.", principalName);
                NuxeoDriveManager driveManager = Framework.getService(NuxeoDriveManager.class);
                driveManager.invalidateSynchronizationRootsCache(principalName);
                driveManager.invalidateCollectionSyncRootMemberCache(principalName);
                Map<String, SynchronizationRoots> synchronizationRoots = driveManager.getSynchronizationRoots(
                        session.getPrincipal());
                SynchronizationRoots updatedActiveRoots = synchronizationRoots.get(session.getRepositoryName());
                Set<String> updatedCollectionSyncRootMemberIds = driveManager.getCollectionSyncRootMemberIds(
                        session.getPrincipal()).get(session.getRepositoryName());
                entries = queryAuditEntries(session, updatedActiveRoots, updatedCollectionSyncRootMemberIds, lowerBound,
                        upperBound, limit);
                break;
            }
        }

        if (entries.size() >= limit) {
            throw new TooManyChangesException("Too many changes found in the audit logs.");
        }
        for (LogEntry entry : entries) {
            log.debug("Handling log entry {}", entry);
            FileSystemItemChange change = null;
            DocumentRef docRef = new IdRef(entry.getDocUUID());
            String fsIdInfo = entry.getExtendedValue("fileSystemItemId");
            if (fsIdInfo != null) {
                // This document has been deleted, moved, is an unregistered synchronization root or its security has
                // been updated, we just know the FileSystemItem id and name.
                log.debug("Found extended info in audit log entry: document has been deleted, moved,"
                        + " is an unregistered synchronization root or its security has been updated,"
                        + " we just know the FileSystemItem id and name.");
                // Ignore trashed documents to which the current user doesn't have access
                if ("deleted".equals(entry.getEventId()) && !session.exists(docRef)) {
                    log.debug(
                            "Document {} ({}) doesn't exist, it has been removed or the current user doesn't have access to it, not adding entry to the change summary.",
                            entry::getDocPath, () -> docRef);
                    continue;
                }
                boolean isChangeSet = false;
                // First try to adapt the document as a FileSystemItem to provide it to the FileSystemItemChange entry,
                // only in the case of a move or a security update.
                // This can succeed if this is a move to a synchronization root or a security update after which the
                // current user still has access to the document.
                if (!"deleted".equals(entry.getEventId()) && session.exists(docRef)) {
                    change = getFileSystemItemChange(session, docRef, entry, fsIdInfo);
                    if (change != null) {
                        if (NuxeoDriveEvents.MOVED_EVENT.equals(entry.getEventId())) {
                            // A move to a synchronization root also fires a documentMoved event, don't propagate the
                            // virtual event.
                            log.debug(
                                    "Document {} ({}) has been moved to another synchronzation root, not adding entry to the change summary.",
                                    entry::getDocPath, () -> docRef);
                            continue;
                        }
                        isChangeSet = true;
                    }
                }
                if (!isChangeSet) {
                    // If the document has been deleted, is a regular unregistered synchronization root, has been moved
                    // to a non synchronization root, if its security has been updated denying access to the current
                    // user, or if it is not adaptable as a FileSystemItem for any other reason only provide the
                    // FileSystemItem id and name to the FileSystemItemChange entry.
                    log.debug(
                            "Document {} ({}) doesn't exist or is not adaptable as a FileSystemItem, only providing the FileSystemItem id and name to the FileSystemItemChange entry.",
                            entry::getDocPath, () -> docRef);
                    String eventId;
                    if (NuxeoDriveEvents.MOVED_EVENT.equals(entry.getEventId())
                            || NuxeoDriveEvents.ABOUT_TO_REMOVE_EVENT.equals(entry.getEventId())) {
                        // Move to a non synchronization root or an unauthorized folder, or permanent removal
                        eventId = NuxeoDriveEvents.DELETED_EVENT;
                    } else {
                        // Deletion, unregistration or security update
                        eventId = entry.getEventId();
                    }
                    change = new FileSystemItemChangeImpl(eventId, entry.getEventDate().getTime(),
                            entry.getRepositoryId(), entry.getDocUUID(), fsIdInfo, null);
                }
                log.debug("Adding FileSystemItemChange entry to the change summary: {}", change);
                changes.add(change);
            } else {
                // No extended info in the audit log entry, this should not be a deleted document, a moved document, an
                // unregistered synchronization root nor a security update denying access to the current user.
                log.debug(
                        "No extended info found in audit log entry {} ({}): this is not a deleted document, a moved document,"
                                + " an unregistered synchronization root nor a security update denying access to the current user.",
                        entry::getDocPath, () -> docRef);
                if (!session.exists(docRef)) {
                    log.debug("Document {} ({}) doesn't exist, not adding entry to the change summary.",
                            entry::getDocPath, () -> docRef);
                    // Deleted or non accessible documents are mapped to
                    // deleted file system items in a separate event: no need to
                    // try to propagate this event.
                    continue;
                }
                // Let's try to adapt the document as a FileSystemItem to
                // provide it to the FileSystemItemChange entry.
                change = getFileSystemItemChange(session, docRef, entry, null);
                if (change == null) {
                    // Non-adaptable documents are ignored
                    log.debug(
                            "Document {} ({}) is not adaptable as a FileSystemItem, not adding any entry to the change summary.",
                            entry::getDocPath, () -> docRef);
                } else {
                    if (DocumentEventTypes.BLOB_DIGEST_UPDATED.equals(entry.getEventId())) {
                        String oldDigest = entry.getExtendedValue(CoreEventConstants.BLOB_DIGEST_UPDATED_OLD_DIGEST);
                        if (oldDigest != null) {
                            FileSystemItem fsItem = change.getFileSystemItem();
                            if (fsItem instanceof DocumentBackedFileItem dbfi) {
                                dbfi.setOldDigest(oldDigest);
                            }
                        }
                    }
                    log.debug("Adding FileSystemItemChange entry to the change summary: {}", change);
                    changes.add(change);
                }
            }
        }
        return changes;
    }

    /**
     * Returns the last available log id in the audit log table (primary key) to be used as the upper bound of the event
     * log id range clause in the change query.
     * <p>
     * To avoid an expensive full-index sort on backends like OpenSearch, the lookup is first attempted with a
     * {@code logDate} lower bound that is widened on each iteration (1, 2, 4, 8, 16, 32 days). The lower bound is
     * truncated to the day so that concurrent calls share the same query and benefit from the backend cache. If no
     * entry is found within the last 32 days, the method falls back to an unbounded query.
     */
    @Override
    public long getUpperBound() {
        var auditBackend = Framework.getService(AuditService.class).getAuditBackend(DEFAULT_AUDIT_BACKEND);
        var today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
        // scroll on previous days with a times 2 step up to 32
        for (int i = 1; i <= 32; i = i * 2) {
            var lowerLogDate = today.minusDays(i);
            var queryBuilder = new AuditQueryBuilder().predicate(gt(LOG_LOG_DATE, lowerLogDate))
                                                      .order(desc(LOG_ID))
                                                      .limit(1);
            log.debug("Querying audit log for greatest id with logDate > {}", lowerLogDate);
            List<LogEntry> entries = auditBackend.queryLogs(queryBuilder);
            if (!entries.isEmpty()) {
                return entries.getFirst().getId();
            }
        }
        // fallback to an unbounded query in case the most recent entry is older than 32 days
        log.debug("Found no audit log entries within the last 32 days, falling back to an unbounded query");
        var queryBuilder = new AuditQueryBuilder().order(desc(LOG_ID)).limit(1);
        List<LogEntry> entries = auditBackend.queryLogs(queryBuilder);
        if (entries.isEmpty()) {
            log.debug("Found no audit log entries, returning -1");
            return -1;
        }
        return entries.getFirst().getId();
    }

    @SuppressWarnings("unchecked")
    protected List<LogEntry> queryAuditEntries(CoreSession session, SynchronizationRoots activeRoots,
            Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound, int limit) {
        // build the audit query
        var isPlatformEventsList = new ArrayList<Predicate>();
        isPlatformEventsList.add(eq(LOG_CATEGORY, "eventDocumentCategory"));
        isPlatformEventsList.add(in(LOG_EVENT_ID, "documentCreated", "documentModified", "documentMoved",
                "documentCreatedByCopy", "documentRestored", "addedToCollection", "documentProxyPublished",
                "documentLocked", "documentUnlocked", "documentUntrashed", "blobDigestUpdated"));
        // handle active roots & collection sync root members
        var isRootList = activeRoots.getPaths()
                                    .stream()
                                    .map(path -> startsWith(LOG_DOC_PATH, path))
                                    .collect(Collectors.toCollection(ArrayList::new));
        if (!collectionSyncRootMemberIds.isEmpty()) {
            isRootList.add(in(LOG_DOC_UUID, collectionSyncRootMemberIds));
        }
        if (!isRootList.isEmpty()) {
            isPlatformEventsList.add(or(isRootList));
        }
        var isPlatformEvents = and(isPlatformEventsList);
        var isDriveEvents = and(eq(LOG_CATEGORY, EVENT_CATEGORY), noteq(LOG_EVENT_ID, "rootUnregistered"));
        var queryBuilder = new AuditQueryBuilder().predicate(eq(LOG_REPOSITORY_ID, session.getRepositoryName()))
                                                  // interesting events
                                                  .and(or(isPlatformEvents, isDriveEvents))
                                                  // id range
                                                  .and(and(gt(LOG_ID, lowerBound), lte(LOG_ID, upperBound)))
                                                  .order(asc(LOG_REPOSITORY_ID))
                                                  .order(desc(LOG_EVENT_DATE))
                                                  .limit(limit);
        String principalName = session.getPrincipal().getName();
        var auditBackend = Framework.getService(AuditService.class).getAuditBackend(DEFAULT_AUDIT_BACKEND);
        if (auditBackend.hasCapability(EXTENDED_INFO_SEARCH)) {
            // is current user
            queryBuilder.and(or(isnull(LOG_EXTENDED + '/' + IMPACTED_USERNAME_PROPERTY),
                    eq(LOG_EXTENDED + '/' + IMPACTED_USERNAME_PROPERTY, principalName)));
        }
        log.debug("Querying audit log for changes: {}", queryBuilder);
        var entries = auditBackend.queryLogs(queryBuilder);

        if (auditBackend.hasCapability(EXTENDED_INFO_SEARCH)) {
            entries.forEach(entry -> log.debug("Change detected: {}", entry));
            return entries;
        } else {
            // Post filter the output to remove (un)registration that are unrelated to the current user.
            return entries.stream()
                          .filter(entry -> entry.getExtendedValue(IMPACTED_USERNAME_PROPERTY) == null
                                  || principalName.equals(entry.getExtendedValue(IMPACTED_USERNAME_PROPERTY)))
                          .peek(entry -> log.debug("Change detected: {}", entry))
                          .toList();
        }
    }

    protected FileSystemItemChange getFileSystemItemChange(CoreSession session, DocumentRef docRef, LogEntry entry,
            String expectedFileSystemItemId) {
        DocumentModel doc = session.getDocument(docRef);
        // TODO: check the facet, last root change and list of roots
        // to have a special handling for the roots.
        FileSystemItem fsItem = null;
        try {
            // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
            fsItem = Framework.getService(FileSystemItemAdapterService.class)
                              .getFileSystemItem(doc, false, false, false);
        } catch (RootlessItemException e) {
            // Can happen for an unregistered synchronization root that cannot
            // be adapted as a FileSystemItem: nothing to do.
            log.debug("RootlessItemException thrown while trying to adapt document {} ({}) as a FileSystemItem.",
                    entry::getDocPath, () -> docRef);
        }
        if (fsItem == null) {
            log.debug("Document {} ({}) is not adaptable as a FileSystemItem, returning null.", entry::getDocPath,
                    () -> docRef);
            return null;
        }
        if (expectedFileSystemItemId != null
                && !fsItem.getId()
                          .endsWith(AbstractFileSystemItem.FILE_SYSTEM_ITEM_ID_SEPARATOR + expectedFileSystemItemId)) {
            log.debug(
                    "Id {} of FileSystemItem adapted from document {} ({}) doesn't match expected FileSystemItem id {}, returning null.",
                    fsItem::getId, entry::getDocPath, () -> docRef, () -> expectedFileSystemItemId);
            return null;
        }
        log.debug("Document {} ({}) is adaptable as a FileSystemItem, providing it to the FileSystemItemChange entry.",
                entry::getDocPath, () -> docRef);
        // EventDate is able to reflect the ordering of the events
        // inside a transaction (e.g. when several documents are
        // created, updated, deleted at once) hence it's useful
        // to pass that info to the client even though the change
        // detection filtering is using the log id to have a
        // guaranteed monotonic behavior that evenDate cannot
        // guarantee when facing long transactions.
        return new FileSystemItemChangeImpl(entry.getEventId(), entry.getEventDate().getTime(), entry.getRepositoryId(),
                entry.getDocUUID(), fsItem);
    }
}
