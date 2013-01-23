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
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.drive.service.TooManyChangesException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
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
            Set<IdRef> lastActiveRootRefs, SynchronizationRoots activeRoots,
            long lastSuccessfulSyncDate, long syncDate, int limit)
            throws ClientException, TooManyChangesException {
        String principalName = session.getPrincipal().getName();
        List<FileSystemItemChange> changes = new ArrayList<FileSystemItemChange>();

        // Find changes from the log under active roots or events that are
        // linked to the unregistration or deletion of formerly synchronized
        // roots
        if (!activeRoots.paths.isEmpty() || !lastActiveRootRefs.isEmpty()) {
            AuditReader auditService = Framework.getLocalService(AuditReader.class);
            StringBuilder auditQuerySb = new StringBuilder();
            auditQuerySb.append("log.repositoryId = '");
            auditQuerySb.append(session.getRepositoryName());
            auditQuerySb.append("' and ");
            auditQuerySb.append("(");
            if (!activeRoots.paths.isEmpty()) {
                // detect changes under the currently active roots for the
                // current user
                auditQuerySb.append("(");
                auditQuerySb.append("log.category = 'eventDocumentCategory'");
                auditQuerySb.append(" and (log.eventId = 'documentCreated' or log.eventId = 'documentModified' or log.eventId = 'documentMoved')");
                auditQuerySb.append(" or ");
                auditQuerySb.append("log.category = 'eventLifeCycleCategory'");
                auditQuerySb.append(" and log.eventId = 'lifecycle_transition_event' and log.docLifeCycle != 'deleted' ");
                auditQuerySb.append(" or ");
                auditQuerySb.append("log.category = '");
                auditQuerySb.append(NuxeoDriveEvents.EVENT_CATEGORY);
                auditQuerySb.append("'");
                auditQuerySb.append(") and (");
                auditQuerySb.append(getCurrentRootFilteringClause(activeRoots.paths));
                auditQuerySb.append(")");
            }
            if (!activeRoots.paths.isEmpty() && !lastActiveRootRefs.isEmpty()) {
                auditQuerySb.append("or ");
            }
            if (!lastActiveRootRefs.isEmpty()) {
                // detect root unregistrition changes for the roots previously
                // seen by the current user
                auditQuerySb.append("log.category = '");
                auditQuerySb.append(NuxeoDriveEvents.EVENT_CATEGORY);
                auditQuerySb.append("' and ");
                auditQuerySb.append(getLastRootFilteringClause(lastActiveRootRefs));
            }
            auditQuerySb.append(") and (");
            auditQuerySb.append(getJPADateClause(lastSuccessfulSyncDate,
                    syncDate));
            auditQuerySb.append(") order by log.repositoryId asc, log.eventDate desc");

            String auditQuery = auditQuerySb.toString();
            log.debug("Querying audit logs for document changes: " + auditQuery);
            List<LogEntry> entries = auditService.nativeQueryLogs(auditQuery,
                    1, limit);
            if (entries.size() >= limit) {
                throw new TooManyChangesException(
                        "Too many changes found in the audit logs.");
            }
            for (LogEntry entry : entries) {
                ExtendedInfo impactedUserInfo = entry.getExtendedInfos().get(
                        "impactedUserName");
                if (impactedUserInfo != null
                        && !principalName.equals(impactedUserInfo.getValue(String.class))) {
                    // This change does not impact the current user, skip.
                    continue;
                }
                ExtendedInfo fsIdInfo = entry.getExtendedInfos().get(
                        "fileSystemItemId");
                if (fsIdInfo != null) {
                    // This document has been deleted and we just know the
                    // fileSystem Id and Name
                    String fsId = fsIdInfo.getValue(String.class);
                    String fsName = entry.getExtendedInfos().get(
                            "fileSystemItemName").getValue(String.class);
                    FileSystemItemChange change = new FileSystemItemChange(
                            entry.getEventId(), entry.getEventDate().getTime(),
                            entry.getRepositoryId(), entry.getDocUUID(), fsId,
                            fsName);
                    changes.add(change);
                } else {
                    DocumentRef docRef = new IdRef(entry.getDocUUID());
                    if (!session.exists(docRef)) {
                        // deleted documents are mapped to deleted file
                        // system items in a separate event: no need to try
                        // to propagate this event.
                        // TODO: find a consistent way to map ACL removals as
                        // filesystem deletion change
                        continue;
                    }
                    DocumentModel doc = session.getDocument(docRef);
                    // TODO: check the facet, last root change and list of roots
                    // to have a special handling for the roots.
                    FileSystemItem fsItem = doc.getAdapter(FileSystemItem.class);
                    if (fsItem != null) {
                        FileSystemItemChange change = new FileSystemItemChange(
                                entry.getEventId(),
                                entry.getEventDate().getTime(),
                                entry.getRepositoryId(), entry.getDocUUID(),
                                fsItem);
                        changes.add(change);
                    }
                    // non-adaptable documents are ignored
                }
            }
        }
        return changes;
    }

    protected String getCurrentRootFilteringClause(Set<String> rootPaths) {
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

    protected String getLastRootFilteringClause(Set<IdRef> lastActiveRootRefs) {
        StringBuilder rootPathClause = new StringBuilder();
        if (!lastActiveRootRefs.isEmpty()) {
            rootPathClause.append("log.docUUID in (");
            for (IdRef ref : lastActiveRootRefs) {
                rootPathClause.append("'");
                rootPathClause.append(ref.toString());
                rootPathClause.append("'");
            }
            rootPathClause.append(")");
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

    /**
     * Map the backing document to a FileSystemItem using the adapters when
     * possible and store the mapping in the FileSystemItemChange instance. If
     * not possible (because of missing permissions for instance), skip the
     * change.
     */
    protected boolean adaptDocument(FileSystemItemChange change,
            CoreSession session, SynchronizationRoots synchronizationRoots)
            throws ClientException {
        IdRef ref = new IdRef(change.getDocUuid());
        try {
            DocumentModel doc = session.getDocument(ref);
            // TODO: check the facet, last root change and list of roots to have
            // a special handling for the roots.
            FileSystemItem fsItem = doc.getAdapter(FileSystemItem.class);
            if (fsItem == null) {
                return false;
            }
            change.setFileSystemItem(fsItem);
            return true;
        } catch (DocumentSecurityException e) {
            // This event matches a document that is not visible by the
            // current user, skip it.
            // TODO: how to detect ACL removal to map those as
            return false;
        }
    }

}
