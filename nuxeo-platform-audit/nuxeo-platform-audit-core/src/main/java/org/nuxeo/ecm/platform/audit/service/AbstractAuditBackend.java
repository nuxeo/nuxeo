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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ELException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;

/**
 * Abstract class to share code between {@link AuditBackend} implementations
 * 
 * @author tiry
 * 
 */
public abstract class AbstractAuditBackend implements AuditBackend {

    protected static final Log log = LogFactory.getLog(AbstractAuditBackend.class);

    protected NXAuditEventsService component;

    @Override
    public void activate(NXAuditEventsService component) throws Exception {
        this.component = component;
    }

    protected final ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(
            new ExpressionFactoryImpl());

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

    protected LogEntry doCreateAndFillEntryFromDocument(DocumentModel doc,
            Principal principal) {
        LogEntry entry = newLogEntry();
        entry.setDocPath(doc.getPathAsString());
        entry.setDocType(doc.getType());
        entry.setDocUUID(doc.getId());
        entry.setRepositoryId(doc.getRepositoryName());
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
            for (AdapterDescriptor ad : component.getDocumentAdapters()) {
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
        for (ExtendedInfoDescriptor descriptor : component.getExtendedInfoDescriptors()) {
            String exp = descriptor.getExpression();
            Serializable value = null;
            try {
                value = expressionEvaluator.evaluateExpression(context, exp,
                        Serializable.class);
            } catch (ELException e) {
                continue;
            }
            if (value == null) {
                continue;
            }
            extendedInfos.put(descriptor.getKey(), newExtendedInfo(value));
        }
    }

    @Override
    public Set<String> getAuditableEventNames() {
        return component.getAuditableEventNames();
    }

    protected LogEntry buildEntryFromEvent(Event event) {
        EventContext ctx = event.getContext();
        String eventName = event.getName();
        Date eventDate = new Date(event.getTime());

        if (!getAuditableEventNames().contains(event.getName())) {
            return null;
        }

        LogEntry entry = newLogEntry();
        entry.setEventId(eventName);
        entry.setEventDate(eventDate);

        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            DocumentModel document = docCtx.getSourceDocument();
            if (document.hasFacet(SYSTEM_DOCUMENT)) {
                // do not log event on System documents
                return null;
            }

            Boolean disabled = (Boolean) docCtx.getProperty(NXAuditEventsService.DISABLE_AUDIT_LOGGER);
            if (disabled != null && disabled) {
                // don't log events with this flag
                return null;
            }
            Principal principal = docCtx.getPrincipal();
            Map<String, Serializable> properties = docCtx.getProperties();

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
                            "Cannot fetch life cycle state from " + document,
                            e1);
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

        } else {
            Principal principal = ctx.getPrincipal();
            Map<String, Serializable> properties = ctx.getProperties();

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

        }

        return entry;
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

    @Override
    public LogEntry newLogEntry() {
        return new LogEntryImpl();
    }

    @Override
    public ExtendedInfo newExtendedInfo(Serializable value) {
        return ExtendedInfoImpl.createExtendedInfo(value);
    }

    protected long syncLogCreationEntries(LogEntryProvider provider,
            String repoId, String path, Boolean recurs) {

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

    // Default implementations to avoid to have too much code to write in actual
    // implementation
    //
    // these methods are actually overridden in the JPA implementation for
    // optimization purpose

    @Override
    public void logEvents(EventBundle eventBundle) {
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
            logEvent(event);
        }
    }

    @Override
    public void logEvent(Event event) {
        LogEntry entry = buildEntryFromEvent(event);
        if (entry != null) {
            List<LogEntry> entries = new ArrayList<>();
            entries.add(entry);
            addLogEntries(entries);
        }
    }

    @Override
    public List<LogEntry> getLogEntriesFor(String uuid) {
        return getLogEntriesFor(uuid,
                Collections.<String, FilterMapEntry> emptyMap(), false);
    }

    @Override
    public List<?> nativeQuery(String query, int pageNb, int pageSize) {
        return nativeQuery(query, Collections.<String, Object> emptyMap(),
                pageNb, pageSize);
    }

    @Override
    public List<LogEntry> queryLogs(final String[] eventIds,
            final String dateRange) {
        return queryLogsByPage(eventIds, (String) null, (String[]) null, null,
                0, 10000);
    }

    public List<LogEntry> nativeQueryLogs(final String whereClause,
            final int pageNb, final int pageSize) {
        List<LogEntry> entries = new LinkedList<>();
        for (Object entry : nativeQuery(whereClause, pageNb, pageSize)) {
            if (entry instanceof LogEntry) {
                entries.add((LogEntry) entry);
            }
        }
        return entries;
    }

}
