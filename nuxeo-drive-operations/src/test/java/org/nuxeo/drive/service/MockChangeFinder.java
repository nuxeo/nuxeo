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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.impl.FileSystemItemChangeImpl;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Mock implementation of {@link FileSystemChangeFinder} using a
 * {@link CoreSession} query.
 * <p>
 * For test purpose only.
 *
 * @author Antoine Taillefer
 */
public class MockChangeFinder implements FileSystemChangeFinder {

    private static final long serialVersionUID = -8829376616919987451L;

    private static final Log log = LogFactory.getLog(MockChangeFinder.class);

    @Override
    public List<FileSystemItemChange> getFileSystemChanges(CoreSession session,
            Set<IdRef> lastActiveRootRefs, SynchronizationRoots activeRoots,
            long lastSuccessfulSyncDate, long syncDate, int limit)
            throws TooManyChangesException {

        List<FileSystemItemChange> docChanges = new ArrayList<FileSystemItemChange>();
        if (!activeRoots.paths.isEmpty()) {
            StringBuilder querySb = new StringBuilder();
            querySb.append("SELECT * FROM Document WHERE (%s) AND (%s) ORDER BY dc:modified DESC");
            String query = String.format(querySb.toString(),
                    getRootPathClause(activeRoots.paths),
                    getDateClause(lastSuccessfulSyncDate, syncDate));
            log.debug("Querying repository for document changes: " + query);

            NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
            RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
            for (Repository repo : repositoryManager.getRepositories()) {
                CoreSession repoSession = null;
                try {
                    Map<String, Serializable> context = new HashMap<String, Serializable>();
                    context.put("principal", principal);
                    repoSession = repo.open(context);
                    docChanges.addAll(getDocumentChanges(repoSession, query,
                            limit));
                } catch (TooManyChangesException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ClientRuntimeException(e);
                } finally {
                    if (repoSession != null) {
                        CoreInstance.getInstance().close(repoSession);
                    }
                }
            }
        }
        return docChanges;
    }

    @Override
    public List<FileSystemItemChange> getFileSystemChangesIntegerBounds(
            CoreSession session, Set<IdRef> lastActiveRootRefs,
            SynchronizationRoots activeRoots, long lowerBound, long upperBound,
            int limit) throws ClientException, TooManyChangesException {
        throw new UnsupportedOperationException(
                "Using MockChangeFinder with integer bounds is not implemented, please call #getFileSystemChanges.");
    }

    protected String getRootPathClause(Set<String> rootPaths) {
        StringBuilder rootPathClause = new StringBuilder();
        for (String rootPath : rootPaths) {
            if (rootPathClause.length() > 0) {
                rootPathClause.append(" OR ");
            }
            rootPathClause.append(String.format("ecm:path STARTSWITH '%s'",
                    rootPath));
        }
        return rootPathClause.toString();
    }

    protected String getDateClause(long lastSuccessfulSyncDate, long syncDate) {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format(
                "dc:modified >= TIMESTAMP '%s' and dc:modified < TIMESTAMP '%s'",
                sdf.format(new Date(lastSuccessfulSyncDate)),
                sdf.format(new Date(syncDate)));
    }

    protected List<FileSystemItemChange> getDocumentChanges(
            CoreSession session, String query, int limit)
            throws TooManyChangesException {

        try {
            List<FileSystemItemChange> docChanges = new ArrayList<FileSystemItemChange>();
            DocumentModelList queryResult = session.query(query, limit);
            if (queryResult.size() >= limit) {
                throw new TooManyChangesException(
                        "Too many document changes found in the repository.");
            }
            for (DocumentModel doc : queryResult) {
                String repositoryId = session.getRepositoryName();
                String eventId = "documentChanged";
                long eventDate = ((Calendar) doc.getPropertyValue("dc:modified")).getTimeInMillis();
                String docUuid = doc.getId();
                FileSystemItem fsItem = doc.getAdapter(FileSystemItem.class);
                if (fsItem != null) {
                    docChanges.add(new FileSystemItemChangeImpl(eventId,
                            eventDate, repositoryId, docUuid, fsItem));
                }
            }
            return docChanges;
        } catch (TooManyChangesException e) {
            throw e;
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public long getCurrentDate() {
        long now = System.currentTimeMillis();
        return now - (now % 1000);
    }

    @Override
    public long getUpperBound() {
        throw new UnsupportedOperationException(
                "Using MockChangeFinder with integer bounds is not implemented, please call #getCurrentDate.");
    }

}
