/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.audit.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunVoid;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;

/**
 * Contains the Hibernate based (legacy) implementation
 *
 * @author tiry
 */
public class DefaultAuditBackend extends AbstractAuditBackend implements AuditBackend {

    protected PersistenceProvider persistenceProvider;

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
            PersistenceProviderFactory persistenceProviderFactory = Framework.getLocalService(PersistenceProviderFactory.class);
            persistenceProvider = persistenceProviderFactory.newProvider("nxaudit-logs");
            persistenceProvider.openPersistenceUnit();
        } finally {
            thread.setContextClassLoader(last);
        }
    }

    protected void deactivatePersistenceProvider() {
        if (persistenceProvider != null) {
            persistenceProvider.closePersistenceUnit();
            persistenceProvider = null;
        }
    }

    @Override
    public void deactivate() {
        deactivatePersistenceProvider();
    }

    @Override
    public void addLogEntries(final List<LogEntry> entries) {
        getOrCreatePersistenceProvider().run(true, new RunVoid() {
            @Override
            public void runWith(EntityManager em) {
                addLogEntries(em, entries);
            }
        });
    }

    protected void addLogEntries(EntityManager em, List<LogEntry> entries) {
        LogEntryProvider.createProvider(em).addLogEntries(entries);
    }

    @Override
    public List<LogEntry> getLogEntriesFor(final String uuid) {
        return getOrCreatePersistenceProvider().run(false, new RunCallback<List<LogEntry>>() {
            @Override
            public List<LogEntry> runWith(EntityManager em) {
                return getLogEntriesFor(em, uuid);
            }
        });
    }

    protected List<LogEntry> getLogEntriesFor(EntityManager em, String uuid) {
        return LogEntryProvider.createProvider(em).getLogEntriesFor(uuid);
    }

    @Override
    public List<LogEntry> getLogEntriesFor(final String uuid, final Map<String, FilterMapEntry> filterMap,
            final boolean doDefaultSort) {
        return getOrCreatePersistenceProvider().run(false, new RunCallback<List<LogEntry>>() {
            @Override
            public List<LogEntry> runWith(EntityManager em) {
                return getLogEntriesFor(em, uuid, filterMap, doDefaultSort);
            }
        });
    }

    protected List<LogEntry> getLogEntriesFor(EntityManager em, String uuid, Map<String, FilterMapEntry> filterMap,
            boolean doDefaultSort) {
        return LogEntryProvider.createProvider(em).getLogEntriesFor(uuid, filterMap, doDefaultSort);
    }

    @Override
    public LogEntry getLogEntryByID(final long id) {
        return getOrCreatePersistenceProvider().run(false, new RunCallback<LogEntry>() {
            @Override
            public LogEntry runWith(EntityManager em) {
                return getLogEntryByID(em, id);
            }
        });
    }

    protected LogEntry getLogEntryByID(EntityManager em, long id) {
        return LogEntryProvider.createProvider(em).getLogEntryByID(id);
    }

    @Override
    public List<LogEntry> nativeQueryLogs(final String whereClause, final int pageNb, final int pageSize) {
        return getOrCreatePersistenceProvider().run(false, new RunCallback<List<LogEntry>>() {
            @Override
            public List<LogEntry> runWith(EntityManager em) {
                return nativeQueryLogs(em, whereClause, pageNb, pageSize);
            }
        });
    }

    protected List<LogEntry> nativeQueryLogs(EntityManager em, String whereClause, int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQueryLogs(whereClause, pageNb, pageSize);
    }

    @Override
    public List<?> nativeQuery(final String query, final int pageNb, final int pageSize) {
        return getOrCreatePersistenceProvider().run(false, new RunCallback<List<?>>() {
            @Override
            public List<?> runWith(EntityManager em) {
                return nativeQuery(em, query, pageNb, pageSize);
            }
        });
    }

    protected List<?> nativeQuery(EntityManager em, String query, int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQuery(query, pageNb, pageSize);
    }

    @Override
    public List<?> nativeQuery(final String query, final Map<String, Object> params, final int pageNb,
            final int pageSize) {
        return getOrCreatePersistenceProvider().run(false, new RunCallback<List<?>>() {
            @Override
            public List<?> runWith(EntityManager em) {
                return nativeQuery(em, query, params, pageNb, pageSize);
            }
        });
    }

    protected List<?> nativeQuery(EntityManager em, String query, Map<String, Object> params, int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQuery(query, params, pageNb, pageSize);
    }

    @Override
    public List<LogEntry> queryLogs(final String[] eventIds, final String dateRange) {
        return getOrCreatePersistenceProvider().run(false, new RunCallback<List<LogEntry>>() {
            @Override
            public List<LogEntry> runWith(EntityManager em) {
                return queryLogs(em, eventIds, dateRange);
            }
        });
    }

    protected List<LogEntry> queryLogs(EntityManager em, String[] eventIds, String dateRange) {
        return LogEntryProvider.createProvider(em).queryLogs(eventIds, dateRange);
    }

    @Override
    public List<LogEntry> queryLogsByPage(final String[] eventIds, final String dateRange, final String[] category,
            final String path, final int pageNb, final int pageSize) {
        return getOrCreatePersistenceProvider().run(false, new RunCallback<List<LogEntry>>() {
            @Override
            public List<LogEntry> runWith(EntityManager em) {
                return queryLogsByPage(em, eventIds, dateRange, category, path, pageNb, pageSize);
            }
        });
    }

    protected List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds, String dateRange, String[] category,
            String path, int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).queryLogsByPage(eventIds, dateRange, category, path, pageNb,
                pageSize);
    }

    @Override
    public List<LogEntry> queryLogsByPage(final String[] eventIds, final Date limit, final String[] category,
            final String path, final int pageNb, final int pageSize) {
        return getOrCreatePersistenceProvider().run(false, new RunCallback<List<LogEntry>>() {
            @Override
            public List<LogEntry> runWith(EntityManager em) {
                return queryLogsByPage(em, eventIds, limit, category, path, pageNb, pageSize);
            }
        });
    }

    protected List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds, Date limit, String[] category,
            String path, int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).queryLogsByPage(eventIds, limit, category, path, pageNb, pageSize);
    }

    @Override
    public long syncLogCreationEntries(final String repoId, final String path, final Boolean recurs) {
        return getOrCreatePersistenceProvider().run(true, new RunCallback<Long>() {
            @Override
            public Long runWith(EntityManager em) {
                return syncLogCreationEntries(em, repoId, path, recurs);
            }
        });
    }

    protected long syncLogCreationEntries(EntityManager em, String repoId, String path, Boolean recurs) {
        LogEntryProvider provider = LogEntryProvider.createProvider(em);
        return syncLogCreationEntries(provider, repoId, path, recurs);
    }

    public void addLogEntry(final LogEntry entry) {
        getOrCreatePersistenceProvider().run(true, new RunCallback<Integer>() {
            @Override
            public Integer runWith(EntityManager em) {
                addLogEntry(em, entry);
                return 0;
            }
        });
    }

    public void addLogEntry(EntityManager em, LogEntry entry) {
        LogEntryProvider.createProvider(em).addLogEntry(entry);
    }

    @Override
    public Long getEventsCount(final String eventId) {
        return getOrCreatePersistenceProvider().run(false, new RunCallback<Long>() {
            @Override
            public Long runWith(EntityManager em) {
                return getEventsCount(em, eventId);
            }

        });
    }

    public Long getEventsCount(EntityManager em, String eventId) {
        return LogEntryProvider.createProvider(em).countEventsById(eventId);
    }

    public List<String> getLoggedEventIds() {
        return getOrCreatePersistenceProvider().run(false, new RunCallback<List<String>>() {
            @Override
            public List<String> runWith(EntityManager em) {
                return getLoggedEventIds(em);
            }

        });
    }

    protected List<String> getLoggedEventIds(EntityManager em) {
        return LogEntryProvider.createProvider(em).findEventIds();
    }

    @Override
    public void logEvent(final Event event) {
        getOrCreatePersistenceProvider().run(true, new RunVoid() {
            @Override
            public void runWith(EntityManager em) {
                logEvent(em, event);
            }
        });
    }

    @Override
    public void logEvents(final EventBundle eventBundle) {
        getOrCreatePersistenceProvider().run(true, new RunVoid() {
            @Override
            public void runWith(EntityManager em) {
                logEvents(em, eventBundle);
            }
        });
    }

    protected void logEvents(EntityManager em, EventBundle eventBundle) {
        boolean processEvents = false;
        for (String name : getAuditableEventNames()) {
            if (eventBundle.containsEventName(name)) {
                processEvents = true;
                break;
            }
        }
        if (!processEvents) {
            return;
        }
        for (Event event : eventBundle) {
            logEvent(em, event);
        }
    }

    protected void logEvent(EntityManager em, Event event) {
        LogEntry entry = buildEntryFromEvent(event);
        if (entry != null) {
            addLogEntry(em, entry);
        }
    }

    // Compat APIs

    protected List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds, String dateRange, String category,
            String path, int pageNb, int pageSize) {
        String[] categories = { category };
        return queryLogsByPage(em, eventIds, dateRange, categories, path, pageNb, pageSize);
    }

    protected List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds, Date limit, String category,
            String path, int pageNb, int pageSize) {
        String[] categories = { category };
        return queryLogsByPage(em, eventIds, limit, categories, path, pageNb, pageSize);
    }

    @Override
    public void onApplicationStarted() {
        // Nothing to do
    }

}
