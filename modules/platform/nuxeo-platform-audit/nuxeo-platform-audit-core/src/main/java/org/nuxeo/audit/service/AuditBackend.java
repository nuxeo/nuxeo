/*
 * (C) Copyright 2024-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.audit.service;

import static org.nuxeo.audit.api.LogEntryConstants.LOG_DOC_UUID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_EVENT_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_ID;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_REPOSITORY_ID;

import java.util.Collection;
import java.util.List;

import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.LogEntryList;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;

/**
 * @since 2025.0
 */
@SuppressWarnings("removal")
public interface AuditBackend extends org.nuxeo.ecm.platform.audit.service.AuditBackend<LogEntry> {

    /**
     * Adds given log entries.
     *
     * @param entries the list of log entries.
     * @apiNote This API will generate the log entry ids.
     * @deprecated since 2025.16, insertion to audit backend became an internal API, use
     *             {@link AuditRouter#routeToBackends(List)} instead
     */
    @Deprecated(since = "2025.16", forRemoval = true)
    void addLogEntries(List<LogEntry> entries);

    /**
     * Inserts the given log entries into the backend.
     * 
     * @param entries the list of log entries
     * @since 2025.16
     * @apiNote This API won't generate the log entry ids, the caller is responsible for setting them.
     */
    // deprecated since 2021.x, turn this method abstract on deprecation removal
    default void insertLogs(Collection<LogEntry> entries) {
        // let the backend generate the id and log date, as it was the case for addLogEntries
        addLogEntries(entries.stream().map(entry -> entry.builder().id(null).logDate(null).build()).toList());
    }

    Long getEventsCount(final String eventId);

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
    LogEntryList queryLogs(QueryBuilder builder);

    /**
     * Returns the logs given a doc uuid and a repository id.
     *
     * @param uuid the document uuid
     * @param repositoryId the repository id
     * @return a list of log entries
     * @since 8.4
     */
    default List<LogEntry> getLogEntriesFor(String uuid, String repositoryId) {
        return queryLogs(new AuditQueryBuilder().predicate(Predicates.eq(LOG_DOC_UUID, uuid))
                                                .and(Predicates.eq(LOG_REPOSITORY_ID, repositoryId))
                                                .defaultOrder());
    }

    /**
     * Returns the latest log id matching events and repository or 0 when no match found.
     *
     * @since 9.3
     */
    default long getLatestLogId(String repositoryId, String... eventIds) {
        QueryBuilder builder = new AuditQueryBuilder().predicate(Predicates.eq(LOG_REPOSITORY_ID, repositoryId))
                                                      .and(Predicates.in(LOG_EVENT_ID, eventIds))
                                                      .order(OrderByExprs.desc(LOG_ID))
                                                      .limit(1);
        return queryLogs(builder).stream().mapToLong(LogEntry::getId).findFirst().orElse(0L);
    }

    /**
     * Checks whether the backend has the capability.
     */
    boolean hasCapability(Capability capability);

    enum Capability {
        EXTENDED_INFO_SEARCH,

        /** @since 2025.4 */
        STARTS_WITH_PARTIAL_MATCH;
    }
}
