/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.ecm.platform.audit.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.query.sql.model.Literals;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.query.AuditQueryException;
import org.nuxeo.ecm.platform.audit.api.query.DateRangeParser;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;

public class LogEntryProvider implements BaseLogEntryProvider {

    private static final Log log = LogFactory.getLog(LogEntryProvider.class);

    public static final String LIKE = "LIKE";

    protected final EntityManager em;

    private LogEntryProvider(EntityManager em) {
        this.em = em;
    }

    public static LogEntryProvider createProvider(EntityManager em) {
        return new LogEntryProvider(em);
    }

    public void append(List<LogEntry> entries) {
        entries.forEach(e -> {
            if (em.contains(e)) {
                log.warn("Log entry already exists for id " + e.getId());
            }
            em.merge(e);
        });
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
        entries.forEach(this::doPublish);
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
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#addLogEntry(org
     * .nuxeo.ecm.platform.audit.api.LogEntry)
     */
    @Override
    public void addLogEntry(LogEntry entry) {
        doPersist(entry);
    }

    public void addLogEntries(List<LogEntry> entries) {
        entries.forEach(this::doPersist);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<LogEntry> getLogEntriesFor(String uuid, String repositoryId) {
        if (log.isDebugEnabled()) {
            log.debug("getLogEntriesFor() UUID=" + uuid + " and repositoryId=" + repositoryId);
        }
        Query query = em.createNamedQuery("LogEntry.findByDocumentAndRepository");
        query.setParameter("docUUID", uuid);
        query.setParameter("repositoryId", repositoryId);
        return doPublish(query.getResultList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<LogEntry> getLogEntriesFor(String uuid) {
        if (log.isDebugEnabled()) {
            log.debug("getLogEntriesFor() UUID=" + uuid);
        }
        Query query = em.createNamedQuery("LogEntry.findByDocument");
        query.setParameter("docUUID", uuid);
        return doPublish(query.getResultList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<LogEntry> getLogEntriesFor(String uuid, Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        if (log.isDebugEnabled()) {
            log.debug("getLogEntriesFor() UUID=" + uuid);
        }

        if (filterMap == null) {
            filterMap = new HashMap<>();
        }

        StringBuilder queryStr = new StringBuilder();
        queryStr.append(" FROM LogEntry log WHERE log.docUUID=:uuid ");

        Set<String> filterMapKeySet = filterMap.keySet();
        for (String currentKey : filterMapKeySet) {
            FilterMapEntry currentFilterMapEntry = filterMap.get(currentKey);
            String currentOperator = currentFilterMapEntry.getOperator();
            String currentQueryParameterName = currentFilterMapEntry.getQueryParameterName();
            String currentColumnName = currentFilterMapEntry.getColumnName();

            if (LIKE.equals(currentOperator)) {
                queryStr.append(" AND log.")
                        .append(currentColumnName)
                        .append(" LIKE :")
                        .append(currentQueryParameterName)
                        .append(" ");
            } else {
                queryStr.append(" AND log.")
                        .append(currentColumnName)
                        .append(currentOperator)
                        .append(":")
                        .append(currentQueryParameterName)
                        .append(" ");
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

            if (LIKE.equals(currentOperator)) {
                query.setParameter(currentQueryParameterName, "%" + currentObject + "%");
            } else {
                query.setParameter(currentQueryParameterName, currentObject);
            }
        }

        return doPublish(query.getResultList());
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#getLogEntryByID (long)
     */
    public LogEntry getLogEntryByID(long id) {
        if (log.isDebugEnabled()) {
            log.debug("getLogEntriesFor() logID=" + id);
        }
        return doPublish(em.find(LogEntryImpl.class, id));
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#nativeQueryLogs (java.lang.String, int, int)
     */
    @SuppressWarnings("unchecked")
    public List<LogEntry> nativeQueryLogs(String whereClause, int pageNb, int pageSize) {
        Query query = em.createQuery("from LogEntry log where " + whereClause);
        if (pageNb > 1) {
            query.setFirstResult((pageNb - 1) * pageSize);
        } else if (pageNb == 0) {
            log.warn("Requested pageNb equals 0 but page index start at 1. Will fallback to fetch the first page");
        }
        query.setMaxResults(pageSize);
        return doPublish(query.getResultList());
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#nativeQuery(java .lang.String, int, int)
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
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#nativeQuery(java .lang.String, java.util.Map, int,
     * int)
     */
    public List<?> nativeQuery(String queryString, Map<String, Object> params, int pageNb, int pageSize) {
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

    @SuppressWarnings("unchecked")
    public List<LogEntry> queryLogs(QueryBuilder builder) {
        if (log.isDebugEnabled()) {
            log.debug("queryLogs() builder=" + builder);
        }
        // prepare parameters
        Predicate andPredicate = builder.predicate();
        OrderByList orders = builder.orders();
        long offset = builder.offset();
        long limit = builder.limit();
        // cast parameters
        // current implementation only support a MultiExpression with AND operator
        List<Predicate> predicates = (List<Predicate>) ((List<?>) ((MultiExpression) andPredicate).values);
        // current implementation only use Predicate/OrderByExpr with a simple Reference for left and right
        Function<Operand, String> getFieldName = operand -> ((Reference) operand).name;

        StringBuilder queryStr = new StringBuilder(" FROM LogEntry log");

        // add predicate clauses
        boolean firstFilter = true;
        for (Predicate predicate : predicates) {
            if (firstFilter) {
                queryStr.append(" WHERE");
                firstFilter = false;
            } else {
                queryStr.append(" AND");
            }
            String leftName = getFieldName.apply(predicate.lvalue);
            queryStr.append(" log.")
                    .append(leftName)
                    .append(" ")
                    .append(toString(predicate.operator))
                    .append(" :")
                    .append(leftName);
        }

        // add order clauses
        boolean firstOrder = true;
        for (OrderByExpr order : orders) {
            if (firstOrder) {
                queryStr.append(" ORDER BY");
                firstOrder = false;
            } else {
                queryStr.append(",");
            }
            queryStr.append(" log.").append(getFieldName.apply(order.reference));
        }
        // if firstOrder == false then there's at least one order
        if (!firstOrder) {
            if (orders.get(0).isDescending) {
                queryStr.append(" DESC");
            } else {
                queryStr.append(" ASC");
            }
        }

        Query query = em.createQuery(queryStr.toString());

        for (Predicate predicate : predicates) {
            String leftName = getFieldName.apply(predicate.lvalue);
            Operator operator = predicate.operator;
            Object rightValue = Literals.valueOf(predicate.rvalue);
            if (Operator.LIKE.equals(operator)) {
                rightValue = "%" + rightValue + "%";
            } else if (Operator.STARTSWITH.equals(operator)) {
                rightValue = rightValue + "%";
            }
            query.setParameter(leftName, rightValue);
        }

        // add offset clause
        if (offset > 0) {
            query.setFirstResult((int) offset);
        }

        // add limit clause
        if (limit > 0) {
            query.setMaxResults((int) limit);
        }

        return doPublish(query.getResultList());
    }

    /**
     * A string representation of an Operator
     */
    protected String toString(Operator operator) {
        if (Operator.STARTSWITH.equals(operator)) {
            return LIKE;
        }
        return operator.toString();
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#queryLogs(java. lang.String[], java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public List<LogEntry> queryLogs(String[] eventIds, String dateRange) {
        Date limit;
        try {
            limit = DateRangeParser.parseDateRangeQuery(new Date(), dateRange);
        } catch (AuditQueryException aqe) {
            aqe.addInfo("Wrong date range query. Query was " + dateRange);
            throw aqe;
        }

        String queryStr;
        if (eventIds == null || eventIds.length == 0) {
            queryStr = "from LogEntry log" + " where log.eventDate >= :limit" + " ORDER BY log.eventDate DESC";
        } else {
            String inClause = "(";
            for (String eventId : eventIds) {
                inClause += "'" + eventId + "',";
            }
            inClause = inClause.substring(0, inClause.length() - 1);
            inClause += ")";

            queryStr = "from LogEntry log" + " where log.eventId in " + inClause + " AND log.eventDate >= :limit"
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
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#queryLogsByPage (java.lang.String[], java.lang.String,
     * java.lang.String[], java.lang.String, int, int)
     */
    public List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange, String[] categories, String path,
            int pageNb, int pageSize) {
        Date limit;
        try {
            limit = DateRangeParser.parseDateRangeQuery(new Date(), dateRange);
        } catch (AuditQueryException aqe) {
            aqe.addInfo("Wrong date range query. Query was " + dateRange);
            throw aqe;
        }
        return queryLogsByPage(eventIds, limit, categories, path, pageNb, pageSize);
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#queryLogsByPage (java.lang.String[], java.util.Date,
     * java.lang.String[], java.lang.String, int, int)
     */
    @SuppressWarnings("unchecked")
    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit, String[] categories, String path, int pageNb,
            int pageSize) {
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
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#removeEntries(java .lang.String, java.lang.String)
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
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#countEventsById (java.lang.String)
     */
    public Long countEventsById(String eventId) {
        Query query = em.createNamedQuery("LogEntry.countEventsById");
        query.setParameter("eventId", eventId);
        return (Long) query.getSingleResult();
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.ecm.platform.audit.service.LogEntryProvider#findEventIds()
     */
    @SuppressWarnings("unchecked")
    public List<String> findEventIds() {
        Query query = em.createNamedQuery("LogEntry.findEventIds");
        return (List<String>) query.getResultList();
    }
}
