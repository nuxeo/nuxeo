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

public class LogEntryProvider  {

    private static final Log log = LogFactory.getLog(LogEntryProvider.class);

    protected final EntityManager em;

    private LogEntryProvider(EntityManager em) {
        this.em = em;
    }

    public static LogEntryProvider createProvider(EntityManager em) {
        return new LogEntryProvider(em);
    }

    protected void doPersist(LogEntry entry) {
        em.persist(entry);
    }

    protected List<LogEntry> doPublish(List<LogEntry> entries) {
        return entries;
    }

    protected LogEntry doPublish(LogEntry entry) {
        return entry;
    }

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
        if (log.isDebugEnabled()) log.debug("getLogEntriesFor() UUID=" + uuid);
        Query query = em.createNamedQuery("LogEntry.findByDocument");
        query.setParameter("docUUID", uuid);
        return doPublish(query.getResultList());
    }


    @SuppressWarnings("unchecked")
    @Deprecated
    public List<LogEntry> getLogEntriesFor(String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        if (log.isDebugEnabled()) log.debug("getLogEntriesFor() UUID=" + uuid);

        if (filterMap == null) {
            if (log.isWarnEnabled()) log.warn("filter map is null");
            filterMap = new HashMap<String,FilterMapEntry>();
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

    public LogEntry getLogEntryByID(long id)  {
        if (log.isDebugEnabled()) log.debug("getLogEntriesFor() logID=" + id);
        return doPublish(em.find(LogEntry.class, id));
    }

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

    @SuppressWarnings("unchecked")
    public List<LogEntry> queryLogs(String[] eventIds, String dateRange) {
        if (eventIds == null || eventIds.length == 0) {
            throw new IllegalArgumentException("You must give a not null eventId");
        }
        if (log.isDebugEnabled()) log.debug("queryLogs() whereClause=" + eventIds);

        Date limit;
        try {
            limit = DateRangeParser.parseDateRangeQuery(new Date(), dateRange);
        } catch (AuditQueryException aqe) {
            throw new AuditRuntimeException(
                    "Wrong date range query. Query was " + dateRange, aqe);
        }

        String inClause = "(";
        for (String eventId : eventIds) {
            inClause += "'" + eventId + "',";
        }
        inClause = inClause.substring(0, inClause.length() - 1);
        inClause += ")";
        Query query = em.createQuery("from LogEntry log"
                + " where log.eventId in " + inClause
                + " AND log.eventDate >= :limit"
                + " ORDER BY log.eventDate DESC");
        query.setParameter("limit", limit);

        return doPublish(query.getResultList());
    }

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
    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String category, String path, int pageNb, int pageSize) {
        if (eventIds == null) {
            throw new IllegalArgumentException("eventIds should be provided");
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
        if (category != null && !"".equals(category.trim())) {
            queryString.append(" log.category =:category ");
            queryString.append(" AND ");
        }

        if (path != null && !"".equals(path.trim())) {
            queryString.append(" log.docPath LIKE '").append(path).append("%'");
            queryString.append(" AND ");
        }

        queryString.append(" log.eventDate >= :limit");
        queryString.append(" ORDER BY log.eventDate DESC");

        Query query = em.createQuery(queryString.toString());

        if (category != null) {
            query.setParameter("category", category);
        }
        query.setParameter("limit", limit);

        if (pageNb > 1) {
            query.setFirstResult((pageNb - 1) * pageSize);
        }
        query.setMaxResults(pageSize);

        return doPublish(query.getResultList());
    }

    @SuppressWarnings("unchecked")
    public int removeEntries(String eventId, String pathPattern) {
        // TODO extended infos cascade delete does not work using HQL, so we have to delete each
        // entry by hand.
        Query query = em.createNamedQuery("LogEntry.findByEventIdAndPath");
        query.setParameter("eventId", eventId);
        query.setParameter("pathPattern", pathPattern + "%");
        int count = 0;
        for (LogEntry entry:(List<LogEntry>)query.getResultList()) {
            em.remove(entry);
            count += 1;
        }
        if (log.isDebugEnabled()) log.debug("removed " + count + " entries from "
                + pathPattern);
        return count;
    }

}
