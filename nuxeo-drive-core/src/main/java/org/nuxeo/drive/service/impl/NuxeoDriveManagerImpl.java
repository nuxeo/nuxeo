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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import static org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.FileSystemChangeSummary;
import org.nuxeo.drive.service.FileSystemItemChange;
import org.nuxeo.drive.service.NuxeoDriveEvents;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.drive.service.TooManyChangesException;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Manage list of NuxeoDrive synchronization roots and devices for a given nuxeo user.
 */
public class NuxeoDriveManagerImpl extends DefaultComponent implements NuxeoDriveManager {

    private static final Log log = LogFactory.getLog(NuxeoDriveManagerImpl.class);

    public static final String CHANGE_FINDER_EP = "changeFinder";

    public static final String NUXEO_DRIVE_FACET = "DriveSynchronized";

    public static final String DRIVE_SUBSCRIPTIONS_PROPERTY = "drv:subscriptions";

    public static final String DOCUMENT_CHANGE_LIMIT_PROPERTY = "org.nuxeo.drive.document.change.limit";

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    public static final String DRIVE_SYNC_ROOT_CACHE = "driveSyncRoot";

    public static final String DRIVE_COLLECTION_SYNC_ROOT__MEMBER_CACHE = "driveCollectionSyncRootMember";

    protected static final long COLLECTION_CONTENT_PAGE_SIZE = 1000L;

    /**
     * Cache holding the synchronization roots for a given user (first map key) and repository (second map key).
     */
    protected Cache syncRootCache;

    /**
     * Cache holding the collection sync root member ids for a given user (first map key) and repository (second map
     * key).
     */
    protected Cache collectionSyncRootMemberCache;

    protected static ChangeFinderRegistry changeFinderRegistry;

    protected FileSystemChangeFinder changeFinder;

    protected Cache getSyncRootCache() {
        return syncRootCache;
    }

    protected Cache getCollectionSyncRootMemberCache() {
        return collectionSyncRootMemberCache;
    }

    protected void clearCache() {
        if (log.isDebugEnabled()) {
            log.debug("Invalidating synchronization root cache and collection sync root member cache for all users");
        }
        syncRootCache.invalidateAll();
        collectionSyncRootMemberCache.invalidateAll();
    }

    @Override
    public void invalidateSynchronizationRootsCache(String userName) {
        if (log.isDebugEnabled()) {
            log.debug("Invalidating synchronization root cache for user: " + userName);
        }
        getSyncRootCache().invalidate(userName);
    }

    @Override
    public void invalidateCollectionSyncRootMemberCache(String userName) {
        if (log.isDebugEnabled()) {
            log.debug("Invalidating collection sync root member cache for user: " + userName);
        }
        getCollectionSyncRootMemberCache().invalidate(userName);
    }

    @Override
    public void invalidateCollectionSyncRootMemberCache() {
        if (log.isDebugEnabled()) {
            log.debug("Invalidating collection sync root member cache for all users");
        }
        getCollectionSyncRootMemberCache().invalidateAll();
    }

    @Override
    public void registerSynchronizationRoot(Principal principal, final DocumentModel newRootContainer,
            CoreSession session) {
        final String userName = principal.getName();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Registering synchronization root %s for %s", newRootContainer, userName));
        }
        // If new root is child of a sync root, ignore registration, except for
        // the 'Locally Edited' collection: it is under the personal workspace
        // and we want to allow both the personal workspace and the 'Locally
        // Edited' collection to be registered as sync roots
        Map<String, SynchronizationRoots> syncRoots = getSynchronizationRoots(principal);
        SynchronizationRoots synchronizationRoots = syncRoots.get(session.getRepositoryName());
        if (!NuxeoDriveManager.LOCALLY_EDITED_COLLECTION_NAME.equals(newRootContainer.getName())) {
            for (String syncRootPath : synchronizationRoots.getPaths()) {
                String syncRootPrefixedPath = syncRootPath + "/";

                if (newRootContainer.getPathAsString().startsWith(syncRootPrefixedPath)) {
                    // the only exception is when the right inheritance is
                    // blocked
                    // in the hierarchy
                    boolean rightInheritanceBlockedInTheHierarchy = false;
                    // should get only parents up to the sync root

                    Path parentPath = newRootContainer.getPath().removeLastSegments(1);
                    while (!"/".equals(parentPath.toString())) {
                        String parentPathAsString = parentPath.toString() + "/";
                        if (!parentPathAsString.startsWith(syncRootPrefixedPath)) {
                            break;
                        }
                        PathRef parentRef = new PathRef(parentPathAsString);
                        if (!session.hasPermission(principal, parentRef, SecurityConstants.READ)) {
                            rightInheritanceBlockedInTheHierarchy = true;
                            break;
                        }
                        parentPath = parentPath.removeLastSegments(1);
                    }
                    if (!rightInheritanceBlockedInTheHierarchy) {
                        return;
                    }
                }
            }
        }

