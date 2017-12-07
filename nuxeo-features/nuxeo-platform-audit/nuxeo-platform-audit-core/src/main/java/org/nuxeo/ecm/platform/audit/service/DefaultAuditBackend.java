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

import static org.nuxeo.ecm.platform.audit.service.LogEntryProvider.createProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Contains the Hibernate based (legacy) implementation
 *
 * @author tiry
 */
public class DefaultAuditBackend extends AbstractAuditBackend {

    protected PersistenceProvider persistenceProvider;

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
        DefaultComponent component = (DefaultComponent) Framework.getRuntime().getComponent(
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
            PersistenceProviderFactory persistenceProviderFactory = Framework.getLocalService(
                    PersistenceProviderFactory.class);
            persistenceProvider = persistenceProviderFactory.newProvider("nxaudit-logs");
            persistenceProvider.openPersistenceUnit();
        } finally {
            thread.setContextClassLoader(last);
        }
    }

    protected <T> T apply(boolean needActivateSession, Function<LogEntryProvider, T> function) {
        return getOrCreatePersistenceProvider().run(Boolean.valueOf(needActivateSession), em -> {
            return function.apply(createProvider(em));
        });
    }

    protected void accept(boolean needActivateSession, Consumer<LogEntryProvider> consumer) {
        getOrCreatePersistenceProvider().run(Boolean.valueOf(needActivateSession), em -> {
            consumer.accept(createProvider(em));
        });
    }

    @Override
    public void addLogEntries(final List<LogEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        TransactionHelper.runInTransaction(() -> accept(true, provider -> provider.addLogEntries(entries)));
    }

    @Override
    public List<LogEntry> getLogEntriesFor(final String uuid, final String repositoryId) {
        return apply(false, provider -> provider.getLogEntriesFor(uuid, repositoryId));
    }

    @Override
    public List<LogEntry> getLogEntriesFor(final String uuid) {
        return apply(false, provider -> provider.getLogEntriesFor(uuid));
    }

    @Override
    public List<LogEntry> getLogEntriesFor(final String uuid, final Map<String, FilterMapEntry> filterMap,
            final boolean doDefaultSort) {
        return apply(false, provider -> provider.getLogEntriesFor(uuid, filterMap, doDefaultSort));
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
    public List<LogEntry> queryLogs(AuditQueryBuilder builder) {
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

}
