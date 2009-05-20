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

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.audit.api.AuditAdmin;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.PersistenceProvider.RunCallback;
import org.nuxeo.ecm.platform.audit.service.extension.EventDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

import com.sun.el.ExpressionFactoryImpl;

/**
 * Event service configuration.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class NXAuditEventsService extends DefaultComponent implements NXAuditEvents {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.audit.service.NXAuditEventsService");

    private static final String EVENT_EXT_POINT = "event";

    private static final Set<String> eventNames = new HashSet<String>();

    private static final String EXTENDED_INFO_EXT_POINT = "extendedInfo";

    protected static final Set<ExtendedInfoDescriptor> extendedInfoDescriptors = new HashSet<ExtendedInfoDescriptor>();

    private static final Log log = LogFactory.getLog(NXAuditEventsService.class);

    protected static final ContainerManagedHibernateConfiguration hibernateConfiguration
            = new ContainerManagedHibernateConfiguration();

    public static final PersistenceProvider persistenceProvider = new PersistenceProvider(
            hibernateConfiguration);

    private static final ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(
            new ExpressionFactoryImpl());

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        String datasourceProperty = Framework.getProperty("audit.datasource");
        if (datasourceProperty != null) {
            hibernateConfiguration.datasource = datasourceProperty;
        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        persistenceProvider.closePersistenceUnit();
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
        }
    }

    protected void doRegisterEvent(EventDescriptor desc) {
        String eventName = desc.getName();
        Boolean eventEnabled = desc.getEnabled();
        if (eventEnabled == null) {
            eventEnabled = true;
        }
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

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(EVENT_EXT_POINT)) {
            doUnregisterEvent((EventDescriptor) contribution);
        } else if (extensionPoint.equals(EXTENDED_INFO_EXT_POINT)) {
            doUnregisterExtendedInfo((ExtendedInfoDescriptor) contribution);
        }
    }

    protected void doUnregisterEvent(EventDescriptor desc) {
        eventNames.remove(desc.getName());
        if (log.isDebugEnabled()) {
            log.debug("Unregistered event: " + desc.getName());
        }
    }

    protected void doUnregisterExtendedInfo(ExtendedInfoDescriptor desc) {
        extendedInfoDescriptors.remove(desc.getKey());
        if (log.isDebugEnabled()) {
            log.debug("Unregistered extended info: " + desc.getKey());
        }
    }

    public Set<String> getAuditableEventNames() {
        return eventNames;
    }

    protected void doPutExtendedInfos(LogEntry entry,
            EventContext eventContext, DocumentModel source,
            Principal principal) {
        ExpressionContext context = new ExpressionContext();
        if (eventContext != null) {
            expressionEvaluator.bindValue(context, "message", eventContext);
        }
        if (source != null) {
            expressionEvaluator.bindValue(context, "source", source);
        }
        if (principal != null) {
            expressionEvaluator.bindValue(context, "principal", principal);
        }
        Map<String, ExtendedInfo> extendedInfos = entry.getExtendedInfos();
        for (ExtendedInfoDescriptor descriptor : extendedInfoDescriptors) {
            Serializable value = expressionEvaluator.evaluateExpression(
                    context, descriptor.getExpression(), Serializable.class);
            if (value == null) {
                continue;
            }
            extendedInfos.put(descriptor.getKey(),
                    ExtendedInfo.createExtendedInfo(value));
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
        LogEntry entry = new LogEntry();
        entry.setDocPath(doc.getPathAsString());
        entry.setDocType(doc.getType());
        entry.setDocUUID(doc.getId());
        entry.setPrincipalName("system");
        entry.setCategory("eventDocumentCategory");
        entry.setEventId(DocumentEventTypes.DOCUMENT_CREATED);
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

    public void addLogEntries(List<LogEntry> entries) {
        EntityManager em = persistenceProvider.acquireEntityManagerWithActiveTransaction();
        try {
            addLogEntries(em, entries);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    public void addLogEntries(EntityManager em, List<LogEntry> entries) {
        LogEntryProvider.createProvider(em).addLogEntries(entries);
    }

    public List<LogEntry> getLogEntriesFor(final String uuid) {
        return persistenceProvider.run(false,
                new RunCallback<List<LogEntry>>() {
                    public List<LogEntry> runWith(EntityManager em) {
                        return getLogEntriesFor(em, uuid);
                    }

                });
    }

    public List<LogEntry> getLogEntriesFor(EntityManager em, String uuid) {
        return LogEntryProvider.createProvider(em).getLogEntriesFor(uuid);
    }

    public List<LogEntry> getLogEntriesFor(final String uuid,
            final Map<String, FilterMapEntry> filterMap,
            final boolean doDefaultSort) {
        return persistenceProvider.run(false,
                new RunCallback<List<LogEntry>>() {
                    public List<LogEntry> runWith(EntityManager em) {
                        return getLogEntriesFor(em, uuid, filterMap,
                                doDefaultSort);
                    }
                });
    }

    public List<LogEntry> getLogEntriesFor(EntityManager em, String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        return LogEntryProvider.createProvider(em).getLogEntriesFor(uuid,
                filterMap, doDefaultSort);
    }

    public LogEntry getLogEntryByID(final long id) {
        return persistenceProvider.run(false, new RunCallback<LogEntry>() {
            public LogEntry runWith(EntityManager em) {
                return getLogEntryByID(em, id);
            }
        });
    }

    public LogEntry getLogEntryByID(EntityManager em, long id) {
        return LogEntryProvider.createProvider(em).getLogEntryByID(id);
    }

    public List<LogEntry> nativeQueryLogs(final String whereClause,
            final int pageNb, final int pageSize) {
        return persistenceProvider.run(false,
                new RunCallback<List<LogEntry>>() {
                    public List<LogEntry> runWith(EntityManager em) {
                        return nativeQueryLogs(em, whereClause, pageNb,
                                pageSize);
                    }
                });
    }

    public List<LogEntry> nativeQueryLogs(EntityManager em, String whereClause,
            int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQueryLogs(whereClause,
                pageNb, pageSize);
    }

    public List<?> nativeQuery(final String query, final int pageNb, final int pageSize) {
        return persistenceProvider.run(false,
                new RunCallback<List<?>>() {
                    public List<?> runWith(EntityManager em) {
                        return nativeQuery(em, query, pageNb,
                                pageSize);
                    }
                });
    }

    public List<?> nativeQuery(EntityManager em, String query,
            int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQuery(query,
                pageNb, pageSize);
    }

    public List<LogEntry> queryLogs(final String[] eventIds,
            final String dateRange) {
        return persistenceProvider.run(false,
                new RunCallback<List<LogEntry>>() {
                    public List<LogEntry> runWith(EntityManager em) {
                        return queryLogs(em, eventIds, dateRange);
                    }
                });
    }

    public List<LogEntry> queryLogs(EntityManager em, String[] eventIds,
            String dateRange) {
        return LogEntryProvider.createProvider(em).queryLogs(eventIds,
                dateRange);
    }

    public List<LogEntry> queryLogsByPage(final String[] eventIds,
            final String dateRange, final String category, final String path,
            final int pageNb, final int pageSize) {
        return persistenceProvider.run(false,
                new RunCallback<List<LogEntry>>() {
                    public List<LogEntry> runWith(EntityManager em) {
                        return queryLogsByPage(em, eventIds, dateRange,
                                category, path, pageNb, pageSize);
                    }
                });
    }

    public List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds,
            String dateRange, String category, String path, int pageNb,
            int pageSize) {
        return LogEntryProvider.createProvider(em).queryLogs(eventIds,
                dateRange);
    }

    public List<LogEntry> queryLogsByPage(final String[] eventIds,
            final Date limit, final String category, final String path,
            final int pageNb, final int pageSize) {
        return persistenceProvider.run(false,
                new RunCallback<List<LogEntry>>() {
                    public List<LogEntry> runWith(EntityManager em) {
                        return queryLogsByPage(em, eventIds, limit, category,
                                path, pageNb, pageSize);
                    }
                });
    }

    public List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds,
            Date limit, String category, String path, int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).queryLogsByPage(eventIds,
                limit, category, path, pageNb, pageSize);
    }

    public long syncLogCreationEntries(final String repoId, final String path,
            final Boolean recurs) {
        return persistenceProvider.run(true, new RunCallback<Long>() {
            public Long runWith(EntityManager em) {
                return syncLogCreationEntries(em, repoId, path, recurs);
            }
        });
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
            log.debug("synched " + nbAddedEntries + " entries on " + path);
        }

        return nbAddedEntries;
    }

    protected long doSyncNode(LogEntryProvider provider, CoreSession session,
            DocumentModel node, boolean recurs) {

        long nbSynchedEntries = 1;

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
                    nbSynchedEntries += 1;
                }
            }
        } catch (AuditException e) {
            throw new AuditRuntimeException("error occured while synching", e);
        }

        if (recurs) {
            for (DocumentModel folderChild : folderishChildren) {
                nbSynchedEntries += doSyncNode(provider, session, folderChild,
                        recurs);
            }
        }

        return nbSynchedEntries;
    }

    public void addLogEntry(final LogEntry entry) {
        persistenceProvider.run(true, new RunCallback<Integer>() {
            public Integer runWith(EntityManager em) {
                addLogEntry(em, entry);
                return 0;
            }
        });
    }

    public void addLogEntry(EntityManager em, LogEntry entry) {
        LogEntryProvider.createProvider(em).addLogEntry(entry);
    }

    public Long getEventsCount(final String eventId) {
        return persistenceProvider.run(false, new RunCallback<Long>() {
            public Long runWith(EntityManager em) {
                return getEventsCount(em, eventId);
            }

        });
    }

    public Long getEventsCount(EntityManager em, String eventId) {
        return LogEntryProvider.createProvider(em).countEventsById(eventId);
    }

    public List<String> getLoggedEventIds() {
        return persistenceProvider.run(false, new RunCallback<List<String>>() {
            public List<String> runWith(EntityManager em) {
                return getLoggedEventIds(em);
            }

        });
    }

    public List<String> getLoggedEventIds(EntityManager em) {
        return LogEntryProvider.createProvider(em).findEventIds();
    }

    public void logEvent(final Event event) throws AuditException {
        AuditException ae = null;
        ae = persistenceProvider.run(true, new RunCallback<AuditException>() {
            public AuditException runWith(EntityManager em) {
                logEvent(em, event);
                return null;
            }
        });
        if (ae != null) {
            throw ae;
        }
    }

    public void logEvents(final EventBundle eventBundle) throws AuditException {
        AuditException ae = null;
        ae = persistenceProvider.run(true, new RunCallback<AuditException>() {
            public AuditException runWith(EntityManager em) {
                logEvents(em, eventBundle);
                return null;
            }
        });
        if (ae != null) {
            throw ae;
        }
    }

    public void logEvents(EntityManager em, EventBundle eventBundle) {
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

        LogEntry entry = new LogEntry();
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
            entry.setPrincipalName(principal.getName());
        } else {
            log.warn("received event " + eventName + " with null principal");
        }
        entry.setComment((String) properties.get("comment"));
        try {
            if (document.isLifeCycleLoaded()) {
                entry.setDocLifeCycle(document.getCurrentLifeCycleState());
            }
        } catch (ClientException e1) {
            throw new AuditRuntimeException(
                    "Cannot fetch life cycle state from " + document, e1);
        }
        entry.setCategory("eventDocumentCategory");

        doPutExtendedInfos(entry, docCtx, document, principal);

        addLogEntry(em, entry);
    }

    protected void logMiscEvent(EntityManager em, String eventName,
            EventContext ctx, Date eventDate) {
        Principal principal = ctx.getPrincipal();
        Map<String, Serializable> properties = ctx.getProperties();

        LogEntry entry = new LogEntry();
        entry.setEventId(eventName);
        entry.setEventDate(eventDate);
        entry.setPrincipalName(principal.getName());
        entry.setComment((String) properties.get("comment"));

        String category = (String) properties.get("category");
        entry.setCategory(category);

        doPutExtendedInfos(entry, ctx, null, principal);

        addLogEntry(em, entry);
    }



}
