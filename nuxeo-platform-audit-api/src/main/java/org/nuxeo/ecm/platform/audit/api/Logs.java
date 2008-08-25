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
 * $Id: LogEntry.java 1362 2006-07-26 14:26:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.api;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Logs interface.
 * <p>
 * {@see http://jira.nuxeo.org/browse/NXP-514}
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface Logs extends Serializable {

    /**
     * Adds given log entries.
     *
     * @param entries the list of log entries.
     * @throws AuditException
     */
    void addLogEntries(List<LogEntry> entries) throws AuditException;

    /**
     * Returns the logs given a doc uuid.
     * <p>
     * :XXX: add parameters to this method for paging.
     *
     * @param uuid the document uuid
     * @return a list of log entries
     * @throws AuditException
     */
    List<LogEntry> getLogEntriesFor(String uuid) throws AuditException;

    List<LogEntry> getLogEntriesFor(String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort)
            throws AuditException;

    /**
     * Returns a given log entry given its id.
     *
     * @param id the log entry identifier
     * @return a LogEntry instance
     * @throws AuditException
     */
    LogEntry getLogEntryByID(long id) throws AuditException;

    /**
     * Returns the list of log entries.
     * <p>
     * Note we will use NXQL in the future when the search engine will index
     * history.
     *
     * @see org.nuxeo.ecm.platform.audit.api.query.DateRangeQueryConstants
     *
     * @param eventIds : the event ids.
     * @param dateRange : a preset date range.
     * @return a list of log entries.
     * @throws AuditException
     */
    List<LogEntry> queryLogs(String[] eventIds, String dateRange)
            throws AuditException;

    /**
     * Returns the batched list of log entries.
     * <p>
     * Note we will use NXQL in the future when the search engine will index
     * history.
     *
     * @see org.nuxeo.ecm.platform.audit.api.query.DateRangeQueryConstants
     *
     * @param eventIds : the event ids.
     * @param dateRange : a preset date range.
     * @param category : add filter on events category
     * @param path : add filter on document path
     * @param pageNb : page number (ignore if <=1)
     * @param pageSize : number of results per page
     * @return a list of log entries.
     * @throws AuditException
     */
    List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange,
            String category, String path, int pageNb, int pageSize)
            throws AuditException;

    /**
     * Returns the batched list of log entries.
     *
     * <p>
     * Note we will use NXQL in the future when the search engine will index
     * history.
     * </p>
     *
     * @see org.nuxeo.ecm.platform.audit.api.query.DateRangeQueryConstants
     *
     * @param eventIds : the event ids.
     * @param limit : filter envents by date from limit to now
     * @param category : add filter on events category
     * @param path : add filter on document path
     * @param pageNb : page number (ignore if <=1)
     * @param pageSize : number of results per page
     * @return a list of log entries.
     * @throws AuditException
     */
    List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String category, String path, int pageNb, int pageSize)
            throws AuditException;

    /**
     * Forces log Synchronisation for a branch of the repository. This can be
     * usefull to add the create entries if DB was initializd from a bulk
     * import.
     *
     * @param repoId
     * @param path
     * @param recurs
     * @return
     * @throws AuditException
     * @throws ClientException
     */
    long syncLogCreationEntries(String repoId, String path, Boolean recurs)
            throws AuditException, ClientException;

    /**
     * Returns a batched list of log entries. WhereClause is a native where
     * clause for the backend : here EJBQL 3.0 can be used
     *
     * @param whereClause
     * @param pageNb
     * @param pageSize
     * @return
     * @throws AuditException
     */
    public List<LogEntry> nativeQueryLogs(String whereClause, int pageNb,
            int pageSize) throws AuditException;

}
