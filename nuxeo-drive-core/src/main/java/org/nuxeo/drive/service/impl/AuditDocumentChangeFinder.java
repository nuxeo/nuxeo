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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.service.DocumentChangeFinder;
import org.nuxeo.drive.service.TooManyDocumentChangesException;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of {@link DocumentChangeFinder} using the {@link AuditReader}.
 *
 * @author Antoine Taillefer
 */
public class AuditDocumentChangeFinder implements DocumentChangeFinder {

    private static final long serialVersionUID = 1963018967324857522L;

    private static final Log log = LogFactory.getLog(AuditDocumentChangeFinder.class);

    protected String blackListedDocTypes;

    public AuditDocumentChangeFinder() {
        initBlackListedDocTypes();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AuditDocumentChange> getDocumentChanges(String repoName,
            Set<String> rootPaths, Calendar lastSuccessfulSync, int limit)
            throws TooManyDocumentChangesException {

        List<AuditDocumentChange> docChanges = new ArrayList<AuditDocumentChange>();
        if (!rootPaths.isEmpty()) {
            AuditReader auditService = Framework.getLocalService(AuditReader.class);
            StringBuilder auditQuerySb = new StringBuilder();
            auditQuerySb.append("select log.eventId,log.docLifeCycle,log.eventDate,log.docPath,log.docUUID from LogEntry log ");
            auditQuerySb.append("where log.repositoryId = '%s' ");
            auditQuerySb.append("and (");
            auditQuerySb.append("log.category = 'eventDocumentCategory' and (log.eventId = 'documentCreated' or log.eventId = 'documentModified' or log.eventId = 'documentMoved') ");
            auditQuerySb.append("or ");
            auditQuerySb.append("log.category = 'eventLifeCycleCategory' and log.eventId = 'lifecycle_transition_event' ");
            auditQuerySb.append("or ");
            auditQuerySb.append("log.category = 'nuxeoDriveCategory'");
            auditQuerySb.append(") ");
            auditQuerySb.append("and log.docType not in (%s) ");
            auditQuerySb.append("and (%s) ");
            auditQuerySb.append("and log.eventDate > '%s' ");
            auditQuerySb.append("order by log.eventDate desc");

            String auditQuery = String.format(auditQuerySb.toString(),
                    repoName, blackListedDocTypes,
                    getRootPathClause(rootPaths),
                    getLastSuccessfulSyncDate(lastSuccessfulSync));
            log.debug("Querying audit logs for document changes: " + auditQuery);

            List<Object[]> queryResult = (List<Object[]>) auditService.nativeQuery(
                    auditQuery, 1, limit);
            if (queryResult.size() >= limit) {
                throw new TooManyDocumentChangesException(
                        "Too many document changes found in the audit logs.");
            }
            for (Object[] auditEntry : queryResult) {
                String eventId = (String) auditEntry[0];
                String docLifeCycleState = (String) auditEntry[1];
                Calendar eventDate = Calendar.getInstance();
                eventDate.setTimeInMillis(((Timestamp) auditEntry[2]).getTime());
                String docPath = (String) auditEntry[3];
                String docUuid = (String) auditEntry[4];
                docChanges.add(new AuditDocumentChange(eventId,
                        docLifeCycleState, eventDate, docPath, docUuid));
            }
        }
        return docChanges;
    }

    public String getBlackListedDocTypes() {
        return blackListedDocTypes;
    }

    public void setBlackListedDocTypes(String blackListedDocTypes) {
        this.blackListedDocTypes = blackListedDocTypes;
    }

    protected void initBlackListedDocTypes() {
        StringBuilder sb = new StringBuilder();
        for (BlackListedDocTypesEnum blackListedDocType : BlackListedDocTypesEnum.values()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append("'");
            sb.append(blackListedDocType.name());
            sb.append("'");
        }
        blackListedDocTypes = sb.toString();
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

    protected String getLastSuccessfulSyncDate(Calendar lastSuccessfulSync) {
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        return sdf.format(new Date(lastSuccessfulSync.getTimeInMillis()));
    }

}
