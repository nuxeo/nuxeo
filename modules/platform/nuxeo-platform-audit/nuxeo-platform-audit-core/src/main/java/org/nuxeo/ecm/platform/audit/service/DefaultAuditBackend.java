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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.audit.service;

import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_ID;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import org.nuxeo.common.function.ThrowableFunction;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CursorResult;
import org.nuxeo.ecm.core.api.CursorService;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Contains the Hibernate based (legacy) implementation
 *
 * @author tiry
 * @deprecated since 2025.0, use {@code org.nuxeo.sql.audit.SQLAuditBackend} instead
 */
@SuppressWarnings("removal")
@Deprecated(since = "2025.0", forRemoval = true)
public class DefaultAuditBackend extends AbstractAuditBackend<LogEntry> {

    protected static final Logger log = LogManager.getLogger(DefaultAuditBackend.class);

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected PersistenceProvider persistenceProvider;

    protected CursorService<Iterator<LogEntry>, LogEntry, String> cursorService = new CursorService<>(
            entry -> String.valueOf(entry.getId()));

    protected CursorService<Iterator<LogEntry>, LogEntry, String> storageCursorService = new CursorService<>(
            ThrowableFunction.asFunction(
                    entry -> MarshallerHelper.objectToJson(entry, RenderingContext.CtxBuilder.get())));

    @SuppressWarnings("removal")
    public DefaultAuditBackend(NXAuditEventsService component, AuditBackendDescriptor config) {
        super(component, config);
        activatePersistenceProvider();
    }

    /**
     * @since 9.3
     */
    public DefaultAuditBackend() {
        super();
    }

    @Override
    public int getApplicationStartedOrder() {
        var component = (DefaultComponent) Framework.getRuntime()
                                                    .getComponent(
                                                            "org.nuxeo.ecm.core.persistence.PersistenceComponent");
        return component.getApplicationStartedOrder() + 1;
    }

    @Override
    public void onApplicationStarted() {
        activatePersistenceProvider();
    }

    @Override
    public void onApplicationStopped() {
        try {
            persistenceProvider.closePersistenceUnit();
        } finally {
            persistenceProvider = null;
        }
    }

    // public for testing purpose !
    public PersistenceProvider getOrCreatePersistenceProvider() {
        if (persistenceProvider == null) {
            activatePersistenceProvider();
        }
        return persistenceProvider;
    }

