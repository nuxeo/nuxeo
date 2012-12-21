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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemChangeFinder;
import org.nuxeo.drive.service.NuxeoDriveEvents;
import org.nuxeo.drive.service.TooManyChangesException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of {@link FileSystemChangeFinder} using the
 * {@link AuditReader}.
 *
 * @author Antoine Taillefer
 */
public class AuditChangeFinder implements FileSystemChangeFinder {

    private static final long serialVersionUID = 1963018967324857522L;

    private static final Log log = LogFactory.getLog(AuditChangeFinder.class);

    @Override
    public List<FileSystemItemChange> getFileSystemChanges(CoreSession session,
            Set<String> rootPaths, long lastSuccessfulSyncDate, long syncDate,
            int limit) throws ClientException, TooManyChangesException {
        String principalName = session.getPrincipal().getName();
        List<FileSystemItemChange> changes = new ArrayList<FileSystemItemChange>();

        // Find changes from the log under active roots
        if (!rootPaths.isEmpty()) {
            AuditReader auditService = Framework.getLocalService(AuditReader.class);
            StringBuilder auditQuerySb = new StringBuilder();
            auditQuerySb.append("log.repositoryId = '%s' and ");
            auditQuerySb.append("(");
            auditQuerySb.append("log.category = 'eventDocumentCategory' and (log.eventId = 'documentCreated' or log.eventId = 'documentModified' or log.eventId = 'documentMoved') ");
            auditQuerySb.append("or ");
            auditQuerySb.append("log.category = 'eventLifeCycleCategory' and log.eventId = 'lifecycle_transition_event' and log.docLifeCycle != 'deleted' ");
            auditQuerySb.append("or ");
            auditQuerySb.append("log.category = '%s'");
            auditQuerySb.append(") ");
            auditQuerySb.append("and (%s) ");
            auditQuerySb.append("and (%s) ");
            auditQuerySb.append("order by log.repositoryId asc, log.eventDate desc");

            String repositoryName = session.getRepositoryName();
            String auditQuery = String.format(auditQuerySb.toString(),
                    repositoryName, NuxeoDriveEvents.EVENT_CATEGORY,
                    getRootPathClause(rootPaths),
                    getJPADateClause(lastSuccessfulSyncDate, syncDate));
            log.debug("Querying audit logs for document changes: " + auditQuery);

            List<LogEntry> entries = auditService.nativeQueryLogs(auditQuery, 1, limit);
            if (entries.size() >= limit) {
                throw new TooManyChangesException(
                        "Too many changes found in the audit logs.");
            }
            for (LogEntry entry: entries) {
                ExtendedInfo impactedUserInfo = entry.getExtendedInfos().get("impactedUserName");
                if (impactedUserInfo != null && !principalName.equals(impactedUserInfo.getValue(String.class))) {
                    // This change does not impact the current user, skip.
                    continue;
                }
                FileSystemItemChange change = new FileSystemItemChange(entry.getRepositoryId(), entry.getEventId(),
                        entry.getDocLifeCycle(), entry.getEventDate().getTime(), entry.getDocPath(), entry.getDocUUID());
                ExtendedInfo fsItemInfo = entry.getExtendedInfos().get(
                        "fileSystemItem");
                if (fsItemInfo != null) {
                    change.setFileSystemItem(fsItemInfo.getValue(FileSystemItem.class));
                }
                changes.add(change);
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
    protected String getJPADateClause(long lastSuccessfulSyncDate, long syncDate) {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("log.eventDate >= '%s' and log.eventDate < '%s'",
                sdf.format(new Date(lastSuccessfulSyncDate)),
                sdf.format(new Date(syncDate)));
    }

}
