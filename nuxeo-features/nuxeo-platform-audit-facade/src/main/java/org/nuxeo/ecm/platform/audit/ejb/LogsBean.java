/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id:LogsBean.java 1583 2006-08-04 10:26:40Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.ejb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.audit.NXAudit;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryBase;
import org.nuxeo.ecm.platform.audit.api.LogEntryFactory;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.api.query.AuditQueryException;
import org.nuxeo.ecm.platform.audit.api.query.DateRangeParser;
import org.nuxeo.ecm.platform.audit.api.remote.LogsRemote;
import org.nuxeo.ecm.platform.audit.ejb.local.LogsLocal;
import org.nuxeo.runtime.api.Framework;

/**
 * Stateful bean allowing to query the logs.
 *
 * <p>
 * This class takes advantage of EJBQL.
 * </p>
 *
 * :XXX: http://jira.nuxeo.org/browse/NXP-514
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Stateless
@Local(LogsLocal.class)
@Remote(LogsRemote.class)
public class LogsBean implements Logs {

    private static final long serialVersionUID = -9120913062582569871L;

    private static final Log log = LogFactory.getLog(LogsBean.class);

    @PersistenceContext(unitName = "NXAudit")
    private EntityManager em;

    public void addLogEntries(List<LogEntry> entries) throws AuditException {
        try {
            for (LogEntry entry : entries) {
                em.persist(entry);
            }
        } catch (Exception e) {
            throw new AuditException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<LogEntry> getLogEntriesFor(String uuid) throws AuditException {
        log.debug("getLogEntriesFor() UUID=" + uuid);
        Class<LogEntry> klass = getLogEntryClass();
        try {
            Query query = em.createQuery("from " + klass.getSimpleName()
                    + " log where log.docUUID=:uuid"
                    + " ORDER BY log.eventDate DESC");
            query.setParameter("uuid", uuid);
            // :XXX: paging here
            List<LogEntry> returned = new ArrayList<LogEntry>();
            for (Object object : query.getResultList()) {
                LogEntry entry = (LogEntry) object;
                returned.add(getLogEntryFactory().createLogEntryBase(entry));
            }
            return returned;

        } catch (Exception e) {
            throw new AuditException(e);
        }
    }

    public List<LogEntry> getLogEntriesFor(String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort)
            throws AuditException {
        log.debug("getLogEntriesFor() UUID=" + uuid);
        Class<LogEntry> klass = getLogEntryClass();
        try {
            StringBuilder queryStr = new StringBuilder();
            queryStr.append(" FROM ").append(klass.getSimpleName()).append(
                    " log WHERE log.docUUID=:uuid ");

            if (filterMap != null) {
                Set<String> filterMapKeySet = filterMap.keySet();
                for (String currentKey : filterMapKeySet) {
                    FilterMapEntry currentFilterMapEntry = filterMap.get(currentKey);
                    String currentOperator = currentFilterMapEntry.getOperator();
                    String currentQueryParameterName = currentFilterMapEntry.getQueryParameterName();
                    String currentColumnName = currentFilterMapEntry.getColumnName();

                    if ("LIKE".equals(currentOperator)) {
                        queryStr.append(" AND log.").append(currentColumnName).append(
                                " LIKE :").append(currentQueryParameterName).append(
                                " ");
                    } else {
                        queryStr.append(" AND log.").append(currentColumnName).append(
                                currentOperator).append(":").append(
                                currentQueryParameterName).append(" ");
                    }
                }
            }
            if (doDefaultSort) {
                queryStr.append(" ORDER BY log.eventDate DESC");
            }

            Query query = em.createQuery(queryStr.toString());
            query.setParameter("uuid", uuid);
            if (filterMap != null) {
                Set<String> filterMapKeySet = filterMap.keySet();
                for (String currentKey : filterMapKeySet) {
                    FilterMapEntry currentFilterMapEntry = filterMap.get(currentKey);
                    String currentOperator = currentFilterMapEntry.getOperator();
                    String currentQueryParameterName = currentFilterMapEntry.getQueryParameterName();
                    Object currentObject = currentFilterMapEntry.getObject();

                    if ("LIKE".equals(currentOperator)) {
                        query.setParameter(currentQueryParameterName, "%"
                                + currentObject + "%");
                    } else {
                        query.setParameter(currentQueryParameterName,
                                currentObject);
                    }
                }
            }

            List<LogEntry> returned = new ArrayList<LogEntry>();
            for (Object object : query.getResultList()) {
                LogEntry entry = (LogEntry) object;
                returned.add(getLogEntryFactory().createLogEntryBase(entry));
            }

            return returned;

        } catch (Exception e) {
            throw new AuditException(e);
        }
    }

    /**
     * Returns the audit core service.
     *
     * @return a <code>NXAuditEvents</code> instance.
     */
    private NXAuditEvents getAuditService() {
        return NXAudit.getNXAuditEventsService();
    }

    /**
     * Returns the log entry factory registered on the audit service.
     *
     * @return the log entry factory registered on the audit service.
     */
    private LogEntryFactory getLogEntryFactory() {
        NXAuditEvents service = getAuditService();
        return service.getLogEntryFactory();
    }

    private Class<LogEntry> getLogEntryClass() throws AuditException {
        LogEntryFactory factory = getLogEntryFactory();
        if (factory == null) {
            throw new AuditException(
                    "Cannot find log entry factory...Check configuration");
        }
        return factory.getLogEntryClass();
    }

    public List<LogEntry> nativeQueryLogs(String whereClause, int pageNb,
            int pageSize) throws AuditException {

        Class<LogEntry> klass = getLogEntryClass();
        List<LogEntry> results = new ArrayList<LogEntry>();

        Query query = em.createQuery("from " + klass.getSimpleName()
                + " log where " + whereClause);
        if (pageNb > 1) {
            query.setFirstResult((pageNb - 1) * pageSize + 1);
        }

        query.setMaxResults(pageSize);
        results.addAll(query.getResultList());

        List<LogEntry> returned = new ArrayList<LogEntry>();
        for (LogEntry entry : results) {
            returned.add(getLogEntryFactory().createLogEntryBase(entry));
        }
        return returned;
    }

    @SuppressWarnings("unchecked")
    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String category, String path, int pageNb, int pageSize)
            throws AuditException {

        // :FIXME: This is not working remotelty since the LogEntryImpl returned
        // is not within the api package.

        Class<LogEntry> klass = getLogEntryClass();

        List<LogEntry> results = new ArrayList<LogEntry>();

        String inClause = null;

        StringBuffer queryString = new StringBuffer();

        queryString.append("from " + klass.getSimpleName() + " log where ");

        Query query = null;
        if (eventIds != null) {
            inClause = "(";
            for (String eventId : eventIds) {
                inClause = inClause + "'" + eventId + "',";
            }
            inClause = inClause.substring(0, inClause.length() - 1);
            inClause = inClause + ")";

            queryString.append(" log.eventId IN " + inClause);
            queryString.append(" AND ");
        }
        if (category != null && !"".equals(category.trim())) {
            queryString.append(" log.category =:category ");
            queryString.append(" AND ");
        }

        if (path != null && !"".equals(path.trim())) {
            queryString.append(" log.docPath LIKE '" + path + "%'");
            queryString.append(" AND ");
        }

        queryString.append(" log.eventDate >= :limit");
        queryString.append(" ORDER BY log.eventDate DESC");

        query = em.createQuery(queryString.toString());

        if (category != null) {
            query.setParameter("category", category);
        }
        query.setParameter("limit", limit);

        if (pageNb > 1) {
            query.setFirstResult((pageNb - 1) * pageSize + 1);
        }
        query.setMaxResults(pageSize);
        results.addAll(query.getResultList());

        List<LogEntry> returned = new ArrayList<LogEntry>();
        for (LogEntry entry : results) {
            returned.add(getLogEntryFactory().createLogEntryBase(entry));
        }

        return returned;
    }

    @SuppressWarnings("unchecked")
    public List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange,
            String category, String path, int pageNb, int pageSize)
            throws AuditException {

        Date limit = null;
        try {
            limit = DateRangeParser.parseDateRangeQuery(new Date(), dateRange);
        } catch (AuditQueryException aqe) {
            throw new AuditException("Wrong date range query. Query was "
                    + dateRange, aqe);
        }
        return queryLogsByPage(eventIds, limit, category, path, pageNb,
                pageSize);
    }

    @SuppressWarnings("unchecked")
    public List<LogEntry> queryLogs(String[] eventIds, String dateRange)
            throws AuditException {

        // :FIXME: This is not working remotelty since the LogEntryImpl returned
        // is not within the api package.

        if (eventIds == null || eventIds.length == 0) {
            throw new AuditException("You must give a not null eventId");
        }
        log.debug("queryLogs() whereClause=" + eventIds);
        Class<LogEntry> klass = getLogEntryClass();

        List<LogEntry> results = new ArrayList<LogEntry>();

        Date limit = null;
        try {
            limit = DateRangeParser.parseDateRangeQuery(new Date(), dateRange);
        } catch (AuditQueryException aqe) {
            throw new AuditException("Wrong date range query. Query was "
                    + dateRange, aqe);
        }

        String inClause = "(";
        for (String eventId : eventIds) {
            inClause = inClause + "'" + eventId + "',";
        }
        inClause = inClause.substring(0, inClause.length() - 1);
        inClause = inClause + ")";
        Query query = em.createQuery("from " + klass.getSimpleName()
                + " log where log.eventId in " + inClause
                + " AND log.eventDate >= :limit"
                + " ORDER BY log.eventDate DESC");
        query.setParameter("limit", limit);

        results.addAll(query.getResultList());
        List<LogEntry> returned = new ArrayList<LogEntry>();
        for (LogEntry entry : results) {
            returned.add(getLogEntryFactory().createLogEntryBase(entry));
        }

        return returned;
    }

    @SuppressWarnings("unchecked")
    public LogEntry getLogEntryByID(long id) throws AuditException {
        log.debug("getLogEntriesFor() logID=" + id);
        Class<LogEntry> klass = getLogEntryClass();
        LogEntryBase match = null;
        try {
            Query query = em.createQuery("from " + klass.getSimpleName()
                    + " log where log.id=:id");
            query.setParameter("id", id);

            List<LogEntry> res = query.getResultList();
            if (res != null && res.size() == 1) {
                match = getLogEntryFactory().createLogEntryBase(res.get(0));
            }
        } catch (Exception e) {
            throw new AuditException(e);
        }
        return match;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long syncLogCreationEntries(String repoId, String path,
            Boolean recurs) throws AuditException, ClientException {
        // first delete all creation event for the given path
        // XXX : TODO
        removeOldEntriesBeforeSync("documentCreated", path);
        // now fetch from the core
        CoreSession session;
        RepositoryManager rm;
        try {
            rm = Framework.getService(RepositoryManager.class);
        } catch (Exception e1) {
            throw new AuditException("Unable to get RepositoryManager", e1);
        }
        Repository repo = rm.getRepository(repoId);

        if (repo == null) {
            throw new AuditException("Can not find repository");
        }

        try {
            session = repo.open();
        } catch (Exception e1) {
            throw new AuditException("Can not open repository", e1);
        }

        DocumentRef rootRef = new PathRef(path);
        DocumentModel root = session.getDocument(rootRef);

        return syncNode(session, root, recurs);
    }

    protected long syncNode(CoreSession session, DocumentModel node,
            boolean recurs) throws ClientException, AuditException {
        long nbSynchedEntries = 0;

        List<LogEntry> entries = new ArrayList<LogEntry>();
        List<DocumentModel> folderishChildren = new ArrayList<DocumentModel>();

        entries.add(makeEntryFromDoc(node));

        for (DocumentModel child : session.getChildren(node.getRef())) {
            if (child.isFolder() && recurs) {
                folderishChildren.add(child);
            } else {
                entries.add(makeEntryFromDoc(child));
            }
        }

        // store entries
        addLogEntries(entries);
        nbSynchedEntries += entries.size();
        node = null;
        entries = null;

        if (recurs) {
            for (DocumentModel folderChild : folderishChildren) {
                nbSynchedEntries += syncNode(session, folderChild, recurs);
            }
        }

        return nbSynchedEntries;
    }

    protected LogEntry makeEntryFromDoc(DocumentModel doc) {
        LogEntryBase entry = new LogEntryImpl();
        entry.setDocPath(doc.getPathAsString());
        entry.setDocType(doc.getType());
        entry.setDocUUID(doc.getId());
        entry.setPrincipalName("system");
        entry.setCategory("eventDocumentCategory");
        entry.setEventId("documentCreated");
        entry.setDocLifeCycle("project");
        Calendar creationDate = (Calendar) doc.getProperty("dublincore",
                "created");
        if (creationDate != null) {
            entry.setEventDate(creationDate.getTime());
        }
        return entry;
    }

    protected void removeOldEntriesBeforeSync(String eventId, String pathPattern)
            throws AuditException {
        Class<LogEntry> klass = getLogEntryClass();
        Query query = em.createQuery("DELETE from " + klass.getSimpleName()
                + " log where log.eventId=:eventId"
                + " AND log.docPath LIKE :pathPattern"
                + " ORDER BY log.eventDate DESC");
        query.setParameter("eventId", eventId);
        query.setParameter("pathPattern", pathPattern + "%");
        int nbDeletedEntries = query.executeUpdate();
    }
}
