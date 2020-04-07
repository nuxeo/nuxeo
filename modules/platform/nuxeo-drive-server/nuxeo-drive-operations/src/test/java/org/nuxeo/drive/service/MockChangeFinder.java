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
package org.nuxeo.drive.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DateUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.impl.FileSystemItemChangeImpl;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Mock implementation of {@link FileSystemChangeFinder} using a {@link CoreSession} query.
 * <p>
 * For test purpose only.
 *
 * @author Antoine Taillefer
 */
public class MockChangeFinder implements FileSystemChangeFinder {

    private static final Logger log = LogManager.getLogger(MockChangeFinder.class);

    protected Map<String, String> parameters = new HashMap<>();

    @Override
    public void handleParameters(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    @Override
    public List<FileSystemItemChange> getFileSystemChanges(CoreSession session, Set<IdRef> lastActiveRootRefs,
            SynchronizationRoots activeRoots, Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound,
            int limit) {

        List<FileSystemItemChange> docChanges = new ArrayList<>();
        if (!activeRoots.paths.isEmpty()) {
            StringBuilder querySb = new StringBuilder();
            querySb.append("SELECT * FROM Document WHERE (%s) AND (%s) ORDER BY dc:modified DESC");
            String query = String.format(querySb.toString(), getRootPathClause(activeRoots.paths),
                    getDateClause(lowerBound, upperBound));
            log.debug("Querying repository for document changes: {}", query);

            NuxeoPrincipal principal = session.getPrincipal();
            RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
            for (String repositoryName : repositoryManager.getRepositoryNames()) {
                CoreSession repoSession = CoreInstance.getCoreSession(repositoryName, principal);
                docChanges.addAll(getDocumentChanges(repoSession, query, limit));
            }
        }
        return docChanges;
    }

    protected String getRootPathClause(Set<String> rootPaths) {
        StringBuilder rootPathClause = new StringBuilder();
        for (String rootPath : rootPaths) {
            if (rootPathClause.length() > 0) {
                rootPathClause.append(" OR ");
            }
            rootPathClause.append(String.format("ecm:path STARTSWITH '%s'", rootPath));
        }
        return rootPathClause.toString();
    }

    protected String getDateClause(long lastSuccessfulSyncDate, long syncDate) {
        return String.format("dc:modified >= TIMESTAMP '%s' and dc:modified < TIMESTAMP '%s'",
                DateUtils.formatISODateTime(new Date(lastSuccessfulSyncDate)),
                DateUtils.formatISODateTime(new Date(syncDate)));
    }

    protected List<FileSystemItemChange> getDocumentChanges(CoreSession session, String query, int limit) {
        List<FileSystemItemChange> docChanges = new ArrayList<>();
        DocumentModelList queryResult = session.query(query, limit);
        if (queryResult.size() >= limit) {
            throw new TooManyChangesException("Too many document changes found in the repository.");
        }
        for (DocumentModel doc : queryResult) {
            String repositoryId = session.getRepositoryName();
            String eventId = "documentChanged";
            long eventDate = ((Calendar) doc.getPropertyValue("dc:modified")).getTimeInMillis();
            String docUuid = doc.getId();
            FileSystemItem fsItem = doc.getAdapter(FileSystemItem.class);
            if (fsItem != null) {
                docChanges.add(new FileSystemItemChangeImpl(eventId, eventDate, repositoryId, docUuid, fsItem));
            }
        }
        return docChanges;
    }

    @Override
    public long getUpperBound() {
        long now = System.currentTimeMillis();
        return now - (now % 1000);
    }

}
