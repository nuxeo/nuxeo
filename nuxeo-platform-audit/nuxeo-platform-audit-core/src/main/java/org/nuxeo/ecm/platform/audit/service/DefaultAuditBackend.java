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

import static org.nuxeo.ecm.core.schema.FacetNames.SYSTEM_DOCUMENT;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.core.persistence.PersistenceProvider.RunVoid;
import org.nuxeo.ecm.core.persistence.PersistenceProviderFactory;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Contains the Hibernate based (legacy) implementation
 * 
 * @author tiry
 *
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

    
    public void activate(NXAuditEventsService component) throws Exception {
        super.activate(component);
    }
    
    @Override
    public void deactivate() throws Exception {
        deactivatePersistenceProvider();   
    }
    
    @Override    
    public void addLogEntries(final List<LogEntry> entries) {
        try {
            getOrCreatePersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    addLogEntries(em, entries);
                }
            });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    
    // ???
    protected void addLogEntries(EntityManager em, List<LogEntry> entries) {
        LogEntryProvider.createProvider(em).addLogEntries(entries);
    }

    @Override
    public List<LogEntry> getLogEntriesFor(final String uuid) {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<List<LogEntry>>() {
                        public List<LogEntry> runWith(EntityManager em) {
                            return getLogEntriesFor(em, uuid);
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    // ???
    protected List<LogEntry> getLogEntriesFor(EntityManager em, String uuid) {
        return LogEntryProvider.createProvider(em).getLogEntriesFor(uuid);
    }

    @Override
    public List<LogEntry> getLogEntriesFor(final String uuid,
            final Map<String, FilterMapEntry> filterMap,
            final boolean doDefaultSort) {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<List<LogEntry>>() {
                        public List<LogEntry> runWith(EntityManager em) {
                            return getLogEntriesFor(em, uuid, filterMap,
                                    doDefaultSort);
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    
    // ??
    protected List<LogEntry> getLogEntriesFor(EntityManager em, String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        return LogEntryProvider.createProvider(em).getLogEntriesFor(uuid,
                filterMap, doDefaultSort);
    }

    @Override
    public LogEntry getLogEntryByID(final long id) {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<LogEntry>() {
                        public LogEntry runWith(EntityManager em) {
                            return getLogEntryByID(em, id);
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    
    // ??
    protected LogEntry getLogEntryByID(EntityManager em, long id) {
        return LogEntryProvider.createProvider(em).getLogEntryByID(id);
    }

    @Override
    public List<LogEntry> nativeQueryLogs(final String whereClause,
            final int pageNb, final int pageSize) {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<List<LogEntry>>() {
                        public List<LogEntry> runWith(EntityManager em) {
                            return nativeQueryLogs(em, whereClause, pageNb,
                                    pageSize);
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    // ??
    protected List<LogEntry> nativeQueryLogs(EntityManager em, String whereClause,
            int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQueryLogs(whereClause,
                pageNb, pageSize);
    }

    @Override
    public List<?> nativeQuery(final String query, final int pageNb,
            final int pageSize) {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<List<?>>() {
                        public List<?> runWith(EntityManager em) {
                            return nativeQuery(em, query, pageNb, pageSize);
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    // ??
    protected List<?> nativeQuery(EntityManager em, String query, int pageNb,
            int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQuery(query, pageNb,
                pageSize);
    }

    @Override
    public List<?> nativeQuery(final String query,
            final Map<String, Object> params, final int pageNb,
            final int pageSize) {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<List<?>>() {
                        public List<?> runWith(EntityManager em) {
                            return nativeQuery(em, query, params, pageNb,
                                    pageSize);
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    // ??
    protected List<?> nativeQuery(EntityManager em, String query,
            Map<String, Object> params, int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQuery(query, params,
                pageNb, pageSize);
    }

    @Override
    public List<LogEntry> queryLogs(final String[] eventIds,
            final String dateRange) {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<List<LogEntry>>() {
                        public List<LogEntry> runWith(EntityManager em) {
                            return queryLogs(em, eventIds, dateRange);
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    // ??
    protected List<LogEntry> queryLogs(EntityManager em, String[] eventIds,
            String dateRange) {
        return LogEntryProvider.createProvider(em).queryLogs(eventIds,
                dateRange);
    }

    @Override
    public List<LogEntry> queryLogsByPage(final String[] eventIds,
            final String dateRange, final String[] category, final String path,
            final int pageNb, final int pageSize) {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<List<LogEntry>>() {
                        public List<LogEntry> runWith(EntityManager em) {
                            return queryLogsByPage(em, eventIds, dateRange,
                                    category, path, pageNb, pageSize);
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    // ??
    protected List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds,
            String dateRange, String[] category, String path, int pageNb,
            int pageSize) {
        try {
            return LogEntryProvider.createProvider(em).queryLogsByPage(
                    eventIds, dateRange, category, path, pageNb, pageSize);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public List<LogEntry> queryLogsByPage(final String[] eventIds,
            final Date limit, final String[] category, final String path,
            final int pageNb, final int pageSize) {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<List<LogEntry>>() {
                        public List<LogEntry> runWith(EntityManager em) {
                            return queryLogsByPage(em, eventIds, limit,
                                    category, path, pageNb, pageSize);
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    // ??
    protected List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds,
            Date limit, String[] category, String path, int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).queryLogsByPage(eventIds,
                limit, category, path, pageNb, pageSize);
    }

    @Override
    public long syncLogCreationEntries(final String repoId, final String path,
            final Boolean recurs) {
        try {
            return getOrCreatePersistenceProvider().run(true,
                    new RunCallback<Long>() {
                        public Long runWith(EntityManager em) {
                            return syncLogCreationEntries(em, repoId, path,
                                    recurs);
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    // ??
    protected long syncLogCreationEntries(EntityManager em, String repoId,
            String path, Boolean recurs) {
        LogEntryProvider provider = LogEntryProvider.createProvider(em);
        provider.removeEntries(DocumentEventTypes.DOCUMENT_CREATED, path);
        try (CoreSession session = CoreInstance.openCoreSession(repoId)) {
            DocumentRef rootRef = new PathRef(path);
            DocumentModel root = guardedDocument(session, rootRef);
            long nbAddedEntries = doSyncNode(provider, session, root, recurs);

            if (log.isDebugEnabled()) {
                log.debug("synced " + nbAddedEntries + " entries on " + path);
            }

            return nbAddedEntries;
        } catch (ClientException e) {
            throw new AuditRuntimeException("Cannot open core session for "
                    + repoId, e);
        }
    }

    
    protected long doSyncNode(LogEntryProvider provider, CoreSession session,
            DocumentModel node, boolean recurs) {

        long nbSyncedEntries = 1;

        Principal principal = guardedPrincipal(session);
        List<DocumentModel> folderishChildren = new ArrayList<DocumentModel>();

        try {
            provider.addLogEntry(doCreateAndFillEntryFromDocument(node,
                    guardedPrincipal(session)));

            for (DocumentModel child : guardedDocumentChildren(session,
                    node.getRef())) {
                if (child.isFolder() && recurs) {
                    folderishChildren.add(child);
                } else {
                    provider.addLogEntry(doCreateAndFillEntryFromDocument(
                            child, principal));
                    nbSyncedEntries += 1;
                }
            }
        } catch (AuditException e) {
            throw new AuditRuntimeException("error occurred while syncing", e);
        }

        if (recurs) {
            for (DocumentModel folderChild : folderishChildren) {
                nbSyncedEntries += doSyncNode(provider, session, folderChild,
                        recurs);
            }
        }

        return nbSyncedEntries;
    }

    public void addLogEntry(final LogEntry entry) {
        try {
            getOrCreatePersistenceProvider().run(true,
                    new RunCallback<Integer>() {
                        public Integer runWith(EntityManager em) {
                            addLogEntry(em, entry);
                            return 0;
                        }
                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void addLogEntry(EntityManager em, LogEntry entry) {
        LogEntryProvider.createProvider(em).addLogEntry(entry);
    }

    @Override
    public Long getEventsCount(final String eventId) {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<Long>() {
                        public Long runWith(EntityManager em) {
                            return getEventsCount(em, eventId);
                        }

                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public Long getEventsCount(EntityManager em, String eventId) {
        return LogEntryProvider.createProvider(em).countEventsById(eventId);
    }

    public List<String> getLoggedEventIds() {
        try {
            return getOrCreatePersistenceProvider().run(false,
                    new RunCallback<List<String>>() {
                        public List<String> runWith(EntityManager em) {
                            return getLoggedEventIds(em);
                        }

                    });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public List<String> getLoggedEventIds(EntityManager em) {
        return LogEntryProvider.createProvider(em).findEventIds();
    }

    public void logEvent(final Event event) {
        try {
            getOrCreatePersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    logEvent(em, event);
                }
            });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void logEvents(final EventBundle eventBundle) {
        try {
            getOrCreatePersistenceProvider().run(true, new RunVoid() {
                public void runWith(EntityManager em) {
                    logEvents(em, eventBundle);
                }
            });
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }

    }

    public void logEvents(EntityManager em, EventBundle eventBundle) {
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

    public void logEvent(EntityManager em, Event event) {
        if (!getAuditableEventNames().contains(event.getName())) {
            return;
        }
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            logDocumentEvent(em, event.getName(), docCtx,
                    new Date(event.getTime()));
        } else {
            logMiscEvent(em, event.getName(), ctx, new Date(event.getTime()));
        }
    }

    protected void logDocumentEvent(EntityManager em, String eventName,
            DocumentEventContext docCtx, Date eventDate) {
        DocumentModel document = docCtx.getSourceDocument();
        if (document.hasFacet(SYSTEM_DOCUMENT)) {
            // do not log event on System documents
            return;
        }

        Boolean disabled = (Boolean) docCtx.getProperty(NXAuditEventsService.DISABLE_AUDIT_LOGGER);
        if (disabled != null && disabled) {
            // don't log events with this flag
            return;
        }
        Principal principal = docCtx.getPrincipal();
        Map<String, Serializable> properties = docCtx.getProperties();

        LogEntry entry = newLogEntry();
        entry.setEventId(eventName);
        entry.setEventDate(eventDate);
        if (document != null) {
            entry.setDocUUID(document.getId());
            entry.setDocPath(document.getPathAsString());
            entry.setDocType(document.getType());
            entry.setRepositoryId(document.getRepositoryName());
        } else {
            log.warn("received event " + eventName + " with null document");
        }
        if (principal != null) {
            String principalName = null;
            if (principal instanceof NuxeoPrincipal) {
                principalName = ((NuxeoPrincipal) principal).getActingUser();
            }
            entry.setPrincipalName(principalName);
        } else {
            log.warn("received event " + eventName + " with null principal");
        }
        entry.setComment((String) properties.get("comment"));
        if (document instanceof DeletedDocumentModel) {
            entry.setComment("Document does not exist anymore!");
        } else {
            try {
                if (document.isLifeCycleLoaded()) {
                    entry.setDocLifeCycle(document.getCurrentLifeCycleState());
                }
            } catch (ClientException e1) {
                throw new AuditRuntimeException(
                        "Cannot fetch life cycle state from " + document, e1);
            }
        }
        if (LifeCycleConstants.TRANSITION_EVENT.equals(eventName)) {
            entry.setDocLifeCycle((String) docCtx.getProperty(LifeCycleConstants.TRANSTION_EVENT_OPTION_TO));
        }
        String category = (String) properties.get("category");
        if (category != null) {
            entry.setCategory(category);
        } else {
            entry.setCategory("eventDocumentCategory");
        }

        doPutExtendedInfos(entry, docCtx, document, principal);

        addLogEntry(em, entry);
    }

    protected void logMiscEvent(EntityManager em, String eventName,
            EventContext ctx, Date eventDate) {
        Principal principal = ctx.getPrincipal();
        Map<String, Serializable> properties = ctx.getProperties();

        LogEntry entry = newLogEntry();
        entry.setEventId(eventName);
        entry.setEventDate(eventDate);
        if (principal != null) {
            String principalName = null;
            if (principal instanceof NuxeoPrincipal) {
                principalName = ((NuxeoPrincipal) principal).getActingUser();
            }
            entry.setPrincipalName(principalName);
        }
        entry.setComment((String) properties.get("comment"));

        String category = (String) properties.get("category");
        entry.setCategory(category);

        doPutExtendedInfos(entry, ctx, null, principal);

        addLogEntry(em, entry);
    }

    // Compat APIs

    public List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds,
            String dateRange, String category, String path, int pageNb,
            int pageSize) {
        String[] categories = { category };
        return queryLogsByPage(em, eventIds, dateRange, categories, path,
                pageNb, pageSize);
    }

    public List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange,
            String category, String path, int pageNb, int pageSize) {
        String[] categories = { category };
        return queryLogsByPage(eventIds, dateRange, categories, path, pageNb,
                pageSize);
    }

    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String category, String path, int pageNb, int pageSize) {
        String[] categories = { category };
        return queryLogsByPage(eventIds, limit, categories, path, pageNb,
                pageSize);
    }

    public List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds,
            Date limit, String category, String path, int pageNb, int pageSize) {
        String[] categories = { category };
        return queryLogsByPage(em, eventIds, limit, categories, path, pageNb,
                pageSize);
    }

    @Override
    public LogEntry newLogEntry() {
        return new LogEntryImpl();
    }

    @Override
    public ExtendedInfo newExtendedInfo(Serializable value) {
        return ExtendedInfoImpl.createExtendedInfo(value);
    }

    
}
