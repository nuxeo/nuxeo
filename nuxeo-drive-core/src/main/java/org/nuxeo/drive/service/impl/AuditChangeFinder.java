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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
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
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of {@link FileSystemChangeFinder} using the {@link AuditReader}.
 *
 * @author Antoine Taillefer
 */
public class AuditChangeFinder implements FileSystemChangeFinder {

    private static final long serialVersionUID = 1963018967324857522L;

    private static final Log log = LogFactory.getLog(AuditChangeFinder.class);

    protected Map<String, String> parameters = new HashMap<String, String>();

    @Override
    public void handleParameters(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    @Override
    public List<FileSystemItemChange> getFileSystemChanges(CoreSession session, Set<IdRef> lastActiveRootRefs,
            SynchronizationRoots activeRoots, Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound,
            int limit) throws TooManyChangesException {
        String principalName = session.getPrincipal().getName();
        List<FileSystemItemChange> changes = new ArrayList<FileSystemItemChange>();

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
        // - a "deleted" transition
        // - an "undeleted" transition
        // - a removal
        // - a move to an non synchronization root
        // - a security update
        // Thus the list of active roots may have changed and the cache might
        // need to be invalidated: let's make sure we perform a
        // query with the actual active roots.
        for (LogEntry entry : entries) {
            if (NuxeoDriveEvents.EVENT_CATEGORY.equals(entry.getCategory())) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Detected sync root change for user '%s' in audit log:"
                            + " invalidating the root cache and refetching the changes.", principalName));
                }
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
            if (log.isDebugEnabled()) {
                log.debug(String.format("Handling log entry %s", entry));
            }
            FileSystemItemChange change = null;
            DocumentRef docRef = new IdRef(entry.getDocUUID());
            ExtendedInfo fsIdInfo = entry.getExtendedInfos().get("fileSystemItemId");
            if (fsIdInfo != null) {
                // This document has been deleted, moved, is an unregistered synchronization root or its security has
                // been updated, we just know the FileSystemItem id and name.
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Found extended info in audit log entry: document has been deleted, moved,"
                            + " is an unregistered synchronization root or its security has been updated,"
                            + " we just know the FileSystemItem id and name."));
                }
                boolean isChangeSet = false;
                // First try to adapt the document as a FileSystemItem to provide it to the FileSystemItemChange entry,
                // only in the case of a move or a security update.
                // This can succeed if this is a move to a synchronization root or a security update after which the
                // current user still has access to the document.
                if (!"deleted".equals(entry.getEventId()) && session.exists(docRef)) {
                    change = getFileSystemItemChange(session, docRef, entry, fsIdInfo.getValue(String.class));
                    if (change != null) {
                        if (NuxeoDriveEvents.MOVED_EVENT.equals(entry.getEventId())) {
                            // A move to a synchronization root also fires a documentMoved event, don't propagate the
                            // virtual event.
                            if (log.isDebugEnabled()) {
                                log.debug(String.format(
                                        "Document %s (%s) has been moved to another synchronzation root, not adding entry to the change summary.",
                                        entry.getDocPath(), docRef));
                            }
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
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Document %s (%s) doesn't exist or is not adaptable as a FileSystemItem, only providing the FileSystemItem id and name to the FileSystemItemChange entry.",
                                entry.getDocPath(), docRef));
                    }
                    String fsId = fsIdInfo.getValue(String.class);
                    String eventId;
                    if (NuxeoDriveEvents.MOVED_EVENT.equals(entry.getEventId())) {
                        // Move to a non synchronization root
                        eventId = NuxeoDriveEvents.DELETED_EVENT;
                    } else {
                        // Deletion, unregistration or security update
                        eventId = entry.getEventId();
                    }
                    change = new FileSystemItemChangeImpl(eventId, entry.getEventDate().getTime(),
                            entry.getRepositoryId(), entry.getDocUUID(), fsId, null);
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Adding FileSystemItemChange entry to the change summary: %s", change));
                }
                changes.add(change);
            } else {
                // No extended info in the audit log entry, this should not be a deleted document, a moved document, an
                // unregistered synchronization root nor a security update denying access to the current user.
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "No extended info found in audit log entry %s (%s): this is not a deleted document, a moved document,"
                                    + " an unregistered synchronization root nor a security update denying access to the current user.",
                            entry.getDocPath(), docRef));
                }
                if (!session.exists(docRef)) {
                    if (log.isDebugEnabled()) {
                        log.debug(
                                String.format("Document %s (%s) doesn't exist, not adding entry to the change summary.",
                                        entry.getDocPath(), docRef));
                    }
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
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Document %s (%s) is not adaptable as a FileSystemItem, not adding any entry to the change summary.",
                                entry.getDocPath(), docRef));
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Adding FileSystemItemChange entry to the change summary: %s", change));
                    }
                    changes.add(change);
                }
            }
        }
        return changes;
    }

    /**
     * Returns the last available log id in the audit log table (primary key) to be used as the upper bound of the event
     * log id range clause in the change query.
     */
    @Override
    @SuppressWarnings("unchecked")
    public long getUpperBound() {
        AuditReader auditService = Framework.getService(AuditReader.class);
        String auditQuery = "from LogEntry log order by log.id desc";
        if (log.isDebugEnabled()) {
            log.debug("Querying audit log for greatest id: " + auditQuery);
        }
        List<LogEntry> entries = (List<LogEntry>) auditService.nativeQuery(auditQuery, 1, 1);
        if (entries.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Found no audit log entries, returning -1");
            }
            return -1;
        }
        return entries.get(0).getId();
    }

    /**
     * Returns the last available log id in the audit log table (primary key) considering events older than the last
     * clustering invalidation date if clustering is enabled for at least one of the given repositories. This is to make
     * sure the {@code DocumentModel} further fetched from the session using the audit entry doc id is fresh.
     */
    @Override
    @SuppressWarnings("unchecked")
    public long getUpperBound(Set<String> repositoryNames) {
        long clusteringDelay = getClusteringDelay(repositoryNames);
        AuditReader auditService = Framework.getService(AuditReader.class);
        Map<String, Object> params = new HashMap<String, Object>();
        StringBuilder auditQuerySb = new StringBuilder("from LogEntry log");
        if (clusteringDelay > -1) {
            // Double the delay in case of overlapping, see https://jira.nuxeo.com/browse/NXP-14826
            long lastClusteringInvalidationDate = System.currentTimeMillis() - 2 * clusteringDelay;
            params.put("lastClusteringInvalidationDate", new Date(lastClusteringInvalidationDate));
            auditQuerySb.append(" where log.logDate < :lastClusteringInvalidationDate");
        }
        auditQuerySb.append(" order by log.id desc");
        String auditQuery = auditQuerySb.toString();
        if (log.isDebugEnabled()) {
            log.debug("Querying audit log for greatest id: " + auditQuery + " with params: " + params);
        }
        List<LogEntry> entries = (List<LogEntry>) auditService.nativeQuery(auditQuery, params, 1, 1);
        if (entries.isEmpty()) {
            if (clusteringDelay > -1) {
                // Check for existing entries without the clustering invalidation date filter to not return -1 in this
                // case and make sure the lower bound of the next call to NuxeoDriveManager#getChangeSummary will be >=
                // 0
                List<LogEntry> allEntries = (List<LogEntry>) auditService.nativeQuery("from LogEntry", 1, 1);
                if (!allEntries.isEmpty()) {
                    log.debug("Found no audit log entries matching the criterias but some exist, returning 0");
                    return 0;
                }
            }
            log.debug("Found no audit log entries, returning -1");
            return -1;
        }
        return entries.get(0).getId();
    }

    /**
     * Returns the longest clustering delay among the given repositories for which clustering is enabled.
     */
    protected long getClusteringDelay(Set<String> repositoryNames) {
        long clusteringDelay = -1;
        SQLRepositoryService repositoryService = Framework.getService(SQLRepositoryService.class);
        for (String repositoryName : repositoryNames) {
            RepositoryDescriptor repositoryDescriptor = repositoryService.getRepositoryDescriptor(repositoryName);
            if (repositoryDescriptor == null) {
                // Not a VCS repository`
                continue;
            }
            if (repositoryDescriptor.getClusteringEnabled()) {
                clusteringDelay = Math.max(clusteringDelay, repositoryDescriptor.getClusteringDelay());
            }
        }
        return clusteringDelay;
    }

    @SuppressWarnings("unchecked")
    protected List<LogEntry> queryAuditEntries(CoreSession session, SynchronizationRoots activeRoots,
            Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound, int limit) {
        AuditReader auditService = Framework.getService(AuditReader.class);
        // Set fixed query parameters
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("repositoryId", session.getRepositoryName());

        // Build query and set dynamic parameters
        StringBuilder auditQuerySb = new StringBuilder("from LogEntry log where ");
        auditQuerySb.append("log.repositoryId = :repositoryId");
        auditQuerySb.append(" and ");
        auditQuerySb.append("(");
        if (!activeRoots.getPaths().isEmpty()) {
            // detect changes under the currently active roots for the
            // current user
            auditQuerySb.append("(");
            auditQuerySb.append("log.category = 'eventDocumentCategory'");
            // TODO: don't hardcode event ids (contribute them?)
            auditQuerySb.append(
                    " and (log.eventId = 'documentCreated' or log.eventId = 'documentModified' or log.eventId = 'documentMoved' or log.eventId = 'documentCreatedByCopy' or log.eventId = 'documentRestored' or log.eventId = 'addedToCollection' or log.eventId = 'documentProxyPublished' or log.eventId = 'documentLocked' or log.eventId = 'documentUnlocked')");
            auditQuerySb.append(" or ");
            auditQuerySb.append("log.category = 'eventLifeCycleCategory'");
            auditQuerySb.append(" and log.eventId = 'lifecycle_transition_event' and log.docLifeCycle != 'deleted' ");
            auditQuerySb.append(") and (");
            auditQuerySb.append("(");
            auditQuerySb.append(getCurrentRootFilteringClause(activeRoots.getPaths(), params));
            auditQuerySb.append(")");
            if (collectionSyncRootMemberIds != null && !collectionSyncRootMemberIds.isEmpty()) {
                auditQuerySb.append(" or (");
                auditQuerySb.append(getCollectionSyncRootFilteringClause(collectionSyncRootMemberIds, params));
                auditQuerySb.append(")");
            }
            auditQuerySb.append(") or ");
        }
        // Detect any root (un-)registration changes for the roots previously
        // seen by the current user.
        // Exclude 'rootUnregistered' since root unregistration is covered by a
        // "deleted" virtual event.
        auditQuerySb.append("(");
        auditQuerySb.append("log.category = '");
        auditQuerySb.append(NuxeoDriveEvents.EVENT_CATEGORY);
        auditQuerySb.append("' and log.eventId != 'rootUnregistered'");
        auditQuerySb.append(")");
        auditQuerySb.append(") and (");
        auditQuerySb.append(getJPARangeClause(lowerBound, upperBound, params));
        // we intentionally sort by eventDate even if the range filtering is
        // done on the log id: eventDate is useful to reflect the ordering of
        // events occurring inside the same transaction while the
        // monotonic behavior of log id is useful for ensuring that consecutive
        // range queries to the audit won't miss any events even when long
        // running transactions are logged after a delay.
        auditQuerySb.append(") order by log.repositoryId asc, log.eventDate desc");
        String auditQuery = auditQuerySb.toString();

        if (log.isDebugEnabled()) {
            log.debug("Querying audit log for changes: " + auditQuery + " with params: " + params);
        }
        List<LogEntry> entries = (List<LogEntry>) auditService.nativeQuery(auditQuery, params, 1, limit);

        // Post filter the output to remove (un)registration that are unrelated
        // to the current user.
        List<LogEntry> postFilteredEntries = new ArrayList<LogEntry>();
        String principalName = session.getPrincipal().getName();
        for (LogEntry entry : entries) {
            ExtendedInfo impactedUserInfo = entry.getExtendedInfos().get("impactedUserName");
            if (impactedUserInfo != null && !principalName.equals(impactedUserInfo.getValue(String.class))) {
                // ignore event that only impact other users
                continue;
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Change detected: %s", entry));
            }
            postFilteredEntries.add(entry);
        }
        return postFilteredEntries;
    }

    protected String getCurrentRootFilteringClause(Set<String> rootPaths, Map<String, Object> params) {
        StringBuilder rootPathClause = new StringBuilder();
        int rootPathCount = 0;
        for (String rootPath : rootPaths) {
            rootPathCount++;
            String rootPathParam = "rootPath" + rootPathCount;
            if (rootPathClause.length() > 0) {
                rootPathClause.append(" or ");
            }
            rootPathClause.append(String.format("log.docPath like :%s", rootPathParam));
            params.put(rootPathParam, rootPath + '%');

        }
        return rootPathClause.toString();
    }

    protected String getCollectionSyncRootFilteringClause(Set<String> collectionSyncRootMemberIds,
            Map<String, Object> params) {
        String paramName = "collectionMemberIds";
        params.put(paramName, collectionSyncRootMemberIds);
        return String.format("log.docUUID in (:%s)", paramName);
    }

    /**
     * Using event log id to ensure consistency, see https://jira.nuxeo.com/browse/NXP-14826.
     */
    protected String getJPARangeClause(long lowerBound, long upperBound, Map<String, Object> params) {
        params.put("lowerBound", lowerBound);
        params.put("upperBound", upperBound);
        return "log.id > :lowerBound and log.id <= :upperBound";
    }

    protected FileSystemItemChange getFileSystemItemChange(CoreSession session, DocumentRef docRef, LogEntry entry,
            String expectedFileSystemItemId) {
        DocumentModel doc = session.getDocument(docRef);
        // TODO: check the facet, last root change and list of roots
        // to have a special handling for the roots.
        FileSystemItem fsItem = null;
        try {
            // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
            fsItem = Framework.getService(FileSystemItemAdapterService.class).getFileSystemItem(doc, false, false,
                    false);
        } catch (RootlessItemException e) {
            // Can happen for an unregistered synchronization root that cannot
            // be adapted as a FileSystemItem: nothing to do.
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "RootlessItemException thrown while trying to adapt document %s (%s) as a FileSystemItem.",
                        entry.getDocPath(), docRef));
            }
        }
        if (fsItem == null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s (%s) is not adaptable as a FileSystemItem, returning null.",
                        entry.getDocPath(), docRef));
            }
            return null;
        }
        if (expectedFileSystemItemId != null
                && !fsItem.getId()
                          .endsWith(AbstractFileSystemItem.FILE_SYSTEM_ITEM_ID_SEPARATOR + expectedFileSystemItemId)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Id %s of FileSystemItem adapted from document %s (%s) doesn't match expected FileSystemItem id %s, returning null.",
                        fsItem.getId(), entry.getDocPath(), docRef, expectedFileSystemItemId));
            }
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Document %s (%s) is adaptable as a FileSystemItem, providing it to the FileSystemItemChange entry.",
                    entry.getDocPath(), docRef));
        }
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
