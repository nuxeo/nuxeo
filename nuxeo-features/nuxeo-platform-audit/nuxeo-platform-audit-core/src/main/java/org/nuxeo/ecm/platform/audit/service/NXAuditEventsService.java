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
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.service;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ELException;
import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
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
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.EventDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

import de.odysseus.el.ExpressionFactoryImpl;

/**
 * Event service configuration.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class NXAuditEventsService extends DefaultComponent implements
        NXAuditEvents {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.audit.service.NXAuditEventsService");

    private static final String EVENT_EXT_POINT = "event";

    private static final String EXTENDED_INFO_EXT_POINT = "extendedInfo";

    private static final String ADAPTER_POINT = "adapter";

    protected static final Log log = LogFactory.getLog(NXAuditEventsService.class);

    protected final Set<ExtendedInfoDescriptor> extendedInfoDescriptors = new HashSet<ExtendedInfoDescriptor>();

    // the adapters that will injected in the EL context for extended
    // information
    protected final Set<AdapterDescriptor> documentAdapters = new HashSet<AdapterDescriptor>();

    protected final Set<String> eventNames = new HashSet<String>();

    protected final ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(
            new ExpressionFactoryImpl());

    protected PersistenceProvider persistenceProvider;

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
    public void deactivate(ComponentContext context) throws Exception {
        deactivatePersistenceProvider();
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(EVENT_EXT_POINT)) {
            doRegisterEvent((EventDescriptor) contribution);
        } else if (extensionPoint.equals(EXTENDED_INFO_EXT_POINT)) {
            doRegisterExtendedInfo((ExtendedInfoDescriptor) contribution);
        } else if (extensionPoint.equals(ADAPTER_POINT)) {
            doRegisterAdapter((AdapterDescriptor) contribution);
        }
    }

    protected void doRegisterEvent(EventDescriptor desc) {
        String eventName = desc.getName();
        boolean eventEnabled = desc.getEnabled();
        if (eventEnabled) {
            eventNames.add(eventName);
            if (log.isDebugEnabled()) {
                log.debug("Registered event: " + eventName);
            }
        } else if (eventNames.contains(eventName) && !eventEnabled) {
            doUnregisterEvent(desc);
        }
    }

    protected void doRegisterExtendedInfo(ExtendedInfoDescriptor desc) {
        if (log.isDebugEnabled()) {
            log.debug("Registered extended info mapping : " + desc.getKey());
        }
        extendedInfoDescriptors.add(desc);
    }

    protected void doRegisterAdapter(AdapterDescriptor desc) {
        if (log.isDebugEnabled()) {
            log.debug("Registered adapter : " + desc.getName());
        }
        documentAdapters.add(desc);
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(EVENT_EXT_POINT)) {
            doUnregisterEvent((EventDescriptor) contribution);
        } else if (extensionPoint.equals(EXTENDED_INFO_EXT_POINT)) {
            doUnregisterExtendedInfo((ExtendedInfoDescriptor) contribution);
        } else if (extensionPoint.equals(ADAPTER_POINT)) {
            doUnregisterAdapter((AdapterDescriptor) contribution);
        }
    }

    protected void doUnregisterEvent(EventDescriptor desc) {
        eventNames.remove(desc.getName());
        if (log.isDebugEnabled()) {
            log.debug("Unregistered event: " + desc.getName());
        }
    }

    protected void doUnregisterExtendedInfo(ExtendedInfoDescriptor desc) {
        // FIXME: this doesn't look right
        extendedInfoDescriptors.remove(desc.getKey());
        if (log.isDebugEnabled()) {
            log.debug("Unregistered extended info: " + desc.getKey());
        }
    }

    protected void doUnregisterAdapter(AdapterDescriptor desc) {
        // FIXME: this doesn't look right
        documentAdapters.remove(desc.getName());
        if (log.isDebugEnabled()) {
            log.debug("Unregistered adapter: " + desc.getName());
        }
    }

    public Set<String> getAuditableEventNames() {
        return eventNames;
    }

    // useful ? beside tests ?
    public AdapterDescriptor[] getRegisteredAdapters() {
        return documentAdapters.toArray(new AdapterDescriptor[documentAdapters.size()]);
    }

    protected void doPutExtendedInfos(LogEntry entry,
            EventContext eventContext, DocumentModel source, Principal principal) {
        if (source instanceof DeletedDocumentModel) {
            // nothing to log ; it's a light doc
            return;
        }

        ExpressionContext context = new ExpressionContext();
        if (eventContext != null) {
            expressionEvaluator.bindValue(context, "message", eventContext);
        }
        if (source != null) {
            expressionEvaluator.bindValue(context, "source", source);
            // inject now the adapters
            for (AdapterDescriptor ad : documentAdapters) {
                Object adapter = null;
                try {
                    adapter = source.getAdapter(ad.getKlass());
                } catch (Exception e) {
                    log.debug(String.format(
                            "can't get adapter for %s to log extinfo: %s",
                            source.getPathAsString(), e.getMessage()));
                }
                if (adapter != null) {
                    expressionEvaluator.bindValue(context, ad.getName(),
                            adapter);
                }
            }
        }
        if (principal != null) {
            expressionEvaluator.bindValue(context, "principal", principal);
        }

        Map<String, ExtendedInfo> extendedInfos = entry.getExtendedInfos();
        for (ExtendedInfoDescriptor descriptor : extendedInfoDescriptors) {
            String exp = descriptor.getExpression();
            Serializable value = null;
            try {
                value = expressionEvaluator.evaluateExpression(
                    context, exp, Serializable.class);
            } catch (ELException e) {
                continue;
            }
            if (value == null) {
                continue;
            }
            extendedInfos.put(descriptor.getKey(),
                    newExtendedInfo(value));
        }
    }

    protected Principal guardedPrincipal(CoreSession session) {
        try {
            return session.getPrincipal();
        } catch (Exception e) {
            throw new AuditRuntimeException("Cannot get principal from "
                    + session, e);
        }
    }

    protected Principal guardedPrincipal(CoreEvent event) {
        try {
            return event.getPrincipal();
        } catch (Exception e) {
            throw new AuditRuntimeException("Cannot get principal from "
                    + event, e);
        }
    }

    protected DocumentModel guardedDocument(CoreSession session,
            DocumentRef reference) {
        if (session == null) {
            return null;
        }
        if (reference == null) {
            return null;
        }
        try {
            return session.getDocument(reference);
        } catch (ClientException e) {
            return null;
        }
    }

    protected DocumentModelList guardedDocumentChildren(CoreSession session,
            DocumentRef reference) throws AuditException {
        try {
            return session.getChildren(reference);
        } catch (ClientException e) {
            throw new AuditException("Cannot get children of " + reference, e);
        }
    }

    protected RepositoryManager guardedRepositoryManager() {
        try {
            return Framework.getService(RepositoryManager.class);
        } catch (Exception e) {
            throw new AuditRuntimeException("Unable to get RepositoryManager",
                    e);
        }
    }

    protected Repository guardeRepository(String repoId) {
        RepositoryManager manager = guardedRepositoryManager();
        Repository repository = manager.getRepository(repoId);
        if (repository == null) {
            throw new AuditRuntimeException("Can not find repository");
        }
        return repository;
    }

    protected CoreSession guardedCoreSession(String repoId) {
        Repository repository = guardeRepository(repoId);
        try {
            return repository.open();
        } catch (Exception e) {
            throw new AuditRuntimeException("Cannot open core session for "
                    + repoId, e);
        }
    }

    protected LogEntry doCreateAndFillEntryFromDocument(DocumentModel doc,
            Principal principal) {
        LogEntry entry = newLogEntry();
        entry.setDocPath(doc.getPathAsString());
        entry.setDocType(doc.getType());
        entry.setDocUUID(doc.getId());
        entry.setPrincipalName(SecurityConstants.SYSTEM_USERNAME);
        entry.setCategory("eventDocumentCategory");
        entry.setEventId(DocumentEventTypes.DOCUMENT_CREATED);
        // why hard-code it if we have the document life cycle?
        entry.setDocLifeCycle("project");
        Calendar creationDate;
        try {
            creationDate = (Calendar) doc.getProperty("dublincore", "created");
        } catch (ClientException e) {
            throw new AuditRuntimeException(
                    "Cannot fetch date from dublin core for " + doc, e);
        }
        if (creationDate != null) {
            entry.setEventDate(creationDate.getTime());
        }

        doPutExtendedInfos(entry, null, doc, principal);

        return entry;
    }

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

    public void addLogEntries(EntityManager em, List<LogEntry> entries) {
        LogEntryProvider.createProvider(em).addLogEntries(entries);
    }

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

    public List<LogEntry> getLogEntriesFor(EntityManager em, String uuid) {
        return LogEntryProvider.createProvider(em).getLogEntriesFor(uuid);
    }

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

    public List<LogEntry> getLogEntriesFor(EntityManager em, String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        return LogEntryProvider.createProvider(em).getLogEntriesFor(uuid,
                filterMap, doDefaultSort);
    }

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

    public LogEntry getLogEntryByID(EntityManager em, long id) {
        return LogEntryProvider.createProvider(em).getLogEntryByID(id);
    }

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

    public List<LogEntry> nativeQueryLogs(EntityManager em, String whereClause,
            int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQueryLogs(whereClause,
                pageNb, pageSize);
    }

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

    public List<?> nativeQuery(EntityManager em, String query, int pageNb,
            int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQuery(query, pageNb,
                pageSize);
    }

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

    public List<?> nativeQuery(EntityManager em, String query,
            Map<String, Object> params, int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQuery(query, params,
                pageNb, pageSize);
    }

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

    public List<LogEntry> queryLogs(EntityManager em, String[] eventIds,
            String dateRange) {
        return LogEntryProvider.createProvider(em).queryLogs(eventIds,
                dateRange);
    }

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

    public List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds,
            String dateRange, String[] category, String path, int pageNb,
            int pageSize) {
        try {
            return LogEntryProvider.createProvider(em).queryLogsByPage(eventIds,
                    dateRange,category,path,pageNb,pageSize);
        }
        catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

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

    public List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds,
            Date limit, String[] category, String path, int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).queryLogsByPage(eventIds,
                limit, category, path, pageNb, pageSize);
    }

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

    public long syncLogCreationEntries(EntityManager em, String repoId,
            String path, Boolean recurs) {
        LogEntryProvider provider = LogEntryProvider.createProvider(em);
        provider.removeEntries(DocumentEventTypes.DOCUMENT_CREATED, path);
        CoreSession session = guardedCoreSession(repoId);
        DocumentRef rootRef = new PathRef(path);
        DocumentModel root = guardedDocument(session, rootRef);
        long nbAddedEntries = doSyncNode(provider, session, root, recurs);

        if (log.isDebugEnabled()) {
            log.debug("synced " + nbAddedEntries + " entries on " + path);
        }

        return nbAddedEntries;
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
        if (! processEvents) {
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
            logDocumentEvent(em, event.getName(), docCtx, new Date(
                    event.getTime()));
        } else {
            logMiscEvent(em, event.getName(), ctx, new Date(event.getTime()));
        }
    }

    protected void logDocumentEvent(EntityManager em, String eventName,
            DocumentEventContext docCtx, Date eventDate) {
        DocumentModel document = docCtx.getSourceDocument();
        Principal principal = docCtx.getPrincipal();
        Map<String, Serializable> properties = docCtx.getProperties();

        LogEntry entry = newLogEntry();
        entry.setEventId(eventName);
        entry.setEventDate(eventDate);
        if (document != null) {
            entry.setDocUUID(document.getId());
            entry.setDocPath(document.getPathAsString());
            entry.setDocType(document.getType());
        } else {
            log.warn("received event " + eventName + " with null document");
        }
        if (principal != null) {
            String originatingUser = null;
            if (principal instanceof NuxeoPrincipal) {
                originatingUser = ((NuxeoPrincipal) principal).getOriginatingUser();
            }
            entry.setPrincipalName(originatingUser == null ? principal.getName()
                    : originatingUser);
        } else {
            log.warn("received event " + eventName + " with null principal");
        }
        entry.setComment((String) properties.get("comment"));
        try {
            if (document.isLifeCycleLoaded()) {
                entry.setDocLifeCycle(document.getCurrentLifeCycleState());
            }
        } catch (UnsupportedOperationException uoe) {
            entry.setComment("Document does not exist anymore!");
            log.debug("Document associated to event does not exists anymore");
        } catch (ClientException e1) {
            throw new AuditRuntimeException(
                    "Cannot fetch life cycle state from " + document, e1);
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
            String originatingUser = null;
            if (principal instanceof NuxeoPrincipal) {
                originatingUser = ((NuxeoPrincipal) principal).getOriginatingUser();
            }
            entry.setPrincipalName(originatingUser == null ? principal.getName()
                    : originatingUser);
        }
        entry.setComment((String) properties.get("comment"));

        String category = (String) properties.get("category");
        entry.setCategory(category);

        doPutExtendedInfos(entry, ctx, null, principal);

        addLogEntry(em, entry);
    }

    // Compat APIs

    public List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds, String dateRange,
            String category, String path, int pageNb, int pageSize) {
        String[] categories = {category};
        return queryLogsByPage(em,eventIds,dateRange,
                categories, path,pageNb,pageSize);
    }

    public List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange,
            String category, String path, int pageNb, int pageSize) {
        String[] categories = {category};
        return queryLogsByPage(eventIds,dateRange,
                categories, path,pageNb,pageSize);
    }

    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String category, String path, int pageNb, int pageSize) {
        String[] categories = {category};
        return queryLogsByPage(eventIds,limit,
                categories, path,pageNb,pageSize);
    }

    public List<LogEntry> queryLogsByPage(EntityManager em,String[] eventIds, Date limit,
            String category, String path, int pageNb, int pageSize) {
        String[] categories = {category};
        return queryLogsByPage(em,eventIds,limit,
                categories, path,pageNb,pageSize);
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
