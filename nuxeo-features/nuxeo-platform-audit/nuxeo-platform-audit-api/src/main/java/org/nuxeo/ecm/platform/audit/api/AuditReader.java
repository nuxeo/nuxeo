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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.audit.api;

import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_REPOSITORY_ID;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.audit.api.query.AuditQueryException;
import org.nuxeo.ecm.platform.audit.api.query.DateRangeParser;

/**
 * Interface for reading data from the Audit service.
 *
 * @author tiry
 */
public interface AuditReader {

    /**
     * Returns the logs given a doc uuid and a repository id.
     *
     * @param uuid the document uuid
     * @param repositoryId the repository id
     * @return a list of log entries
     * @since 8.4
     */
    default List<LogEntry> getLogEntriesFor(String uuid, String repositoryId) {
        return queryLogs(
                new AuditQueryBuilder().predicates(Predicates.eq(LOG_DOC_UUID, uuid),
                        Predicates.eq(LOG_REPOSITORY_ID, repositoryId)).defaultOrder());
    }

    /**
     * Returns the logs given a doc uuid.
     *
     * @param uuid the document uuid
     * @return a list of log entries
     * @deprecated since 8.4, use {@link #getLogEntriesFor(String, String)} instead.
     */
    @Deprecated
    default List<LogEntry> getLogEntriesFor(String uuid) {
        return queryLogs(
                new AuditQueryBuilder().predicates(Predicates.eq(LOG_DOC_UUID, uuid))
                                       .defaultOrder());
    }

    /**
     * Returns the logs given a doc uuid, a map of filters and a default sort.
     *
     * @param uuid the document uuid
     * @param filterMap the map of filters to apply
     * @param doDefaultSort the default sort to set (eventDate desc)
     * @return a list of log entries
     * @deprecated since 9.3, this method doesn't take into account the document repository, use
     *             {@link #queryLogs(QueryBuilder)} instead.
     */
    @Deprecated
    List<LogEntry> getLogEntriesFor(String uuid, Map<String, FilterMapEntry> filterMap, boolean doDefaultSort);

    /**
     * Returns a given log entry given its id.
     *
     * @param id the log entry identifier
     * @return a LogEntry instance
     */
    LogEntry getLogEntryByID(long id);

    /**
     * Returns the logs given a collection of predicates and a default sort.
     *
     * @param builder the query builder to fetch log entries
     * @return a list of log entries
     * @since 9.3
     */
    List<LogEntry> queryLogs(QueryBuilder builder);

    /**
     * Returns the list of log entries.
     * <p>
     * Note we will use NXQL in the future when the search engine will index history.
     *
     * @see org.nuxeo.ecm.platform.audit.api.query.DateRangeQueryConstants
     * @param eventIds the event ids.
     * @param dateRange a preset date range.
     * @return a list of log entries.
     */
    default List<LogEntry> queryLogs(String[] eventIds, String dateRange) {
        return queryLogsByPage(eventIds, (String) null, (String[]) null, null, 0, 10000);
    }

    /**
     * Returns the batched list of log entries.
     * <p>
     * Note we will use NXQL in the future when the search engine will index history.
     *
     * @see org.nuxeo.ecm.platform.audit.api.query.DateRangeQueryConstants
     * @param eventIds the event ids.
     * @param dateRange a preset date range.
     * @param category add filter on events category
     * @param path add filter on document path
     * @param pageNb page number (ignore if <=1)
     * @param pageSize number of results per page
     * @return a list of log entries.
     */
    default List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange, String category, String path, int pageNb,
            int pageSize) {
        return queryLogsByPage(eventIds, dateRange, new String[] { category }, path, pageNb, pageSize);
    }

    default List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange, String[] categories, String path, int pageNb,
            int pageSize) {

        Date limit = null;
        if (dateRange != null) {
            try {
                limit = DateRangeParser.parseDateRangeQuery(new Date(), dateRange);
            } catch (AuditQueryException aqe) {
                aqe.addInfo("Wrong date range query. Query was " + dateRange);
                throw aqe;
            }
        }
        return queryLogsByPage(eventIds, limit, categories, path, pageNb, pageSize);
    }

    /**
     * Returns the batched list of log entries.
     * <p>
     * Note we will use NXQL in the future when the search engine will index history.
     *
     * @see org.nuxeo.ecm.platform.audit.api.query.DateRangeQueryConstants
     * @param eventIds the event ids.
     * @param limit filter events by date from limit to now
     * @param category add filter on events category
     * @param path add filter on document path
     * @param pageNb page number (ignore if <=1)
     * @param pageSize number of results per page
     * @return a list of log entries.
     */
    default List<LogEntry> queryLogsByPage(String[] eventIds, Date limit, String category, String path, int pageNb,
            int pageSize) {
        return queryLogsByPage(eventIds, limit, new String[] { category }, path, pageNb, pageSize);
    }

    List<LogEntry> queryLogsByPage(String[] eventIds, Date limit, String[] categories, String path, int pageNb,
            int pageSize);

    /**
     * Returns a batched list of log entries. WhereClause is a native where clause for the backend: here EJBQL 3.0 must
     * be used if implementation of audit backend is JPA (< 7.3 or audit.elasticsearch.enabled=false) and JSON if
     * implementation is Elasticsearch.
     */
    default List<LogEntry> nativeQueryLogs(String whereClause, int pageNb, int pageSize) {
        return nativeQuery(whereClause, pageNb, pageSize).stream()
                                                         .filter(LogEntry.class::isInstance)
                                                         .map(LogEntry.class::cast)
                                                         .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Returns a batched list of entries. query string is a native query clause for the backend : here EJBQL 3.0 must be
     * used if implementation of audit backend is JPA (< 7.3 or audit.elasticsearch.enabled=false) and JSON if
     * implementation is Elasticsearch.
     */
    default List<?> nativeQuery(String query, int pageNb, int pageSize) {
        return nativeQuery(query, Collections.<String, Object> emptyMap(), pageNb, pageSize);
    }

    /**
     * Returns a batched list of entries.
     *
     * @param query a JPA query language query if implementation of audit backend is JPA (< 7.3 or
     *            audit.elasticsearch.enabled=false) and JSON if implementation is Elasticsearch
     * @param params parameters for the query
     * @param pageNb the page number (starts at 1)
     * @param pageSize the number of results per page
     */
    List<?> nativeQuery(String query, Map<String, Object> params, int pageNb, int pageSize);

    /**
     * Returns the latest log id matching events and repository or 0 when no match found.
     *
     * @since 9.3
     */
    long getLatestLogId(String repositoryId, String... eventIds);

    /**
     * Returns up to limit log entries matching events and repository with log id greater or equal to logIdOffset.
     *
     * @since 9.3
     */
    List<LogEntry> getLogEntriesAfter(long logIdOffset, int limit, String repositoryId, String... eventIds);

}
