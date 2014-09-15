/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */

package org.nuxeo.ecm.platform.audit.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.query.AuditQueryException;
import org.nuxeo.ecm.platform.audit.api.query.DateRangeParser;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;

public class EMLogEntryProvider implements LogEntryProvider {

    private static final Log log = LogFactory.getLog(EMLogEntryProvider.class);

    protected final EntityManager em;

    private EMLogEntryProvider(EntityManager em) {
        this.em = em;
    }

    public static EMLogEntryProvider createProvider(EntityManager em) {
        return new EMLogEntryProvider(em);
    }

    protected void doPersist(LogEntry entry) {
        // Set the log date in java right before saving to the database. We
        // cannot set a static column definition to
        // "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" as MS SQL Server does not
        // support the TIMESTAMP column type and generating a dynamic
        // persistence configuration that would depend on the database is too
        // complicated.
        entry.setLogDate(new Date());
        em.persist(entry);
    }

    protected List<?> doPublishIfEntries(List<?> entries) {
        if (entries == null || entries.size() == 0) {
            return entries;
        }
        Object entry = entries.get(0);
        if (entry instanceof LogEntry) {
            for (Object logEntry : entries) {
                doPublish((LogEntry) logEntry);
            }
        }
        return entries;
    }

    protected List<LogEntry> doPublish(List<LogEntry> entries) {
        for (LogEntry entry : entries) {
            doPublish(entry);
        }
        return entries;
    }

    protected LogEntry doPublish(LogEntry entry) {
        if (entry.getExtendedInfos() != null) {
            entry.getExtendedInfos().size(); // force lazy loading
        }
        return entry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.audit.service.LogEntryProvider#addLogEntry(org
     * .nuxeo.ecm.platform.audit.api.LogEntry)
     */
    @Override
    public void addLogEntry(LogEntry entry) {
        doPersist(entry);
    }

    public void addLogEntries(List<LogEntry> entries) {
        for (LogEntry entry : entries) {
            doPersist(entry);
        }
    }