    protected void activatePersistenceProvider() {
        Thread thread = Thread.currentThread();
        ClassLoader last = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(PersistenceProvider.class.getClassLoader());
            PersistenceProviderFactory persistenceProviderFactory = Framework.getService(
                    PersistenceProviderFactory.class);
            persistenceProvider = persistenceProviderFactory.newProvider("nxaudit-logs");
            persistenceProvider.openPersistenceUnit();
        } finally {
            thread.setContextClassLoader(last);
        }
    }

    protected <T> T apply(boolean needActivateSession, Function<LogEntryProvider, T> function) {
        return getOrCreatePersistenceProvider().run(Boolean.valueOf(needActivateSession), em -> {
            return function.apply(LogEntryProvider.createProvider(em));
        });
    }

    protected void accept(boolean needActivateSession, Consumer<LogEntryProvider> consumer) {
        getOrCreatePersistenceProvider().run(Boolean.valueOf(needActivateSession), em -> {
            consumer.accept(LogEntryProvider.createProvider(em));
        });
    }

    @Override
    public void addLogEntries(final List<LogEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        try {
            TransactionHelper.runInTransaction(() -> accept(true, provider -> provider.addLogEntries(entries)));
        } catch (ConstraintViolationException e) {
            log.debug("A log entry already exists, inserting entries one by one", e);
            insertLogsOneByOneAndThrow(entries, e);
        }
    }

    /** @since 2025.19 */
    public void insertLogs(List<LogEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        for (var entry : entries) {
            // check API contract
            if (entry.getId() == 0L || entry.getLogDate() == null) {
                throw new IllegalArgumentException("Log entry must have an id and log date to be inserted");
            }
            if (entry instanceof LogEntryImpl entryImpl) {
                // backup id coming from router in case the nuxeo sequencer is enabled for SQL
                entryImpl.setOriginalId(entry.getId());
            }
            // clear id and let sequencer do its work
            entry.setId(0L);
        }
        try {
            TransactionHelper.runInTransaction(() -> accept(true, provider -> provider.addLogEntries(entries)));
        } catch (ConstraintViolationException e) {
            log.debug("A log entry already exists, inserting entries one by one", e);
            insertLogsOneByOneAndThrow(entries, e);
        }
    }

    protected void insertLogsOneByOneAndThrow(List<LogEntry> entries, ConstraintViolationException batchException) {
        // commit current transaction
        boolean startTransaction = TransactionHelper.isTransactionActiveOrMarkedRollback();
        if (startTransaction) {
            TransactionHelper.commitOrRollbackTransaction();
        }
        var constraintViolationExceptions = new ArrayList<ConstraintViolationException>();
        for (LogEntry entry : entries) {
            try {
                // re-init the id as Hibernate fill it during previous tentative to persist the whole batch
                entry.setId(0L);
                TransactionHelper.runInTransaction(() -> accept(true, provider -> provider.addLogEntry(entry)));
            } catch (ConstraintViolationException e) {
                constraintViolationExceptions.add(e);
            }
        }
        // restore transactional context
        if (startTransaction) {
            TransactionHelper.startTransaction();
        }
        List<String> duplicates = constraintViolationExceptions.stream()
                                                               .filter(e -> e.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE)
                                                               .map(Throwable::getMessage)
                                                               .toList();
        if (duplicates.size() == constraintViolationExceptions.size()) {
            var concurrentUpdateException = new ConcurrentUpdateException("Concurrent update");
            concurrentUpdateException.addSuppressed(batchException);
            duplicates.forEach(concurrentUpdateException::addInfo);
            throw concurrentUpdateException;
        }
        var nuxeoException = new NuxeoException("Error while inserting audit log entries", batchException);
        constraintViolationExceptions.forEach(nuxeoException::addSuppressed);
        throw nuxeoException;
    }

    @Override
    public List<LogEntry> getLogEntriesFor(final String uuid, final String repositoryId) {
        return apply(false, provider -> provider.getLogEntriesFor(uuid, repositoryId));
    }

    @Override
    public LogEntry getLogEntryByID(final long id) {
        return apply(false, provider -> provider.getLogEntryByID(id));
    }

    @Override
    public List<LogEntry> nativeQueryLogs(final String whereClause, final int pageNb, final int pageSize) {
        return apply(false, provider -> provider.nativeQueryLogs(whereClause, pageNb, pageSize));
    }

    @Override
    public List<?> nativeQuery(final String query, final int pageNb, final int pageSize) {
        return apply(false, provider -> provider.nativeQuery(query, pageNb, pageSize));
    }

    @Override
    public List<?> nativeQuery(final String query, final Map<String, Object> params, final int pageNb,
            final int pageSize) {
        return apply(false, provider -> provider.nativeQuery(query, params, pageNb, pageSize));
    }

    @Override
    public List<LogEntry> queryLogs(QueryBuilder builder) {
        return apply(false, provider -> provider.queryLogs(builder));
    }

    @Override
    public List<LogEntry> queryLogs(final String[] eventIds, final String dateRange) {
        return apply(false, provider -> provider.queryLogs(eventIds, dateRange));
    }

    @Override
    public List<LogEntry> queryLogsByPage(final String[] eventIds, final Date limit, final String[] category,
            final String path, final int pageNb, final int pageSize) {
        return apply(false, provider -> provider.queryLogsByPage(eventIds, limit, category, path, pageNb, pageSize));
    }

    /**
     * @deprecated since 2025.0, seems unused
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    @Override
    public long syncLogCreationEntries(final String repoId, final String path, final Boolean recurs) {
        return apply(false, provider -> syncLogCreationEntries(provider, repoId, path, recurs));
    }

    @Override
    public Long getEventsCount(final String eventId) {
        return apply(false, provider -> provider.countEventsById(eventId));
    }

    public List<String> getLoggedEventIds() {
        return apply(false, LogEntryProvider::findEventIds);
    }

    @Override
    public ExtendedInfo newExtendedInfo(Serializable value) {
        return ExtendedInfoImpl.createExtendedInfo(value);
    }

    @Override
    public long getLatestLogId(String repositoryId, String... eventIds) {
        Map<String, Object> params = getParams(eventIds);
        String paramNames = getParamNames(eventIds);
        params.put("repoId", repositoryId);
        String query = String.format("FROM LogEntry log" //
                + " WHERE log.eventId IN (%s)" //
                + "   AND log.repositoryId = :repoId" //
                + " ORDER BY log.id DESC", paramNames);
        @SuppressWarnings("unchecked")
        List<LogEntry> entries = (List<LogEntry>) nativeQuery(query, params, 1, 1);
        return entries.isEmpty() ? 0 : entries.get(0).getId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LogEntry> getLogEntriesAfter(long logIdOffset, int limit, String repositoryId, String... eventIds) {
        Map<String, Object> params = getParams(eventIds);
        String paramNames = getParamNames(eventIds);
        params.put("repoId", repositoryId);
        params.put("minId", Long.valueOf(logIdOffset));
        String query = String.format("FROM LogEntry log" //
                + " WHERE log.id >= :minId" //
                + "   AND log.eventId IN (%s)" //
                + "   AND log.repositoryId = :repoId" //
                + " ORDER BY log.id", paramNames);
        return (List<LogEntry>) nativeQuery(query, params, 1, limit);
    }

    protected String getParamNames(String[] eventId) {
        List<String> ret = new ArrayList<>(eventId.length);
        for (String event : eventId) {
            ret.add(":ev" + event);
        }
        return String.join(",", ret);
    }

    protected Map<String, Object> getParams(String[] eventId) {
        HashMap<String, Object> ret = new HashMap<>(eventId.length);
        for (String event : eventId) {
            ret.put("ev" + event, event);
        }
        return ret;
    }

    @Override
    public void append(List<String> jsonEntries) {
        List<LogEntry> entries = new ArrayList<>();
        for (String json : jsonEntries) {
            try {
                LogEntryImpl entry = OBJECT_MAPPER.readValue(json, LogEntryImpl.class);
                if (entry.getId() == 0) {
                    throw new NuxeoException("A json entry has an empty id. entry=" + json);
                }
                entries.add(entry);
            } catch (IOException e) {
                throw new NuxeoException("Unable to deserialize json entries", e);
            }
        }
        accept(false, provider -> provider.append(entries));
    }

    /** @since 2025.18 */
    public ScrollResult<String> scrollLogIds(QueryBuilder builder, int batchSize, Duration keepAlive) {
        cursorService.checkForTimedOutScroll();
        // as we're using pages to scroll audit, we need to add an order to make results across pages deterministic
        builder.orders(OrderByExprs.asc(LOG_ID), builder.orders().toArray(new OrderByExpr[0]));
        String scrollId = cursorService.registerCursorResult(
                new SQLAuditCursorResult(builder, batchSize, (int) keepAlive.toSeconds()));
        return scrollLogIds(scrollId);
    }

    /** @since 2025.18 */
    public ScrollResult<String> scrollLogIds(String scrollId) {
        return cursorService.scroll(scrollId);
    }

    /** @since 2025.18 */
    public void clearScroll(String scrollId) {
        cursorService.unregisterCursor(scrollId);
    }

    @SuppressWarnings("resource") // CursorResult is being registered, must not be closed
    @Override
    public ScrollResult<String> scroll(QueryBuilder builder, int batchSize, int keepAliveSeconds) {
        // as we're using pages to scroll audit, we need to add an order to make results across pages deterministic
        builder.orders(OrderByExprs.asc(LOG_ID), builder.orders().toArray(new OrderByExpr[0]));
        String scrollId = storageCursorService.registerCursorResult(
                new SQLAuditCursorResult(builder, batchSize, keepAliveSeconds));
        return scroll(scrollId);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return storageCursorService.scroll(scrollId);
    }

    public class SQLAuditCursorResult extends CursorResult<Iterator<LogEntry>, LogEntry> {

        protected final QueryBuilder builder;

        protected long pageNb;

        protected boolean end;

        public SQLAuditCursorResult(QueryBuilder builder, int batchSize, int keepAliveSeconds) {
            super(Collections.emptyIterator(), batchSize, keepAliveSeconds);
            this.builder = builder;
            this.pageNb = 0;
        }

        @Override
        public boolean hasNext() {
            if (cursor == null || end) {
                return false;
            } else if (cursor.hasNext()) {
                return true;
            } else {
                runNextPage();
                return !end;
            }
        }

        @Override
        public LogEntry next() {
            if (cursor != null && !cursor.hasNext() && !end) {
                // try to run a next scroll
                runNextPage();
            }
            return super.next();
        }

        protected void runNextPage() {
            builder.offset(pageNb++ * batchSize).limit(batchSize);
            cursor = queryLogs(builder).iterator();
            end = !cursor.hasNext();
        }

        @Override
        public void close() {
            end = true;
            // Call super close to clear cursor
            super.close();
        }

    }

}
