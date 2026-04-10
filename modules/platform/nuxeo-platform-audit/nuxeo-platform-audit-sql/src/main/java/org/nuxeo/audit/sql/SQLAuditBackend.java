/*
 * (C) Copyright 2024-2025 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.audit.sql;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.LogEntryList;
import org.nuxeo.audit.service.AbstractAuditBackend;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntryList2;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 2025.0
 */
@SuppressWarnings("removal")
public class SQLAuditBackend extends AbstractAuditBackend {

    protected static final LogEntryImplToLogEntry FROM_LOG_ENTRY_SQL_MAPPER = new LogEntryImplToLogEntry();

    protected static final LogEntryToLogEntryImpl TO_LOG_ENTRY_SQL_MAPPER = new LogEntryToLogEntryImpl();

    protected final DefaultAuditBackend backend = new DefaultAuditBackend(
            Framework.getService(NXAuditEventsService.class), null);

    @Override
    public ExtendedInfo newExtendedInfo(Serializable value) {
        return backend.newExtendedInfo(value);
    }

    @Override
    public void insertLogs(Collection<LogEntry> entries) {
        backend.insertLogs(entries.stream().map(TO_LOG_ENTRY_SQL_MAPPER).toList());
    }

    @Override
    public void append(List<String> jsonEntries) {
        backend.append(jsonEntries);
    }

    @Override
    public ScrollResult<String> scroll(QueryBuilder queryBuilder, int batchSize, int keepAliveSeconds) {
        return backend.scroll(queryBuilder, batchSize, keepAliveSeconds);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return backend.scroll(scrollId);
    }

    @Override
    public int getApplicationStartedOrder() {
        return backend.getApplicationStartedOrder();
    }

    @Override
    public void onApplicationStarted() {
        backend.onApplicationStarted();
    }

    @Override
    public void onApplicationStopped() {
        backend.onApplicationStopped();
    }

    @Override
    public long syncLogCreationEntries(String repoId, String path, Boolean recurs) {
        return backend.syncLogCreationEntries(repoId, path, recurs);
    }

    @Override
    public Long getEventsCount(String eventId) {
        return backend.getEventsCount(eventId);
    }

    @Override
    public LogEntry getLogEntryByID(long id) {
        return FROM_LOG_ENTRY_SQL_MAPPER.apply(backend.getLogEntryByID(id));
    }

    @Override
    public LogEntryList queryLogs(QueryBuilder builder) {
        List<org.nuxeo.ecm.platform.audit.api.LogEntry> logEntries = backend.queryLogs(builder);
        long totalSize = logEntries instanceof LogEntryList2 l ? l.getTotalSize() : PageProvider.UNKNOWN_SIZE;
        return logEntries.stream()
                         .map(FROM_LOG_ENTRY_SQL_MAPPER)
                         .collect(Collectors.collectingAndThen(Collectors.toList(),
                                 list -> new LogEntryList(list, totalSize)));
    }

    /** @since 2025.18 */
    @Override
    public ScrollResult<String> scrollLogIds(QueryBuilder builder, int batchSize, Duration keepAlive) {
        return backend.scrollLogIds(builder, batchSize, keepAlive);
    }

    /** @since 2025.18 */
    @Override
    public ScrollResult<String> scrollLogIds(String scrollId) {
        return backend.scrollLogIds(scrollId);
    }

    /** @since 2025.18 */
    @Override
    public void clearScrollLogIds(String scrollId) {
        backend.clearScroll(scrollId);
    }

    @Override
    public boolean hasCapability(Capability capability) {
        return switch (capability) {
            case EXTENDED_INFO_SEARCH -> false;
            case STARTS_WITH_PARTIAL_MATCH -> true;
        };
    }

    @Override
    public List<LogEntry> nativeQueryLogs(String whereClause, int pageNb, int pageSize) {
        return backend.nativeQueryLogs(whereClause, pageNb, pageSize).stream().map(FROM_LOG_ENTRY_SQL_MAPPER).toList();
    }

    @Override
    public List<?> nativeQuery(String query, Map<String, Object> params, int pageNb, int pageSize) {
        return backend.nativeQuery(query, params, pageNb, pageSize).stream().map(entry -> {
            if (entry instanceof org.nuxeo.ecm.platform.audit.api.LogEntry logEntry) {
                return FROM_LOG_ENTRY_SQL_MAPPER.apply(logEntry);
            }
            return entry;
        }).toList();
    }

    @Override
    protected void clearEntries() {
        TransactionHelper.runInTransaction(this::doClearEntries);
    }

    protected void doClearEntries() {
        try (var entityManager = Framework.getService(PersistenceProviderFactory.class)
                                          .newProvider("nxaudit-logs")
                                          .acquireEntityManager()) {
            entityManager.createNativeQuery("delete from nxp_logs_mapextinfos").executeUpdate();
            entityManager.createNativeQuery("delete from nxp_logs_extinfo").executeUpdate();
            entityManager.createNativeQuery("delete from nxp_logs").executeUpdate();
        }
    }

    public List<String> getLoggedEventIds() {
        return backend.getLoggedEventIds();
    }
}
