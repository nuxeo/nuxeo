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
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.nuxeo.drive.service.DocumentChangeFinder;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.TooManyDocumentChangesException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.model.PropertyException;
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

    public static final String DRIVE_SUBSCRIBERS_PROPERTY = "drv:subscribers";

    public static final String DOCUMENT_CHANGE_LIMIT_PROPERTY = "org.nuxeo.drive.document.change.limit";

    /**
     * Cache holding the synchronization roots as a 2 dimension array: the first
     * element is a set of root references of type {@link DocumentRef}, the
     * second one a set of root paths of type {@link String}.
     */
    // TODO: upgrade to latest version of google collections to be able to limit
    // the size with a LRU policy
    ConcurrentMap<String, Serializable[]> cache = new MapMaker().concurrencyLevel(
            4).softKeys().softValues().expiration(10, TimeUnit.MINUTES).makeMap();

    // TODO: make this overridable with an extension point
    protected DocumentChangeFinder documentChangeFinder = new AuditDocumentChangeFinder();

    @Override
    public void registerSynchronizationRoot(String userName,
            DocumentModel newRootContainer, CoreSession session)
            throws PropertyException, ClientException, SecurityException {
        if (!newRootContainer.hasFacet(NUXEO_DRIVE_FACET)) {
            newRootContainer.addFacet(NUXEO_DRIVE_FACET);
        }
        if (!newRootContainer.isFolder() || newRootContainer.isProxy()
                || newRootContainer.isVersion()) {
            throw new ClientException(String.format(
                    "Document '%s' (%s) is not a suitable synchronization root"
                            + " as it is either not folderish or is a readonly"
                            + " proxy or archived version.",
                    newRootContainer.getTitle(), newRootContainer.getRef()));
        }
        UserManager userManager = Framework.getLocalService(UserManager.class);
        if (!session.hasPermission(userManager.getPrincipal(userName),
                newRootContainer.getRef(), SecurityConstants.ADD_CHILDREN)) {
            throw new SecurityException(String.format(
                    "%s has no permission to create content in '%s' (%s).",
                    userName, newRootContainer.getTitle(),
                    newRootContainer.getRef()));
        }
        String[] subscribers = (String[]) newRootContainer.getPropertyValue(DRIVE_SUBSCRIBERS_PROPERTY);
        if (subscribers == null) {
            subscribers = new String[] { userName };
        } else {
            if (Arrays.binarySearch(subscribers, userName) < 0) {
                String[] old = subscribers;
                subscribers = new String[old.length + 1];
                for (int i = 0; i < old.length; i++) {
                    subscribers[i] = old[i];
                }
                subscribers[old.length] = userName;
                Arrays.sort(subscribers);
            }
        }
        newRootContainer.setPropertyValue(DRIVE_SUBSCRIBERS_PROPERTY,
                (Serializable) subscribers);
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
        String[] subscribers = (String[]) rootContainer.getPropertyValue(DRIVE_SUBSCRIBERS_PROPERTY);
        if (subscribers == null) {
            subscribers = new String[0];
        } else {
            if (Arrays.binarySearch(subscribers, userName) >= 0) {
                String[] old = subscribers;
                subscribers = new String[old.length - 1];
                for (int i = 0; i < old.length; i++) {
                    if (!userName.equals(old[i])) {
                        subscribers[i] = old[i];
                    }
                }
            }
        }
        rootContainer.setPropertyValue(DRIVE_SUBSCRIBERS_PROPERTY,
                (Serializable) subscribers);
        session.saveDocument(rootContainer);
        session.save();
        cache.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<IdRef> getSynchronizationRootReferences(String userName,
            CoreSession session) throws ClientException {
        return (Set<IdRef>) getSynchronizationRoots(userName, session)[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getSynchronizationRootPaths(String userName,
            CoreSession session) throws ClientException {
        return (Set<String>) getSynchronizationRoots(userName, session)[1];
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
     * {@link DocumentChangeSummary#STATUS_TOO_MANY_CHANGES} if the audit log
     * query returns too many results, to
     * {@link DocumentChangeSummary#STATUS_NO_CHANGES} if no results are
     * returned and to {@link DocumentChangeSummary#STATUS_FOUND_CHANGES}
     * otherwise.
     * <p>
     * The {@link #DOCUMENT_CHANGE_LIMIT_PROPERTY} Framework property is used as
     * a limit of document changes to fetch from the audit logs. Default value
     * is 1000.
     */
    @Override
    public DocumentChangeSummary getDocumentChangeSummary(String userName,
            CoreSession session, Calendar lastSuccessfulSync)
            throws ClientException {

        List<AuditDocumentChange> docChanges = new ArrayList<AuditDocumentChange>();
        Map<String, DocumentModel> changedDocModels = new HashMap<String, DocumentModel>();
        String statusCode = DocumentChangeSummary.STATUS_NO_CHANGES;

        // Get sync root paths
        Set<String> syncRootPaths = getSynchronizationRootPaths(userName,
                session);
        if (!syncRootPaths.isEmpty()) {
            try {
                // Get document changes
                int limit = Integer.parseInt(Framework.getProperty(
                        DOCUMENT_CHANGE_LIMIT_PROPERTY, "1000"));
                docChanges = documentChangeFinder.getDocumentChanges(
                        session.getRepositoryName(), syncRootPaths,
                        lastSuccessfulSync, limit);
                if (!docChanges.isEmpty()) {
                    // Build map of document models that have changed
                    for (AuditDocumentChange docChange : docChanges) {
                        String docUuid = docChange.getDocUuid();
                        if (!changedDocModels.containsKey(docUuid)) {
                            changedDocModels.put(docUuid,
                                    session.getDocument(new IdRef(docUuid)));
                        }
                    }
                    statusCode = DocumentChangeSummary.STATUS_FOUND_CHANGES;
                }
            } catch (TooManyDocumentChangesException e) {
                statusCode = DocumentChangeSummary.STATUS_TOO_MANY_CHANGES;
            }
        }

        return new DocumentChangeSummary(docChanges, changedDocModels,
                statusCode);
    }

    protected Serializable[] getSynchronizationRoots(String userName,
            CoreSession session) throws ClientException {
        // cache uses soft keys hence physical equality: intern key before
        // lookup
        userName = userName.intern();
        Serializable[] syncRoots = cache.get(userName);
        if (syncRoots == null) {
            syncRoots = new Serializable[2];
            Set<IdRef> references = new LinkedHashSet<IdRef>();
            Set<String> paths = new LinkedHashSet<String>();
            String q = String.format(
                    "SELECT ecm:uuid FROM Document WHERE %s = %s"
                            + " AND ecm:currentLifeCycleState <> 'deleted'"
                            + " ORDER BY dc:title, dc:created DESC",
                    DRIVE_SUBSCRIBERS_PROPERTY,
                    NXQLQueryBuilder.prepareStringLiteral(userName, true, true));
            IterableQueryResult results = session.queryAndFetch(q, NXQL.NXQL);
            for (Map<String, Serializable> result : results) {
                IdRef docRef = new IdRef(result.get("ecm:uuid").toString());
                references.add(docRef);
                paths.add(session.getDocument(docRef).getPathAsString());
            }
            results.close();
            syncRoots[0] = (Serializable) references;
            syncRoots[1] = (Serializable) paths;
            cache.put(userName, syncRoots);
        }
        return syncRoots;
    }

}