        checkCanUpdateSynchronizationRoot(newRootContainer, session);

        // Unregister any sub-folder of the new root, except for the 'Locally
        // Edited' collection
        String newRootPrefixedPath = newRootContainer.getPathAsString() + "/";
        for (String existingRootPath : synchronizationRoots.getPaths()) {
            if (!existingRootPath.endsWith(NuxeoDriveManager.LOCALLY_EDITED_COLLECTION_NAME)) {
                if (existingRootPath.startsWith(newRootPrefixedPath)) {
                    // Unregister the nested root sub-folder first
                    PathRef ref = new PathRef(existingRootPath);
                    if (session.exists(ref)) {
                        DocumentModel subFolder = session.getDocument(ref);
                        unregisterSynchronizationRoot(principal, subFolder, session);
                    }
                }
            }
        }

        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                if (!newRootContainer.hasFacet(NUXEO_DRIVE_FACET)) {
                    newRootContainer.addFacet(NUXEO_DRIVE_FACET);
                }

                fireEvent(newRootContainer, session, NuxeoDriveEvents.ABOUT_TO_REGISTER_ROOT, userName);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> subscriptions = (List<Map<String, Object>>) newRootContainer.getPropertyValue(
                        DRIVE_SUBSCRIPTIONS_PROPERTY);
                boolean updated = false;
                for (Map<String, Object> subscription : subscriptions) {
                    if (userName.equals(subscription.get("username"))) {
                        subscription.put("enabled", Boolean.TRUE);
                        subscription.put("lastChangeDate", Calendar.getInstance(UTC));
                        updated = true;
                        break;
                    }
                }
                if (!updated) {
                    Map<String, Object> subscription = new HashMap<String, Object>();
                    subscription.put("username", userName);
                    subscription.put("enabled", Boolean.TRUE);
                    subscription.put("lastChangeDate", Calendar.getInstance(UTC));
                    subscriptions.add(subscription);
                }
                newRootContainer.setPropertyValue(DRIVE_SUBSCRIPTIONS_PROPERTY, (Serializable) subscriptions);
                newRootContainer.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, true);
                newRootContainer.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, true);
                newRootContainer.putContextData(CoreSession.SOURCE, "drive");
                DocumentModel savedNewRootContainer = session.saveDocument(newRootContainer);
                newRootContainer.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, false);
                newRootContainer.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, false);
                fireEvent(savedNewRootContainer, session, NuxeoDriveEvents.ROOT_REGISTERED, userName);
                session.save();
            }
        };
        runner.runUnrestricted();

        invalidateSynchronizationRootsCache(userName);
        invalidateCollectionSyncRootMemberCache(userName);
    }

    @Override
    public void unregisterSynchronizationRoot(Principal principal, final DocumentModel rootContainer,
            CoreSession session) {
        final String userName = principal.getName();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Unregistering synchronization root %s for %s", rootContainer, userName));
        }
        checkCanUpdateSynchronizationRoot(rootContainer, session);
        UnrestrictedSessionRunner runner = new UnrestrictedSessionRunner(session) {
            @Override
            public void run() {
                if (!rootContainer.hasFacet(NUXEO_DRIVE_FACET)) {
                    rootContainer.addFacet(NUXEO_DRIVE_FACET);
                }
                fireEvent(rootContainer, session, NuxeoDriveEvents.ABOUT_TO_UNREGISTER_ROOT, userName);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> subscriptions = (List<Map<String, Object>>) rootContainer.getPropertyValue(
                        DRIVE_SUBSCRIPTIONS_PROPERTY);
                for (Map<String, Object> subscription : subscriptions) {
                    if (userName.equals(subscription.get("username"))) {
                        subscription.put("enabled", Boolean.FALSE);
                        subscription.put("lastChangeDate", Calendar.getInstance(UTC));
                        break;
                    }
                }
                rootContainer.setPropertyValue(DRIVE_SUBSCRIPTIONS_PROPERTY, (Serializable) subscriptions);
                rootContainer.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, true);
                rootContainer.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, true);
                rootContainer.putContextData(CoreSession.SOURCE, "drive");
                session.saveDocument(rootContainer);
                rootContainer.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, false);
                rootContainer.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, false);
                fireEvent(rootContainer, session, NuxeoDriveEvents.ROOT_UNREGISTERED, userName);
                session.save();
            }
        };
        runner.runUnrestricted();
        invalidateSynchronizationRootsCache(userName);
        invalidateCollectionSyncRootMemberCache(userName);
    }

    @Override
    public Set<IdRef> getSynchronizationRootReferences(CoreSession session) {
        Map<String, SynchronizationRoots> syncRoots = getSynchronizationRoots(session.getPrincipal());
        return syncRoots.get(session.getRepositoryName()).getRefs();
    }

    @Override
    public void handleFolderDeletion(IdRef deleted) {
        clearCache();
    }

    protected void fireEvent(DocumentModel sourceDocument, CoreSession session, String eventName,
            String impactedUserName) {
        EventService eventService = Framework.getService(EventService.class);
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), sourceDocument);
        ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, session.getRepositoryName());
        ctx.setProperty(CoreEventConstants.SESSION_ID, session.getSessionId());
        ctx.setProperty("category", NuxeoDriveEvents.EVENT_CATEGORY);
        ctx.setProperty(NuxeoDriveEvents.IMPACTED_USERNAME_PROPERTY, impactedUserName);
        Event event = ctx.newEvent(eventName);
        eventService.fireEvent(event);
    }

    /**
     * Uses the {@link AuditChangeFinder} to get the summary of document changes for the given user and last successful
     * synchronization date.
     * <p>
     * The {@link #DOCUMENT_CHANGE_LIMIT_PROPERTY} Framework property is used as a limit of document changes to fetch
     * from the audit logs. Default value is 1000. If {@code lastSuccessfulSync} is missing (i.e. set to a negative
     * value), the filesystem change summary is empty but the returned sync date is set to the actual server timestamp
     * so that the client can reuse it as a starting timestamp for a future incremental diff request.
     */
    @Override
    public FileSystemChangeSummary getChangeSummary(Principal principal, Map<String, Set<IdRef>> lastSyncRootRefs,
            long lastSuccessfulSync) {
        Map<String, SynchronizationRoots> roots = getSynchronizationRoots(principal);
        return getChangeSummary(principal, lastSyncRootRefs, roots, new HashMap<String, Set<String>>(),
                lastSuccessfulSync, false);
    }

    /**
     * Uses the {@link AuditChangeFinder} to get the summary of document changes for the given user and lower bound.
     * <p>
     * The {@link #DOCUMENT_CHANGE_LIMIT_PROPERTY} Framework property is used as a limit of document changes to fetch
     * from the audit logs. Default value is 1000. If {@code lowerBound} is missing (i.e. set to a negative value), the
     * filesystem change summary is empty but the returned upper bound is set to the greater event log id so that the
     * client can reuse it as a starting id for a future incremental diff request.
     */
    @Override
    public FileSystemChangeSummary getChangeSummaryIntegerBounds(Principal principal,
            Map<String, Set<IdRef>> lastSyncRootRefs, long lowerBound) {
        Map<String, SynchronizationRoots> roots = getSynchronizationRoots(principal);
        Map<String, Set<String>> collectionSyncRootMemberIds = getCollectionSyncRootMemberIds(principal);
        return getChangeSummary(principal, lastSyncRootRefs, roots, collectionSyncRootMemberIds, lowerBound, true);
    }

    protected FileSystemChangeSummary getChangeSummary(Principal principal, Map<String, Set<IdRef>> lastActiveRootRefs,
            Map<String, SynchronizationRoots> roots, Map<String, Set<String>> collectionSyncRootMemberIds,
            long lowerBound, boolean integerBounds) {
        List<FileSystemItemChange> allChanges = new ArrayList<FileSystemItemChange>();
        // Compute the list of all repositories to consider for the aggregate summary
        Set<String> allRepositories = new TreeSet<String>();
        allRepositories.addAll(roots.keySet());
        allRepositories.addAll(lastActiveRootRefs.keySet());
        allRepositories.addAll(collectionSyncRootMemberIds.keySet());
        long syncDate;
        long upperBound;
        if (integerBounds) {
            upperBound = changeFinder.getUpperBound(allRepositories);
            // Truncate sync date to 0 milliseconds
            syncDate = System.currentTimeMillis();
            syncDate = syncDate - (syncDate % 1000);
        } else {
            upperBound = changeFinder.getCurrentDate();
            syncDate = upperBound;
        }
        Boolean hasTooManyChanges = Boolean.FALSE;
        int limit = Integer.parseInt(Framework.getProperty(DOCUMENT_CHANGE_LIMIT_PROPERTY, "1000"));
        if (!allRepositories.isEmpty() && lowerBound >= 0 && upperBound > lowerBound) {
            for (String repositoryName : allRepositories) {
                try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
                    // Get document changes
                    Set<IdRef> lastRefs = lastActiveRootRefs.get(repositoryName);
                    if (lastRefs == null) {
                        lastRefs = Collections.emptySet();
                    }
                    SynchronizationRoots activeRoots = roots.get(repositoryName);
                    if (activeRoots == null) {
                        activeRoots = SynchronizationRoots.getEmptyRoots(repositoryName);
                    }
                    Set<String> repoCollectionSyncRootMemberIds = collectionSyncRootMemberIds.get(repositoryName);
                    if (repoCollectionSyncRootMemberIds == null) {
                        repoCollectionSyncRootMemberIds = Collections.emptySet();
                    }
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Start: getting FileSystemItem changes for repository %s / user %s between %s and %s with activeRoots = %s",
                                repositoryName, principal.getName(), lowerBound, upperBound, activeRoots.getPaths()));
                    }
                    List<FileSystemItemChange> changes;
                    if (integerBounds) {
                        changes = changeFinder.getFileSystemChangesIntegerBounds(session, lastRefs, activeRoots,
                                repoCollectionSyncRootMemberIds, lowerBound, upperBound, limit);
                    } else {
                        changes = changeFinder.getFileSystemChanges(session, lastRefs, activeRoots, lowerBound,
                                upperBound, limit);
                    }
                    allChanges.addAll(changes);
                } catch (TooManyChangesException e) {
                    hasTooManyChanges = Boolean.TRUE;
                    allChanges.clear();
                    break;
                }
            }
        }

        // Send back to the client the list of currently active roots to be able
        // to efficiently detect root unregistration events for the next
        // incremental change summary
        Map<String, Set<IdRef>> activeRootRefs = new HashMap<String, Set<IdRef>>();
        for (Map.Entry<String, SynchronizationRoots> rootsEntry : roots.entrySet()) {
            activeRootRefs.put(rootsEntry.getKey(), rootsEntry.getValue().getRefs());
        }
        FileSystemChangeSummary summary = new FileSystemChangeSummaryImpl(allChanges, activeRootRefs, syncDate,
                upperBound, hasTooManyChanges);
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "End: getting %d FileSystemItem changes for user %s between %s and %s with activeRoots = %s -> %s",
                    allChanges.size(), principal.getName(), lowerBound, upperBound, roots, summary));
        }
        return summary;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, SynchronizationRoots> getSynchronizationRoots(Principal principal) {
        String userName = principal.getName();
        Map<String, SynchronizationRoots> syncRoots = (Map<String, SynchronizationRoots>) getSyncRootCache().get(
                userName);
        if (syncRoots == null) {
            syncRoots = computeSynchronizationRoots(computeSyncRootsQuery(userName), principal);
            getSyncRootCache().put(userName, (Serializable) syncRoots);
        }
        return syncRoots;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Set<String>> getCollectionSyncRootMemberIds(Principal principal) {
        String userName = principal.getName();
        Map<String, Set<String>> collSyncRootMemberIds = (Map<String, Set<String>>) getCollectionSyncRootMemberCache().get(
                userName);
        if (collSyncRootMemberIds == null) {
            collSyncRootMemberIds = computeCollectionSyncRootMemberIds(principal);
            getCollectionSyncRootMemberCache().put(userName, (Serializable) collSyncRootMemberIds);
        }
        return collSyncRootMemberIds;
    }

    @Override
    public boolean isSynchronizationRoot(Principal principal, DocumentModel doc) {
        String repoName = doc.getRepositoryName();
        SynchronizationRoots syncRoots = getSynchronizationRoots(principal).get(repoName);
        return syncRoots.getRefs().contains(doc.getRef());
    }

    protected Map<String, SynchronizationRoots> computeSynchronizationRoots(String query, Principal principal) {
        Map<String, SynchronizationRoots> syncRoots = new HashMap<String, SynchronizationRoots>();
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
                syncRoots.putAll(queryAndFetchSynchronizationRoots(session, query));
            }
        }
        return syncRoots;
    }

    protected Map<String, SynchronizationRoots> queryAndFetchSynchronizationRoots(CoreSession session, String query) {
        Map<String, SynchronizationRoots> syncRoots = new HashMap<String, SynchronizationRoots>();
        Set<IdRef> references = new LinkedHashSet<IdRef>();
        Set<String> paths = new LinkedHashSet<String>();
        try (IterableQueryResult results = session.queryAndFetch(query, NXQL.NXQL)) {
            for (Map<String, Serializable> result : results) {
                IdRef docRef = new IdRef(result.get("ecm:uuid").toString());
                try {
                    DocumentModel doc = session.getDocument(docRef);
                    references.add(docRef);
                    paths.add(doc.getPathAsString());
                } catch (DocumentNotFoundException e) {
                    log.warn(String.format(
                            "Document %s not found, not adding it to the list of synchronization roots for user %s.",
                            docRef, session.getPrincipal().getName()));
                } catch (DocumentSecurityException e) {
                    log.warn(String.format(
                            "User %s cannot access document %s, not adding it to the list of synchronization roots.",
                            session.getPrincipal().getName(), docRef));
                }
            }
        }
        SynchronizationRoots repoSyncRoots = new SynchronizationRoots(session.getRepositoryName(), paths, references);
        syncRoots.put(session.getRepositoryName(), repoSyncRoots);
        return syncRoots;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Set<String>> computeCollectionSyncRootMemberIds(Principal principal) {
        Map<String, Set<String>> collectionSyncRootMemberIds = new HashMap<String, Set<String>>();
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        for (String repositoryName : repositoryManager.getRepositoryNames()) {
            Set<String> collectionMemberIds = new HashSet<String>();
            try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName, principal)) {
                Map<String, Serializable> props = new HashMap<String, Serializable>();
                props.put(CORE_SESSION_PROPERTY, (Serializable) session);
                PageProvider<DocumentModel> collectionPageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                        CollectionConstants.ALL_COLLECTIONS_PAGE_PROVIDER, null, null, 0L, props);
                List<DocumentModel> collections = collectionPageProvider.getCurrentPage();
                for (DocumentModel collection : collections) {
                    if (isSynchronizationRoot(principal, collection)) {
                        PageProvider<DocumentModel> collectionMemberPageProvider = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                                CollectionConstants.COLLECTION_CONTENT_PAGE_PROVIDER, null,
                                COLLECTION_CONTENT_PAGE_SIZE, 0L, props, collection.getId());
                        List<DocumentModel> collectionMembers = collectionMemberPageProvider.getCurrentPage();
                        for (DocumentModel collectionMember : collectionMembers) {
                            collectionMemberIds.add(collectionMember.getId());
                        }
                    }
                }
                collectionSyncRootMemberIds.put(repositoryName, collectionMemberIds);
            }
        }
        return collectionSyncRootMemberIds;
    }

    protected void checkCanUpdateSynchronizationRoot(DocumentModel newRootContainer, CoreSession session) {
        // Cannot update a proxy or a version
        if (newRootContainer.isProxy() || newRootContainer.isVersion()) {
            throw new NuxeoException(String.format(
                    "Document '%s' (%s) is not a suitable synchronization root"
                            + " as it is either a readonly proxy or an archived version.",
                    newRootContainer.getTitle(), newRootContainer.getRef()));
        }
    }

    @Override
    public FileSystemChangeFinder getChangeFinder() {
        return changeFinder;
    }

    /**
     * @since 5.9.5
     */
    protected String computeSyncRootsQuery(String username) {
        return String.format(
                "SELECT ecm:uuid FROM Document" //
                        + " WHERE %s/*1/username = %s" //
                        + " AND %s/*1/enabled = 1" //
                        + " AND ecm:isTrashed = 0" //
                        + " AND ecm:isVersion = 0" //
                        + " ORDER BY dc:title, dc:created DESC",
                DRIVE_SUBSCRIPTIONS_PROPERTY, NXQLQueryBuilder.prepareStringLiteral(username, true, true),
                DRIVE_SUBSCRIPTIONS_PROPERTY);
    }

    @Override
    public void addToLocallyEditedCollection(CoreSession session, DocumentModel doc) {

        // Add document to "Locally Edited" collection, creating if if not
        // exists
        CollectionManager cm = Framework.getService(CollectionManager.class);
        DocumentModel userCollections = cm.getUserDefaultCollections(session);
        DocumentRef locallyEditedCollectionRef = new PathRef(userCollections.getPath().toString(),
                LOCALLY_EDITED_COLLECTION_NAME);
        DocumentModel locallyEditedCollection = null;
        if (session.exists(locallyEditedCollectionRef)) {
            locallyEditedCollection = session.getDocument(locallyEditedCollectionRef);
            cm.addToCollection(locallyEditedCollection, doc, session);
        } else {
            cm.addToNewCollection(LOCALLY_EDITED_COLLECTION_NAME, "Documents locally edited with Nuxeo Drive", doc,
                    session);
            locallyEditedCollection = session.getDocument(locallyEditedCollectionRef);
        }

        // Register "Locally Edited" collection as a synchronization root if not
        // already the case
        Set<IdRef> syncRootRefs = getSynchronizationRootReferences(session);
        if (!syncRootRefs.contains(new IdRef(locallyEditedCollection.getId()))) {
            registerSynchronizationRoot(session.getPrincipal(), locallyEditedCollection, session);
        }
    }

    /*------------------------ DefaultComponent -----------------------------*/
    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CHANGE_FINDER_EP.equals(extensionPoint)) {
            changeFinderRegistry.addContribution((ChangeFinderDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CHANGE_FINDER_EP.equals(extensionPoint)) {
            changeFinderRegistry.removeContribution((ChangeFinderDescriptor) contribution);
        } else {
            log.error("Unknown extension point " + extensionPoint);
        }
    }

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        if (changeFinderRegistry == null) {
            changeFinderRegistry = new ChangeFinderRegistry();
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
        changeFinderRegistry = null;
    }

    @Override
    public int getApplicationStartedOrder() {
        ComponentInstance cacheComponent = Framework.getRuntime()
                                                    .getComponentInstance("org.nuxeo.ecm.core.cache.CacheService");
        if (cacheComponent == null || cacheComponent.getInstance() == null) {
            return super.getApplicationStartedOrder();
        }
        return ((DefaultComponent) cacheComponent.getInstance()).getApplicationStartedOrder() + 1;
    }

    /**
     * Sorts the contributed factories according to their order.
     */
    @Override
    public void start(ComponentContext context) {
        syncRootCache = Framework.getService(CacheService.class).getCache(DRIVE_SYNC_ROOT_CACHE);
        collectionSyncRootMemberCache = Framework.getService(CacheService.class)
                                                 .getCache(DRIVE_COLLECTION_SYNC_ROOT__MEMBER_CACHE);
        changeFinder = changeFinderRegistry.changeFinder;
    }

    @Override
    public void stop(ComponentContext context) {
        syncRootCache = null;
        collectionSyncRootMemberCache = null;
        changeFinder = null;
    }

}