    @SuppressWarnings("unchecked")
    public List<LogEntry> getLogEntriesFor(String uuid) {
        if (log.isDebugEnabled()) {
            log.debug("getLogEntriesFor() UUID=" + uuid);
        }
        Query query = em.createNamedQuery("LogEntry.findByDocument");
        query.setParameter("docUUID", uuid);
        return doPublish(query.getResultList());
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public List<LogEntry> getLogEntriesFor(String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        if (log.isDebugEnabled()) {
            log.debug("getLogEntriesFor() UUID=" + uuid);
        }

        if (filterMap == null) {
            filterMap = new HashMap<String, FilterMapEntry>();
        }

        StringBuilder queryStr = new StringBuilder();
        queryStr.append(" FROM LogEntry log WHERE log.docUUID=:uuid ");

        Set<String> filterMapKeySet = filterMap.keySet();
        for (String currentKey : filterMapKeySet) {
            FilterMapEntry currentFilterMapEntry = filterMap.get(currentKey);
            String currentOperator = currentFilterMapEntry.getOperator();
            String currentQueryParameterName = currentFilterMapEntry.getQueryParameterName();
            String currentColumnName = currentFilterMapEntry.getColumnName();

            if ("LIKE".equals(currentOperator)) {
                queryStr.append(" AND log.").append(currentColumnName).append(
                        " LIKE :").append(currentQueryParameterName).append(" ");
            } else {
                queryStr.append(" AND log.").append(currentColumnName).append(
                        currentOperator).append(":").append(
                        currentQueryParameterName).append(" ");
            }
        }

        if (doDefaultSort) {
            queryStr.append(" ORDER BY log.eventDate DESC");
        }

        Query query = em.createQuery(queryStr.toString());

        query.setParameter("uuid", uuid);

        for (String currentKey : filterMapKeySet) {
            FilterMapEntry currentFilterMapEntry = filterMap.get(currentKey);
            String currentOperator = currentFilterMapEntry.getOperator();
            String currentQueryParameterName = currentFilterMapEntry.getQueryParameterName();
            Object currentObject = currentFilterMapEntry.getObject();

            if ("LIKE".equals(currentOperator)) {
                query.setParameter(currentQueryParameterName, "%"
                        + currentObject + "%");
            } else {
                query.setParameter(currentQueryParameterName, currentObject);
            }
        }

        return doPublish(query.getResultList());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.audit.service.LogEntryProvider#getLogEntryByID
     * (long)
     */
    public LogEntry getLogEntryByID(long id) {
        if (log.isDebugEnabled()) {
            log.debug("getLogEntriesFor() logID=" + id);
        }
        return doPublish(em.find(LogEntryImpl.class, id));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.audit.service.LogEntryProvider#nativeQueryLogs
     * (java.lang.String, int, int)
     */
    @SuppressWarnings("unchecked")
    public List<LogEntry> nativeQueryLogs(String whereClause, int pageNb,
            int pageSize) {
        Query query = em.createQuery("from LogEntry log where " + whereClause);
        if (pageNb > 1) {
            query.setFirstResult((pageNb - 1) * pageSize);
        }
        query.setMaxResults(pageSize);
        return doPublish(query.getResultList());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.audit.service.LogEntryProvider#nativeQuery(java
     * .lang.String, int, int)
     */
    public List<?> nativeQuery(String queryString, int pageNb, int pageSize) {
        Query query = em.createQuery(queryString);
        if (pageNb > 1) {
            query.setFirstResult((pageNb - 1) * pageSize);
        }
        query.setMaxResults(pageSize);
        return doPublishIfEntries(query.getResultList());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.audit.service.LogEntryProvider#nativeQuery(java
     * .lang.String, java.util.Map, int, int)
     */
    public List<?> nativeQuery(String queryString, Map<String, Object> params,
            int pageNb, int pageSize) {
        if (pageSize <= 0) {
            pageSize = 1000;
        }
        Query query = em.createQuery(queryString);
        for (Entry<String, Object> en : params.entrySet()) {
            query.setParameter(en.getKey(), en.getValue());
        }
        if (pageNb > 1) {
            query.setFirstResult((pageNb - 1) * pageSize);
        }
        query.setMaxResults(pageSize);
        return doPublishIfEntries(query.getResultList());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.audit.service.LogEntryProvider#queryLogs(java.
     * lang.String[], java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public List<LogEntry> queryLogs(String[] eventIds, String dateRange) {
        Date limit;
        try {
            limit = DateRangeParser.parseDateRangeQuery(new Date(), dateRange);
        } catch (AuditQueryException aqe) {
            throw new AuditRuntimeException(
                    "Wrong date range query. Query was " + dateRange, aqe);
        }

        String queryStr = "";
        if (eventIds == null || eventIds.length == 0) {
            queryStr = "from LogEntry log" + " where log.eventDate >= :limit"
                    + " ORDER BY log.eventDate DESC";
        } else {
            String inClause = "(";
            for (String eventId : eventIds) {
                inClause += "'" + eventId + "',";
            }
            inClause = inClause.substring(0, inClause.length() - 1);
            inClause += ")";

            queryStr = "from LogEntry log" + " where log.eventId in "
                    + inClause + " AND log.eventDate >= :limit"
                    + " ORDER BY log.eventDate DESC";
        }

        if (log.isDebugEnabled()) {
            log.debug("queryLogs() =" + queryStr);
        }
        Query query = em.createQuery(queryStr);
        query.setParameter("limit", limit);

        return doPublish(query.getResultList());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.audit.service.LogEntryProvider#queryLogsByPage
     * (java.lang.String[], java.lang.String, java.lang.String[],
     * java.lang.String, int, int)
     */
    public List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange,
            String[] categories, String path, int pageNb, int pageSize)
            throws AuditException {
        Date limit = null;
        try {
            limit = DateRangeParser.parseDateRangeQuery(new Date(), dateRange);
        } catch (AuditQueryException aqe) {
            throw new AuditException("Wrong date range query. Query was "
                    + dateRange, aqe);
        }
        return queryLogsByPage(eventIds, limit, categories, path, pageNb,
                pageSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.audit.service.LogEntryProvider#queryLogsByPage
     * (java.lang.String[], java.util.Date, java.lang.String[],
     * java.lang.String, int, int)
     */
    @SuppressWarnings("unchecked")
    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String[] categories, String path, int pageNb, int pageSize) {
        if (eventIds == null) {
            eventIds = new String[0];
        }
        if (categories == null) {
            categories = new String[0];
        }

        StringBuilder queryString = new StringBuilder();

        queryString.append("from LogEntry log where ");

        if (eventIds.length > 0) {
            String inClause = "(";
            for (String eventId : eventIds) {
                inClause += "'" + eventId + "',";
            }
            inClause = inClause.substring(0, inClause.length() - 1);
            inClause += ")";

            queryString.append(" log.eventId IN ").append(inClause);
            queryString.append(" AND ");
        }
        if (categories.length > 0) {
            String inClause = "(";
            for (String cat : categories) {
                inClause += "'" + cat + "',";
            }
            inClause = inClause.substring(0, inClause.length() - 1);
            inClause += ")";
            queryString.append(" log.category IN ").append(inClause);
            queryString.append(" AND ");
        }

        if (path != null && !"".equals(path.trim())) {
            queryString.append(" log.docPath LIKE '").append(path).append("%'");
            queryString.append(" AND ");
        }

        queryString.append(" log.eventDate >= :limit");
        queryString.append(" ORDER BY log.eventDate DESC");

        Query query = em.createQuery(queryString.toString());

        query.setParameter("limit", limit);

        if (pageNb > 1) {
            query.setFirstResult((pageNb - 1) * pageSize);
        }
        query.setMaxResults(pageSize);

        return doPublish(query.getResultList());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.audit.service.LogEntryProvider#removeEntries(java
     * .lang.String, java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public int removeEntries(String eventId, String pathPattern) {
        // TODO extended info cascade delete does not work using HQL, so we
        // have to delete each
        // entry by hand.
        Query query = em.createNamedQuery("LogEntry.findByEventIdAndPath");
        query.setParameter("eventId", eventId);
        query.setParameter("pathPattern", pathPattern + "%");
        int count = 0;
        for (LogEntry entry : (List<LogEntry>) query.getResultList()) {
            em.remove(entry);
            count += 1;
        }
        if (log.isDebugEnabled()) {
            log.debug("removed " + count + " entries from " + pathPattern);
        }
        return count;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.platform.audit.service.LogEntryProvider#countEventsById
     * (java.lang.String)
     */
    public Long countEventsById(String eventId) {
        Query query = em.createNamedQuery("LogEntry.countEventsById");
        query.setParameter("eventId", eventId);
        return (Long) query.getSingleResult();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#findEventIds()
     */
    @SuppressWarnings("unchecked")
    public List<String> findEventIds() {
        Query query = em.createNamedQuery("LogEntry.findEventIds");
        return (List<String>) query.getResultList();
    }

}
