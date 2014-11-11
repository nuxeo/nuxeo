/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
     * Returns the logs given a doc uuid.
     * <p>
     * :XXX: add parameters to this method for paging.
     *
     * @param uuid the document uuid
     * @return a list of log entries
     */
    List<LogEntry> getLogEntriesFor(String uuid);

    List<LogEntry> getLogEntriesFor(String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort);

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
     * Note we will use NXQL in the future when the search engine will index
     * history.
     *
     * @see org.nuxeo.ecm.platform.audit.api.query.DateRangeQueryConstants
     *
     * @param eventIds the event ids.
     * @param dateRange a preset date range.
     * @return a list of log entries.
     */
    List<LogEntry> queryLogs(String[] eventIds, String dateRange);

    /**
     * Returns the batched list of log entries.
     * <p>
     * Note we will use NXQL in the future when the search engine will index
     * history.
     *
     * @see org.nuxeo.ecm.platform.audit.api.query.DateRangeQueryConstants
     *
     * @param eventIds the event ids.
     * @param dateRange a preset date range.
     * @param category add filter on events category
     * @param path add filter on document path
     * @param pageNb page number (ignore if <=1)
     * @param pageSize number of results per page
     *
     * @return a list of log entries.
     */
    List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange,
            String category, String path, int pageNb, int pageSize);

    List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange,
            String[] category, String path, int pageNb, int pageSize);

    /**
     * Returns the batched list of log entries.
     * <p>
     * Note we will use NXQL in the future when the search engine will index
     * history.
     *
     * @see org.nuxeo.ecm.platform.audit.api.query.DateRangeQueryConstants
     *
     * @param eventIds the event ids.
     * @param limit filter events by date from limit to now
     * @param category add filter on events category
     * @param path add filter on document path
     * @param pageNb page number (ignore if <=1)
     * @param pageSize number of results per page
     *
     * @return a list of log entries.
     */
    List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String category, String path, int pageNb, int pageSize);

    List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String[] category, String path, int pageNb, int pageSize);

    /**
     * Returns a batched list of log entries. WhereClause is a native where
     * clause for the backend: here EJBQL 3.0 can be used.
     */
    List<LogEntry> nativeQueryLogs(String whereClause, int pageNb, int pageSize);

    /**
     * Returns a batched list of entries. query string is a native query
     * clause for the backend : here EJBQL 3.0 can be used
     */
    List<?> nativeQuery(String query, int pageNb, int pageSize);

    /**
     * Returns a batched list of entries.
     *
     * @param query a JPA query language query
     * @param params parameters for the query
     * @param pageNb the page number (starts at 1)
     * @param pageSize the number of results per page
     */
    List<?> nativeQuery(String query, Map<String, Object> params, int pageNb, int pageSize);

}
