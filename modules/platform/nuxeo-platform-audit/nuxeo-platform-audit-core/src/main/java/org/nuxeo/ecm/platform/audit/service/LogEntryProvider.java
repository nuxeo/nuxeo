/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.audit.impl.LogEntrySequenceGenerator.USE_NUXEO_SEQUENCER_PROPERTY;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.audit.api.query.AuditQueryException;
import org.nuxeo.audit.api.query.DateRangeParser;
import org.nuxeo.ecm.core.query.sql.model.Literals;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryList2;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * @deprecated since 2025.0, {@link org.nuxeo.audit.service.AuditBackend} has all necessary APIs
 */
@SuppressWarnings("removal")
@Deprecated(since = "2025.0", forRemoval = true)
public class LogEntryProvider implements BaseLogEntryProvider {

    private static final Logger log = LogManager.getLogger(LogEntryProvider.class);

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
                log.warn("Log entry already exists for id: {}", e::getId);
            }
            em.merge(e);
        });
    }

    protected void doPersist(LogEntry entry) {
        // set log date only if we handle the id
        if (!Framework.isBooleanPropertyTrue(USE_NUXEO_SEQUENCER_PROPERTY)) {
            // Set the log date in java right before saving to the database. We
            // cannot set a static column definition to
            // "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" as MS SQL Server does not
            // support the TIMESTAMP column type and generating a dynamic
            // persistence configuration that would depend on the database is too
            // complicated.
            entry.setLogDate(new Date());
        }
        em.persist(entry);
    }

    protected List<?> doPublishIfEntries(List<?> entries) {
        if (CollectionUtils.isEmpty(entries)) {
            return entries;
        }
        Object entry = entries.getFirst();
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
        if (entry != null && entry.getExtendedInfos() != null) {
            entry.getExtendedInfos().size(); // force lazy loading
        }
        return entry;
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.audit.service.LogEntryProvider#addLogEntry(org .nuxeo.ecm.platform.audit.api.LogEntry)
     */
    @Override
    public void addLogEntry(LogEntry entry) {
        doPersist(entry);
    }

    public void addLogEntries(List<LogEntry> entries) {
        entries.forEach(this::doPersist);
    }

    @SuppressWarnings("unchecked")
    public List<LogEntry> getLogEntriesFor(String uuid, String repositoryId) {
        log.debug("getLogEntriesFor() UUID: {} and repositoryId: {}", uuid, repositoryId);
        Query query = em.createNamedQuery("LogEntry.findByDocumentAndRepository");
        query.setParameter("docUUID", uuid);
        query.setParameter("repositoryId", repositoryId);
        return doPublish(query.getResultList());
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.audit.service.LogEntryProvider#getLogEntryByID (long)
     */
    public LogEntry getLogEntryByID(long id) {
        log.debug("getLogEntriesFor() logID: {}", id);
        return doPublish(em.find(LogEntryImpl.class, id));
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.audit.service.LogEntryProvider#nativeQueryLogs (java.lang.String, int, int)
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
     * @see org.nuxeo.audit.service.LogEntryProvider#nativeQuery(java .lang.String, int, int)
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
     * @see org.nuxeo.audit.service.LogEntryProvider#nativeQuery(java .lang.String, java.util.Map, int, int)
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
        log.debug("queryLogs() builder: {}", builder);
        // prepare parameters
        MultiExpression multiExpression = builder.predicate();
        OrderByList orders = builder.orders();
        long offset = builder.offset();
        long limit = builder.limit();
        List<Predicate> predicates = multiExpression.predicates;
        // current implementation only use Predicate/OrderByExpr with a simple Reference for left and right
        Function<Operand, String> getFieldName = operand -> ((Reference) operand).name;

        StringBuilder queryStr = new StringBuilder(" FROM LogEntry log");

        // add predicate clauses
        Map<String, MutableInt> fieldCounts = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        boolean firstFilter = true;
        String op = multiExpression.operator == Operator.AND ? " AND" : " OR";
        for (Predicate predicate : predicates) {
            if (firstFilter) {
                queryStr.append(" WHERE");
                firstFilter = false;
            } else {
                queryStr.append(op);
            }

            String fieldName = getFieldName.apply(predicate.lvalue);
            Operator operator = predicate.operator;
            Object value = Literals.valueOf(predicate.rvalue);

            int fieldCount = fieldCounts.computeIfAbsent(fieldName, k -> new MutableInt()).incrementAndGet();
            String param = fieldCount == 1 ? fieldName : fieldName + fieldCount;

            if (value instanceof ZonedDateTime) {
                // The ZonedDateTime representation is not compatible with Hibernate query
                value = Date.from(((ZonedDateTime) value).toInstant());
            }
            if (Operator.LIKE.equals(operator)) {
                value = "%" + value + "%";
            } else if (Operator.STARTSWITH.equals(operator)) {
                value = value + "%";
            }

            queryStr.append(" log.").append(fieldName).append(" ").append(toString(operator)).append(" ");
            if (operator == Operator.IN) {
                queryStr.append("("); // parentheses needed in old HQL for IN
            }
            if (operator == Operator.BETWEEN) {
                var values = (List<ZonedDateTime>) value;
                queryStr.append(":").append(param).append("Start");
                // The ZonedDateTime representation is not compatible with Hibernate query
                params.put(param + "Start", Date.from(values.getFirst().toInstant()));
                queryStr.append(" AND ");
                queryStr.append(":").append(param).append("End");
                // The ZonedDateTime representation is not compatible with Hibernate query
                params.put(param + "End", Date.from(values.getLast().toInstant()));
            } else {
                queryStr.append(":").append(param);
                params.put(param, value);
            }
            if (operator == Operator.IN) {
                queryStr.append(")");
            }
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
            if (orders.getFirst().isDescending) {
                queryStr.append(" DESC");
            } else {
                queryStr.append(" ASC");
            }
        }

        String queryString = queryStr.toString();
        Query query = em.createQuery(queryString);
        params.forEach(query::setParameter);

        // add offset clause
        if (offset > 0) {
            query.setFirstResult((int) offset);
        }

        // add limit clause
        if (limit > 0) {
            query.setMaxResults((int) limit);
        }

        List<LogEntry> resultList = doPublish(query.getResultList());

        // now compute total size
        long totalSize = PageProvider.UNKNOWN_SIZE;
        if (builder.countTotal()) {
            Query countQuery = em.createQuery("select count(log.id) " + queryString.replaceAll(" ORDER BY.*$", ""));
            params.forEach(countQuery::setParameter);
            countQuery.setMaxResults(20);
            totalSize = (long) countQuery.getResultList().getFirst();
        }

        return new LogEntryList2(resultList, totalSize);
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
     * @see org.nuxeo.audit.service.LogEntryProvider#queryLogs(java. lang.String[], java.lang.String)
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

        log.debug("queryLogs(): {}", queryStr);
        Query query = em.createQuery(queryStr);
        query.setParameter("limit", limit);

        return doPublish(query.getResultList());
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.audit.service.LogEntryProvider#queryLogsByPage (java.lang.String[], java.lang.String,
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
     * @see org.nuxeo.audit.service.LogEntryProvider#queryLogsByPage (java.lang.String[], java.util.Date,
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
     * @see org.nuxeo.audit.service.LogEntryProvider#removeEntries(java .lang.String, java.lang.String)
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
        log.debug("removed {} entries from {}", count, pathPattern);
        return count;
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.audit.service.LogEntryProvider#countEventsById (java.lang.String)
     */
    public Long countEventsById(String eventId) {
        Query query = em.createNamedQuery("LogEntry.countEventsById");
        query.setParameter("eventId", eventId);
        return (Long) query.getSingleResult();
    }

    /*
     * (non-Javadoc)
     * @see org.nuxeo.audit.service.LogEntryProvider#findEventIds()
     */
    @SuppressWarnings("unchecked")
    public List<String> findEventIds() {
        Query query = em.createNamedQuery("LogEntry.findEventIds");
        return query.getResultList();
    }
}
