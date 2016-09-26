/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.api;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
    List<LogEntry> getLogEntriesFor(String uuid, String repositoryId);

    /**
     * Returns the logs given a doc uuid.
     *
     * @param uuid the document uuid
     * @return a list of log entries
     * @deprecated since 8.4, use {@link (org.nuxeo.ecm.platform.audit.api.AuditReader.getLogEntriesFor(String,
     *             String))} instead.
     */
    @Deprecated
    List<LogEntry> getLogEntriesFor(String uuid);

    /**
     * Returns the logs given a doc uuid, a map of filters and a default sort.
     *
     * @param uuid the document uuid
     * @param filterMap the map of filters to apply
     * @param doDefaultSort the default sort to set
     * @return a list of log entries
     */
    List<LogEntry> getLogEntriesFor(String uuid, Map<String, FilterMapEntry> filterMap, boolean doDefaultSort);

    /**
     * Returns a given log entry given its id.
     *
     * @param id the log entry identifier
     * @return a LogEntry instance
     */
    LogEntry getLogEntryByID(long id);

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
    List<LogEntry> queryLogs(String[] eventIds, String dateRange);

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
    List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange, String category, String path, int pageNb,
            int pageSize);

    List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange, String[] category, String path, int pageNb,
            int pageSize);

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
    List<LogEntry> queryLogsByPage(String[] eventIds, Date limit, String category, String path, int pageNb,
            int pageSize);

    List<LogEntry> queryLogsByPage(String[] eventIds, Date limit, String[] category, String path, int pageNb,
            int pageSize);

    /**
     * Returns a batched list of log entries. WhereClause is a native where clause for the backend: here EJBQL 3.0 must
     * be used if implementation of audit backend is JPA (< 7.3 or audit.elasticsearch.enabled=false) and JSON if
     * implementation is Elasticsearch.
     */
    List<LogEntry> nativeQueryLogs(String whereClause, int pageNb, int pageSize);

    /**
     * Returns a batched list of entries. query string is a native query clause for the backend : here EJBQL 3.0 must be
     * used if implementation of audit backend is JPA (< 7.3 or audit.elasticsearch.enabled=false) and JSON if
     * implementation is Elasticsearch.
     */
    List<?> nativeQuery(String query, int pageNb, int pageSize);

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

}
