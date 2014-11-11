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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.extension.EventDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.HibernateOptionsDescriptor;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;

import com.sun.el.ExpressionFactoryImpl;

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

    private static final Set<String> eventNames = new HashSet<String>();

    private static final String EXTENDED_INFO_EXT_POINT = "extendedInfo";

    protected static final Set<ExtendedInfoDescriptor> extendedInfoDescriptors = new HashSet<ExtendedInfoDescriptor>();

    private static final long serialVersionUID = -7945111177284985820L;

    private static final Log log = LogFactory.getLog(NXAuditEventsService.class);

    private static final String HIBERNATE_OPTIONS_EXT_POINT = "hibernateOptions";

    protected static final ContainerManagedHibernateConfiguration hibernateConfiguration = new ContainerManagedHibernateConfiguration();

    public static final PersistenceProvider persistenceProvider = new PersistenceProvider(
            hibernateConfiguration);

    private static final ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(
            new ExpressionFactoryImpl());

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        persistenceProvider.closePersistenceUnit();
        super.deactivate(context);
    }

    @Override
    public void registerExtension(Extension extension) {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals(EVENT_EXT_POINT)) {
                for (Object contribution : contributions) {
                    EventDescriptor desc = (EventDescriptor) contribution;
                    String eventName = desc.getName();
                    Boolean eventEnabled = desc.getEnabled();
                    if (eventEnabled == null) {
                        eventEnabled = true;
                    }
                    if (eventEnabled) {
                        eventNames.add(eventName);
                        if (log.isDebugEnabled())
                            log.debug("Registered event: " + eventName);
                    } else if (eventNames.contains(eventName) && !eventEnabled) {
                        eventNames.remove(eventName);
                        if (log.isDebugEnabled())
                            log.debug("Unregistered event: " + eventName);
                    }
                }
            }

            // TODO add append and enabling behaviours
            else if (extension.getExtensionPoint().equals(
                    EXTENDED_INFO_EXT_POINT)) {
                for (Object contribution : contributions) {
                    ExtendedInfoDescriptor desc = (ExtendedInfoDescriptor) contribution;
                    if (log.isDebugEnabled())
                        log.debug("Registered extended info mapping : "
                                + desc.getKey());

                    extendedInfoDescriptors.add(desc);

                }
                return;
            }

            else if (extension.getExtensionPoint().equals(
                    HIBERNATE_OPTIONS_EXT_POINT)) {
                for (Object contribution : contributions) {
                    HibernateOptionsDescriptor desc = (HibernateOptionsDescriptor) contribution;
                    if (log.isDebugEnabled())
                        log.debug("Registered hibernate datasource : "
                                + desc.getDatasource());
                    hibernateConfiguration.setDescriptor(desc);
                }
                return;
            }
        }
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals(EVENT_EXT_POINT)) {
                for (Object contribution : contributions) {
                    EventDescriptor desc = (EventDescriptor) contribution;
                    eventNames.remove(desc.getName());
                    if (log.isDebugEnabled())
                        log.debug("Unregistered event: " + desc.getName());
                }
            }
            if (extension.getExtensionPoint().equals(EXTENDED_INFO_EXT_POINT)) {
                for (Object contribution : contributions) {
                    ExtendedInfoDescriptor desc = (ExtendedInfoDescriptor) contribution;
                    extendedInfoDescriptors.remove(desc.getKey());
                    if (log.isDebugEnabled())
                        log.debug("Unregistered extended info: "
                                + desc.getKey());
                }
            }
        }
    }

    public Set<String> getAuditableEventNames() {
        return eventNames;
    }

    protected void doPutExtendedInfos(LogEntry entry, DocumentMessage message,
            DocumentModel source, NuxeoPrincipal principal) {
        ExpressionContext context = new ExpressionContext();
        if (message != null) {
            expressionEvaluator.bindValue(context, "message", message);
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

    protected NuxeoPrincipal doCastPrincipal(Principal principal) {
        if (!(principal instanceof NuxeoPrincipal)) {
            if (log.isWarnEnabled())
                log.warn("not a nuxeo principal " + principal);
            return null;
        }
        return (NuxeoPrincipal) principal;
    }

    protected NuxeoPrincipal guardedPrincipal(DocumentMessage message) {
        try {
            return doCastPrincipal(message.getPrincipal());
        } catch (Exception e) {
            throw new AuditRuntimeException("Cannot get principal from "
                    + message, e);
        }
    }

    protected NuxeoPrincipal guardedPrincipal(CoreSession session) {
        try {
            return doCastPrincipal(session.getPrincipal());
        } catch (Exception e) {
            throw new AuditRuntimeException("Cannot get principal from "
                    + session, e);
        }
    }

    protected NuxeoPrincipal guardedPrincipal(CoreEvent event) {
        try {
            return doCastPrincipal(event.getPrincipal());
        } catch (Exception e) {
            throw new AuditRuntimeException("Cannot get principal from "
                    + event, e);
        }
    }

    protected DocumentModel guardedDocument(CoreSession session,
            DocumentMessage message) {
        return guardedDocument(session, message.getRef());
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

    protected LogEntry doCreateAndFillEntryFromMessage(CoreSession session,
            DocumentMessage message) throws AuditException {

        DocumentModel source = guardedDocument(session, message);
        NuxeoPrincipal principal = guardedPrincipal(message);

        LogEntry entry = new LogEntry();
        entry.setEventId(message.getEventId());
        entry.setEventDate(message.getEventDate());
        entry.setDocUUID(message.getId());
        entry.setDocPath(message.getPathAsString());
        entry.setDocType(message.getType());
        entry.setPrincipalName(message.getPrincipalName());
        entry.setCategory(message.getCategory());
        entry.setDocLifeCycle(message.getDocCurrentLifeCycle());
        entry.setComment(message.getComment());

        doPutExtendedInfos(entry, message, source, principal);
        return entry;

    }

    protected LogEntry doCreateAndFillEntryFromEvent(CoreEvent event)
            throws AuditException {
        NuxeoPrincipal principal = guardedPrincipal(event);
        DocumentModel document = (DocumentModel) event.getSource();
        LogEntry entry = new LogEntry();
        entry.setEventId(event.getEventId());
        entry.setEventDate(event.getDate());
        entry.setDocUUID(document.getId());
        entry.setDocPath(document.getPathAsString());
        entry.setDocType(document.getType());
        entry.setPrincipalName(principal.getName());
        entry.setComment(event.getComment());
        try {
            if (document.isLifeCycleLoaded())
                entry.setDocLifeCycle(document.getCurrentLifeCycleState());
        } catch (ClientException e1) {
            throw new AuditRuntimeException(
                    "Cannot fetch life cycle state from " + document, e1);
        }
        entry.setCategory("eventDocumentCategory");

        DocumentMessage message = new DocumentMessageImpl(document, event);

        doPutExtendedInfos(entry, message, document, principal);

        return entry;

    }

    protected LogEntry doCreateAndFillEntryFromDocument(DocumentModel doc,
            NuxeoPrincipal principal) throws AuditException {
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

        doPutExtendedInfos(entry, (DocumentMessage) null, doc, principal);

        return entry;
    }

    public void logMessage(CoreSession session, DocumentMessage message)
            throws AuditException {
        EntityManager em = persistenceProvider.acquireEntityManagerWithActiveTransaction();
        try {
            logMessage(em, session, message);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    public void logMessage(EntityManager em, CoreSession session,
            DocumentMessage message) throws AuditException {
        String eventId = message.getEventId();
        if (eventId != null && !eventNames.contains(eventId)) {
            return;
        }
        LogEntry entry = doCreateAndFillEntryFromMessage(session, message);
        addLogEntry(em, entry);
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

    public List<LogEntry> getLogEntriesFor(String uuid) {
        EntityManager em = persistenceProvider.acquireEntityManager();
        try {
            return getLogEntriesFor(em, uuid);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    public List<LogEntry> getLogEntriesFor(EntityManager em, String uuid) {
        return LogEntryProvider.createProvider(em).getLogEntriesFor(uuid);
    }

    public List<LogEntry> getLogEntriesFor(String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        EntityManager em = persistenceProvider.acquireEntityManager();
        try {
            return getLogEntriesFor(em, uuid, filterMap, doDefaultSort);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    public List<LogEntry> getLogEntriesFor(EntityManager em, String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        return LogEntryProvider.createProvider(em).getLogEntriesFor(uuid,
                filterMap, doDefaultSort);
    }

    public LogEntry getLogEntryByID(long id) {
        EntityManager em = persistenceProvider.acquireEntityManager();
        try {
            return getLogEntryByID(em, id);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    public LogEntry getLogEntryByID(EntityManager em, long id) {
        return LogEntryProvider.createProvider(em).getLogEntryByID(id);
    }

    public List<LogEntry> nativeQueryLogs(String whereClause, int pageNb,
            int pageSize) {
        EntityManager em = persistenceProvider.acquireEntityManager();
        try {
            return nativeQueryLogs(em, whereClause, pageNb, pageSize);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    public List<LogEntry> nativeQueryLogs(EntityManager em, String whereClause,
            int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).nativeQueryLogs(whereClause,
                pageNb, pageSize);
    }

    public List<LogEntry> queryLogs(String[] eventIds, String dateRange) {
        EntityManager em = persistenceProvider.acquireEntityManager();
        try {
            return queryLogs(em, eventIds, dateRange);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    public List<LogEntry> queryLogs(EntityManager em, String[] eventIds,
            String dateRange) {
        return LogEntryProvider.createProvider(em).queryLogs(eventIds,
                dateRange);
    }

    public List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange,
            String category, String path, int pageNb, int pageSize)
            throws AuditException {
        EntityManager em = persistenceProvider.acquireEntityManager();
        try {
            return queryLogsByPage(em, eventIds, dateRange, category, path,
                    pageNb, pageSize);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    public List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds,
            String dateRange, String category, String path, int pageNb,
            int pageSize) {
        return LogEntryProvider.createProvider(em).queryLogs(eventIds,
                dateRange);
    }

    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String category, String path, int pageNb, int pageSize)
            throws AuditException {
        EntityManager em = persistenceProvider.acquireEntityManager();
        try {
            return queryLogsByPage(em, eventIds, limit, category, path, pageNb,
                    pageSize);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    public List<LogEntry> queryLogsByPage(EntityManager em, String[] eventIds,
            Date limit, String category, String path, int pageNb, int pageSize) {
        return LogEntryProvider.createProvider(em).queryLogsByPage(eventIds,
                limit, category, path, pageNb, pageSize);
    }

    public long syncLogCreationEntries(String repoId, String path,
            Boolean recurs) throws AuditException {
        EntityManager em = persistenceProvider.acquireEntityManagerWithActiveTransaction();
        try {
            return syncLogCreationEntries(em, repoId, path, recurs);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    public long syncLogCreationEntries(EntityManager em, String repoId,
            String path, Boolean recurs) throws AuditException {

        LogEntryProvider provider = LogEntryProvider.createProvider(em);
        provider.removeEntries(DocumentEventTypes.DOCUMENT_CREATED, path);
        CoreSession session = guardedCoreSession(repoId);
        DocumentRef rootRef = new PathRef(path);
        DocumentModel root = guardedDocument(session, rootRef);
        long nbAddedEntries = doSyncNode(provider, session, root, recurs);

        if (log.isDebugEnabled())
            log.debug("synched " + nbAddedEntries + " entries on " + path);

        return nbAddedEntries;
    }

    protected long doSyncNode(LogEntryProvider provider, CoreSession session,
            DocumentModel node, boolean recurs) throws AuditException {

        long nbSynchedEntries = 1;

        NuxeoPrincipal principal = guardedPrincipal(session);
        List<DocumentModel> folderishChildren = new ArrayList<DocumentModel>();

        provider.addLogEntry(doCreateAndFillEntryFromDocument(node,
                guardedPrincipal(session)));

        for (DocumentModel child : guardedDocumentChildren(session,
                node.getRef())) {
            if (child.isFolder() && recurs) {
                folderishChildren.add(child);
            } else {
                provider.addLogEntry(doCreateAndFillEntryFromDocument(child,
                        principal));
                nbSynchedEntries += 1;
            }
        }

        if (recurs) {
            for (DocumentModel folderChild : folderishChildren) {
                nbSynchedEntries += doSyncNode(provider, session, folderChild,
                        recurs);
            }
        }

        return nbSynchedEntries;
    }

    public void addLogEntry(LogEntry entry) {
        EntityManager em = persistenceProvider.acquireEntityManagerWithActiveTransaction();
        try {
            LogEntryProvider.createProvider(em).addLogEntry(entry);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    public void addLogEntry(EntityManager em, LogEntry entry) {
        LogEntryProvider.createProvider(em).addLogEntry(entry);
    }

    /**
     * @param coreEvent
     * @throws AuditException
     */
    public void logEvent(CoreEvent coreEvent) throws AuditException {
        EntityManager em = persistenceProvider.acquireEntityManagerWithActiveTransaction();
        try {
            logEvent(em, coreEvent);
        } finally {
            persistenceProvider.releaseEntityManager(em);
        }
    }

    /**
     * @param em
     * @param coreEvent
     * @throws AuditException
     */
    private void logEvent(EntityManager em, CoreEvent event)
            throws AuditException {
        String eventId = event.getEventId();
        if (eventId != null && eventNames.contains(eventId) == false)
            return;
        LogEntry entry = doCreateAndFillEntryFromEvent(event);
        addLogEntry(em, entry);
    }

}
