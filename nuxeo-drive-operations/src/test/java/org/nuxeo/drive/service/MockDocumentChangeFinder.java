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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.service.impl.DocumentChange;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Mock implementation of {@link DocumentChangeFinder} using a
 * {@link CoreSession} query.
 * <p>
 * For test purpose only.
 *
 * @author Antoine Taillefer
 */
public class MockDocumentChangeFinder implements DocumentChangeFinder {

    private static final long serialVersionUID = -8829376616919987451L;

    private static final Log log = LogFactory.getLog(MockDocumentChangeFinder.class);

    @Override
    public List<DocumentChange> getDocumentChanges(CoreSession session,
            Set<String> rootPaths, long lastSuccessfulSync, int limit)
            throws TooManyDocumentChangesException {

        List<DocumentChange> docChanges = new ArrayList<DocumentChange>();
        if (!rootPaths.isEmpty()) {
            try {
                StringBuilder querySb = new StringBuilder();
                querySb.append("SELECT * FROM Document WHERE (%s) AND dc:modified > '%s' ORDER BY dc:modified DESC");
                String query = String.format(querySb.toString(),
                        getRootPathClause(rootPaths),
                        getLastSuccessfulSyncDate(lastSuccessfulSync));
                log.debug("Querying repository for document changes: " + query);

                DocumentModelList queryResult = session.query(query, limit);
                if (queryResult.size() >= limit) {
                    throw new TooManyDocumentChangesException(
                            "Too many document changes found in the repository.");
                }
                for (DocumentModel doc : queryResult) {
                    String repositoryId = session.getRepositoryName();
                    String eventId = "documentChanged";
                    String docLifeCycleState = doc.getCurrentLifeCycleState();
                    long eventDate = ((Calendar) doc.getPropertyValue("dc:modified")).getTimeInMillis();
                    String docPath = doc.getPathAsString();
                    String docUuid = doc.getId();
                    docChanges.add(new DocumentChange(repositoryId, eventId,
                            docLifeCycleState, eventDate, docPath, docUuid));
                }
            } catch (Exception e) {
                throw new ClientRuntimeException(e);
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
            rootPathClause.append(String.format("ecm:path STARTSWITH '%s'",
                    rootPath));
        }
        return rootPathClause.toString();
    }

    protected String getLastSuccessfulSyncDate(long lastSuccessfulSync) {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(new Date(lastSuccessfulSync));
    }

}
