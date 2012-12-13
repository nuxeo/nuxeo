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
package org.nuxeo.drive.service.impl;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.TooManyChangesException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of {@link FileSystemChangeFinder} using the {@link AuditReader}.
 *
 * @author Antoine Taillefer
 */
public class AuditDocumentChangeFinder implements FileSystemChangeFinder {

    private static final long serialVersionUID = 1963018967324857522L;

    private static final Log log = LogFactory.getLog(AuditDocumentChangeFinder.class);

    @Override
    @SuppressWarnings("unchecked")
    public List<FileSystemItemChange> getFileSystemChanges(boolean allRepositories,
            CoreSession session, Set<String> rootPaths,
            long lastSuccessfulSyncDate, long syncDate, int limit)
            throws TooManyChangesException {

        List<FileSystemItemChange> changes = new ArrayList<FileSystemItemChange>();
        if (!rootPaths.isEmpty()) {
            AuditReader auditService = Framework.getLocalService(AuditReader.class);
            StringBuilder auditQuerySb = new StringBuilder();
            auditQuerySb.append("select log.repositoryId,log.eventId,log.docLifeCycle,log.eventDate,log.docPath,log.docUUID from LogEntry log where ");
            if (!allRepositories) {
                auditQuerySb.append("log.repositoryId = '%s' and ");
            }
            auditQuerySb.append("(");
            auditQuerySb.append("log.category = 'eventDocumentCategory' and (log.eventId = 'documentCreated' or log.eventId = 'documentModified' or log.eventId = 'documentMoved') ");
            auditQuerySb.append("or ");
            auditQuerySb.append("log.category = 'eventLifeCycleCategory' and log.eventId = 'lifecycle_transition_event' ");
            auditQuerySb.append("or ");
            auditQuerySb.append("log.category = 'nuxeoDriveCategory'");
            auditQuerySb.append(") ");
            auditQuerySb.append("and (%s) ");
            auditQuerySb.append("and (%s) ");
            auditQuerySb.append("order by log.repositoryId asc, log.eventDate desc");

            String auditQuery;
            if (!allRepositories) {
                String repositoryName = session.getRepositoryName();
                auditQuery = String.format(auditQuerySb.toString(),
                        repositoryName, getRootPathClause(rootPaths),
                        getDateClause(lastSuccessfulSyncDate, syncDate));
            } else {
                auditQuery = String.format(auditQuerySb.toString(),
                        getRootPathClause(rootPaths),
                        getDateClause(lastSuccessfulSyncDate, syncDate));
            }
            log.debug("Querying audit logs for document changes: " + auditQuery);

            List<Object[]> queryResult = (List<Object[]>) auditService.nativeQuery(
                    auditQuery, 1, limit);
            if (queryResult.size() >= limit) {
                throw new TooManyChangesException(
                        "Too many document changes found in the audit logs.");
            }
            for (Object[] auditEntry : queryResult) {
                String repositoryId = (String) auditEntry[0];
                String eventId = (String) auditEntry[1];
                String docLifeCycleState = (String) auditEntry[2];
                Long eventDate = ((Timestamp) auditEntry[3]).getTime();
                String docPath = (String) auditEntry[4];
                String docUuid = (String) auditEntry[5];
                changes.add(new FileSystemItemChange(repositoryId, eventId,
                        docLifeCycleState, eventDate, docPath, docUuid));
            }
        }
        return changes;
    }

    protected String getRootPathClause(Set<String> rootPaths) {
        StringBuilder rootPathClause = new StringBuilder();
        for (String rootPath : rootPaths) {
            if (rootPathClause.length() > 0) {
                rootPathClause.append(" or ");
            }
            rootPathClause.append(String.format("log.docPath like '%s%%'",
                    rootPath));
        }
        return rootPathClause.toString();
    }

    // Round dates to the lower second to ensure consistency
    // in the case of databases that don't support milliseconds
    protected String getDateClause(long lastSuccessfulSyncDate, long syncDate) {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("log.eventDate >= '%s' and log.eventDate < '%s'",
                sdf.format(new Date(lastSuccessfulSyncDate)),
                sdf.format(new Date(syncDate)));
    }

}
