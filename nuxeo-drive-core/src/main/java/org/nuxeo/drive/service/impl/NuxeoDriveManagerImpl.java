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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.drive.service.TooManyChangesException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.security.SecurityException;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import com.google.common.collect.MapMaker;

/**
 * Manage list of NuxeoDrive synchronization roots and devices for a given nuxeo
 * user.
 */
public class NuxeoDriveManagerImpl extends DefaultComponent implements
        NuxeoDriveManager {

    public static final String NUXEO_DRIVE_FACET = "DriveSynchronized";

    public static final String DRIVE_SUBSCRIPTIONS_PROPERTY = "drv:subscriptions";

    public static final String DOCUMENT_CHANGE_LIMIT_PROPERTY = "org.nuxeo.drive.document.change.limit";

    /**
     * Cache holding the synchronization roots for a given user (first map key)
     * and repository (second map key).
     */
    // TODO: upgrade to latest version of google collections to be able to limit
    // the size with a LRU policy
    ConcurrentMap<String, Map<String, SynchronizationRoots>> cache = new MapMaker().concurrencyLevel(
            4).softKeys().softValues().expiration(10, TimeUnit.MINUTES).makeMap();

    // TODO: make this overridable with an extension point
    protected FileSystemChangeFinder changeFinder = new AuditDocumentChangeFinder();

    @Override
    public void registerSynchronizationRoot(String userName,
            DocumentModel newRootContainer, CoreSession session)
            throws PropertyException, ClientException, SecurityException {
        if (!newRootContainer.hasFacet(NUXEO_DRIVE_FACET)) {
            newRootContainer.addFacet(NUXEO_DRIVE_FACET);
        }
        if (newRootContainer.isProxy() || newRootContainer.isVersion()) {
            throw new ClientException(
                    String.format(
                            "Document '%s' (%s) is not a suitable synchronization root"
                                    + " as it is either a readonly proxy or an archived version.",
                            newRootContainer.getTitle(),
                            newRootContainer.getRef()));
        }
        UserManager userManager = Framework.getLocalService(UserManager.class);
        if (!session.hasPermission(userManager.getPrincipal(userName),
                newRootContainer.getRef(), SecurityConstants.ADD_CHILDREN)) {
            throw new SecurityException(String.format(
                    "%s has no permission to create content in '%s' (%s).",
                    userName, newRootContainer.getTitle(),
                    newRootContainer.getRef()));
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subscriptions = (List<Map<String, Object>>) newRootContainer.getPropertyValue(DRIVE_SUBSCRIPTIONS_PROPERTY);
        boolean updated = false;
        for (Map<String, Object> subscription : subscriptions) {
            if (userName.equals(subscription.get("username"))) {
                subscription.put("enabled", Boolean.TRUE);
                subscription.put("lastChangeDate",
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                updated = true;
                break;
            }
        }
        if (!updated) {
            Map<String, Object> subscription = new HashMap<String, Object>();
            subscription.put("username", userName);
            subscription.put("enabled", Boolean.TRUE);
            subscription.put("lastChangeDate",
                    Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            subscriptions.add(subscription);
        }
        newRootContainer.setPropertyValue(DRIVE_SUBSCRIPTIONS_PROPERTY,
                (Serializable) subscriptions);
        session.saveDocument(newRootContainer);
        session.save();
        cache.clear();
    }

    @Override
    public void unregisterSynchronizationRoot(String userName,
            DocumentModel rootContainer, CoreSession session)
            throws PropertyException, ClientException {
        if (!rootContainer.hasFacet(NUXEO_DRIVE_FACET)) {
            rootContainer.addFacet(NUXEO_DRIVE_FACET);
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subscriptions = (List<Map<String, Object>>) rootContainer.getPropertyValue(DRIVE_SUBSCRIPTIONS_PROPERTY);
        for (Map<String, Object> subscription : subscriptions) {
            if (userName.equals(subscription.get("username"))) {
                subscription.put("enabled", Boolean.FALSE);
                subscription.put("lastChangeDate",
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                break;
            }
        }
        rootContainer.setPropertyValue(DRIVE_SUBSCRIPTIONS_PROPERTY,
                (Serializable) subscriptions);
        session.saveDocument(rootContainer);
        session.save();
        cache.clear();
    }

    @Override
    public Set<IdRef> getSynchronizationRootReferences(String userName,
            CoreSession session) throws ClientException {
        Map<String, SynchronizationRoots> syncRoots = getSynchronizationRoots(
                false, userName, session);
        return syncRoots.get(session.getRepositoryName()).refs;
    }

    @Override
    @Deprecated
    // XXX: use getSynchronizationRoots directly to avoid mixing references from
    // various repos
    public Set<IdRef> getSynchronizationRootReferences(boolean allRepositories,
            String userName, CoreSession session) throws ClientException {
        Map<String, SynchronizationRoots> syncRoots = getSynchronizationRoots(
                allRepositories, userName, session);
        if (allRepositories) {
            Set<IdRef> syncRootRefs = new LinkedHashSet<IdRef>();
            for (SynchronizationRoots repoSyncRoots : syncRoots.values()) {
                syncRootRefs.addAll((Set<IdRef>) repoSyncRoots.refs);
            }
            return syncRootRefs;
        } else {
            return syncRoots.get(session.getRepositoryName()).refs;
        }
    }

    @Override
    @Deprecated
    // XXX: use getSynchronizationRoots directly to avoid mixing paths from
    // various repos
    public Set<String> getSynchronizationRootPaths(boolean allRepositories,
            String userName, CoreSession session) throws ClientException {
        Map<String, SynchronizationRoots> syncRoots = getSynchronizationRoots(
                allRepositories, userName, session);
        if (allRepositories) {
            Set<String> syncRootPaths = new LinkedHashSet<String>();
            for (SynchronizationRoots repoSyncRoots : syncRoots.values()) {
                syncRootPaths.addAll(repoSyncRoots.paths);
            }
            return syncRootPaths;
        } else {
            return syncRoots.get(session.getRepositoryName()).paths;
        }
    }

    @Override
    public void handleFolderDeletion(IdRef deleted) throws ClientException {
        cache.clear();
    }

    /**
     * Uses the {@link AuditDocumentChangeFinder} to get the summary of document
     * changes for the given user and last successful synchronization date.
     * <p>
     * Sets the status code to
     * {@link FileSystemChangeSummary#STATUS_TOO_MANY_CHANGES} if the audit log
     * query returns too many results, to
     * {@link FileSystemChangeSummary#STATUS_NO_CHANGES} if no results are
     * returned and to {@link FileSystemChangeSummary#STATUS_FOUND_CHANGES}
     * otherwise.
     * <p>
     * The {@link #DOCUMENT_CHANGE_LIMIT_PROPERTY} Framework property is used as
     * a limit of document changes to fetch from the audit logs. Default value
     * is 1000.
     */
    @Override
    public FileSystemChangeSummary getDocumentChangeSummary(
            boolean allRepositories, String userName, CoreSession session,
            long lastSuccessfulSync) throws ClientException {

        // Get sync root paths
        Set<String> syncRootPaths = getSynchronizationRootPaths(
                allRepositories, userName, session);
        return getDocumentChangeSummary(allRepositories, syncRootPaths,
                session, lastSuccessfulSync);
    }

    /**
     * Uses the {@link AuditDocumentChangeFinder} to get the summary of document
     * changes for the given folder and last successful synchronization date.
     *
     * @see #getDocumentChangeSummary(boolean, String, CoreSession, long)
     */
    @Override
    public FileSystemChangeSummary getFolderChangeSummary(String folderPath,
            CoreSession session, long lastSuccessfulSync)
            throws ClientException {

        Set<String> syncRootPaths = new HashSet<String>();
        syncRootPaths.add(folderPath);
        return getDocumentChangeSummary(false, syncRootPaths, session,
                lastSuccessfulSync);
    }

    protected FileSystemChangeSummary getDocumentChangeSummary(
            boolean allRepositories, Set<String> syncRootPaths,
            CoreSession session, long lastSuccessfulSync)
            throws ClientException {

        List<FileSystemItemChange> changes = new ArrayList<FileSystemItemChange>();
        Map<String, DocumentModel> changedDocModels = new HashMap<String, DocumentModel>();

        // Update sync date, rounded to the lower second to ensure consistency
        // in the case of databases that don't support milliseconds
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.MILLISECOND, 0);
        long syncDate = cal.getTimeInMillis();
        Boolean hasTooManyChanges = Boolean.FALSE;
        if (!syncRootPaths.isEmpty()) {
            try {
                // Get document changes
                int limit = Integer.parseInt(Framework.getProperty(
                        DOCUMENT_CHANGE_LIMIT_PROPERTY, "1000"));
                changes = changeFinder.getFileSystemChanges(allRepositories,
                        session, syncRootPaths, lastSuccessfulSync, syncDate,
                        limit);
                if (!changes.isEmpty()) {
                    // remove changes referring to documents
                    // not adaptable as a FileSystemItem or not visible to the
                    // active user due to permission restrictions
                    filterAndAdaptDocuments(allRepositories, session, changes,
                            changedDocModels);
                }
            } catch (TooManyChangesException e) {
                hasTooManyChanges = Boolean.TRUE;
            }
        }
        return new FileSystemChangeSummary(changes, syncDate, hasTooManyChanges);
    }

    public Map<String, SynchronizationRoots> getSynchronizationRoots(
            boolean allRepositories, String userName, CoreSession session)
            throws ClientException {
        // cache uses soft keys hence physical equality: intern key before
        // lookup
        userName = userName.intern();
        Map<String, SynchronizationRoots> syncRoots = cache.get(userName);
        if (syncRoots == null) {
            syncRoots = new HashMap<String, SynchronizationRoots>();
            String query = String.format(
                    "SELECT ecm:uuid FROM Document WHERE %s/*1/username = %s"
                            + " AND %s/*1/enabled = 1"
                            + " AND ecm:currentLifeCycleState <> 'deleted'"
                            + " ORDER BY dc:title, dc:created DESC",
                    DRIVE_SUBSCRIPTIONS_PROPERTY,
                    NXQLQueryBuilder.prepareStringLiteral(userName, true, true),
                    DRIVE_SUBSCRIPTIONS_PROPERTY);
            computeSynchronizationRoots(allRepositories, query, session,
                    syncRoots);
            cache.put(userName, syncRoots);
        }
        return syncRoots;
    }

    protected void computeSynchronizationRoots(boolean allRepositories,
            String query, CoreSession session,
            Map<String, SynchronizationRoots> syncRoots) throws ClientException {

        if (allRepositories) {
            CoreSession repoSession = null;
            NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
            RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
            for (Repository repo : repositoryManager.getRepositories()) {
                Map<String, Serializable> context = new HashMap<String, Serializable>();
                context.put("principal", principal);
                try {
                    repoSession = repo.open(context);
                    queryAndFecthSynchronizationRoots(repoSession, query,
                            syncRoots);
                } catch (Exception e) {
                    throw ClientException.wrap(e);
                } finally {
                    if (repoSession != null) {
                        CoreInstance.getInstance().close(repoSession);
                    }
                }
            }
        } else {
            queryAndFecthSynchronizationRoots(session, query, syncRoots);
        }
    }

    protected void queryAndFecthSynchronizationRoots(CoreSession session,
            String query, Map<String, SynchronizationRoots> syncRoots)
            throws ClientException {

        Set<IdRef> references = new LinkedHashSet<IdRef>();
        Set<String> paths = new LinkedHashSet<String>();
        IterableQueryResult results = session.queryAndFetch(query, NXQL.NXQL);
        for (Map<String, Serializable> result : results) {
            IdRef docRef = new IdRef(result.get("ecm:uuid").toString());
            references.add(docRef);
            paths.add(session.getDocument(docRef).getPathAsString());
        }
        results.close();
        SynchronizationRoots repoSyncRoots = new SynchronizationRoots(
                session.getRepositoryName(), paths, references);
        syncRoots.put(session.getRepositoryName(), repoSyncRoots);
    }

    protected void filterAndAdaptDocuments(boolean allRepositories,
            CoreSession session, List<FileSystemItemChange> docChanges,
            Map<String, DocumentModel> changedDocModels) throws ClientException {

        // Fetch the roots for the current user to be able to find the adapted
        // parent ids for there child if needed.
        Map<String, SynchronizationRoots> synchronizationRoots = getSynchronizationRoots(
                allRepositories, session.getPrincipal().getName(), session);

        List<FileSystemItemChange> filteredChanges = new ArrayList<FileSystemItemChange>();
        if (allRepositories) {
            RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
            NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
            Map<String, CoreSession> repoSessions = new HashMap<String, CoreSession>();
            try {
                for (FileSystemItemChange docChange : docChanges) {
                    String repositoryId = docChange.getRepositoryId();
                    CoreSession repoSession = repoSessions.get(repositoryId);
                    if (repoSession == null) {
                        Repository repository = repositoryManager.getRepository(repositoryId);
                        if (repository == null) {
                            throw new ClientRuntimeException(String.format(
                                    "No repository registered with id '%s'.",
                                    repositoryId));
                        }
                        Map<String, Serializable> context = new HashMap<String, Serializable>();
                        context.put("principal", principal);
                        try {
                            repoSession = repository.open(context);
                        } catch (Exception e) {
                            throw ClientException.wrap(e);
                        }
                        repoSessions.put(repositoryId, repoSession);
                    }
                    if (adaptDocument(docChange, repoSession,
                            synchronizationRoots.get(repositoryId))) {
                        filteredChanges.add(docChange);
                    }
                }
            } finally {
                for (CoreSession repoSession : repoSessions.values()) {
                    CoreInstance.getInstance().close(repoSession);
                }
            }
        } else {
            for (FileSystemItemChange docChange : docChanges) {
                if (adaptDocument(docChange, session,
                        synchronizationRoots.get(session.getRepositoryName()))) {
                    filteredChanges.add(docChange);
                }
            }
        }
        docChanges.clear();
        docChanges.addAll(filteredChanges);
    }

    /**
     * Map the backing document to a FileSystemItem using the adapters when
     * possible and store the mapping in the FileSystemItemChange instance. If
     * not possible (because of missing permissions for instance), skip the
     * change.
     */
    protected boolean adaptDocument(FileSystemItemChange docChange,
            CoreSession session, SynchronizationRoots synchronizationRoots)
            throws ClientException {
        IdRef ref = new IdRef(docChange.getDocUuid());
        try {
            DocumentModel doc = session.getDocument(ref);
            // TODO: check the facet, last root change and list of roots to have
            // a special handling for the roots.
            FileSystemItem fsItem = doc.getAdapter(FileSystemItem.class);
            if (fsItem == null) {
                return false;
            }
            docChange.setFileSystemItem(fsItem);
            return true;
        } catch (DocumentSecurityException e) {
            // This event matches a document that is not visible by the
            // current user, skip it.
            // TODO: how to detect ACL removal to map those as
            return false;
        }
    }

    // TODO: make changeFinder overridable with an extension point and
    // remove setter
    public void setChangeFinder(FileSystemChangeFinder changeFinder) {
        this.changeFinder = changeFinder;
    }

}
