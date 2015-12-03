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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.el.ELException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;

/**
 * Abstract class to share code between {@link AuditBackend} implementations
 *
 * @author tiry
 */
public abstract class AbstractAuditBackend implements AuditBackend {

    protected static final Log log = LogFactory.getLog(AbstractAuditBackend.class);

    public static final String FORCE_AUDIT_FACET = "ForceAudit";

    protected NXAuditEventsService component;

    @Override
    public void activate(NXAuditEventsService component) {
        this.component = component;
    }

    protected final ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(new ExpressionFactoryImpl());

    protected DocumentModel guardedDocument(CoreSession session, DocumentRef reference) {
        if (session == null) {
            return null;
        }
        if (reference == null) {
            return null;
        }
        try {
            return session.getDocument(reference);
        } catch (DocumentNotFoundException e) {
            return null;
        }
    }

    protected DocumentModelList guardedDocumentChildren(CoreSession session, DocumentRef reference) {
        return session.getChildren(reference);
    }

    protected LogEntry doCreateAndFillEntryFromDocument(DocumentModel doc, Principal principal) {
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
        Calendar creationDate = (Calendar) doc.getProperty("dublincore", "created");
        if (creationDate != null) {
            entry.setEventDate(creationDate.getTime());
        }

        doPutExtendedInfos(entry, null, doc, principal);

        return entry;
    }

    protected void doPutExtendedInfos(LogEntry entry, EventContext eventContext, DocumentModel source,
            Principal principal) {

        ExpressionContext context = new ExpressionContext();
        if (eventContext != null) {
            expressionEvaluator.bindValue(context, "message", eventContext);
        }
        if (source != null) {
            expressionEvaluator.bindValue(context, "source", source);
            // inject now the adapters
            for (AdapterDescriptor ad : component.getDocumentAdapters()) {
                if (source instanceof DeletedDocumentModel) {
                    continue; // skip
                }
                Object adapter = source.getAdapter(ad.getKlass());
                if (adapter != null) {
                    expressionEvaluator.bindValue(context, ad.getName(), adapter);
                }
            }
        }
        if (principal != null) {
            expressionEvaluator.bindValue(context, "principal", principal);
        }

        // Global extended info
        populateExtendedInfo(entry, source, context,  component.getExtendedInfoDescriptors());
        // Event id related extended info
        populateExtendedInfo(entry, source, context,  component.getEventExtendedInfoDescriptors().get(entry.getEventId()));

        if (eventContext != null) {
            @SuppressWarnings("unchecked")
            Map<String, Serializable> map = (Map<String, Serializable>) eventContext.getProperty("extendedInfos");
            if (map != null) {
                Map<String, ExtendedInfo> extendedInfos = entry.getExtendedInfos();
                for (Entry<String, Serializable> en : map.entrySet()) {
                    Serializable value = en.getValue();
                    if (value != null) {
                        extendedInfos.put(en.getKey(), newExtendedInfo(value));
                    }
                }
            }
        }
    }

    /**
     * @since 7.4
     */
    protected void populateExtendedInfo(LogEntry entry, DocumentModel source, ExpressionContext context,
            Collection<ExtendedInfoDescriptor> extInfos) {
        if (extInfos != null) {
            Map<String, ExtendedInfo> extendedInfos = entry.getExtendedInfos();
            for (ExtendedInfoDescriptor descriptor : extInfos) {
                String exp = descriptor.getExpression();
                Serializable value = null;
                try {
                    value = expressionEvaluator.evaluateExpression(context, exp, Serializable.class);
                } catch (PropertyException | UnsupportedOperationException e) {
                    if (source instanceof DeletedDocumentModel) {
                        log.debug("Can not evaluate the expression: " + exp + " on a DeletedDocumentModel, skipping.");
                    }
                    continue;
                } catch (ELException e) {
                    continue;
                }
                if (value == null) {
                    continue;
                }
                extendedInfos.put(descriptor.getKey(), newExtendedInfo(value));
            }
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
            if (document.hasFacet(SYSTEM_DOCUMENT) && !document.hasFacet(FORCE_AUDIT_FACET)) {
                // do not log event on System documents
               // unless it has the FORCE_AUDIT_FACET facet
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
            }
            if (principal != null) {
                String principalName = null;
                if (principal instanceof NuxeoPrincipal) {
                    principalName = ((NuxeoPrincipal) principal).getActingUser();
                } else {
                    principalName = principal.getName();
                }
                entry.setPrincipalName(principalName);
            } else {
                log.warn("received event " + eventName + " with null principal");
            }
            entry.setComment((String) properties.get("comment"));
            if (document instanceof DeletedDocumentModel) {
                entry.setComment("Document does not exist anymore!");
            } else {
                if (document.isLifeCycleLoaded()) {
                    entry.setDocLifeCycle(document.getCurrentLifeCycleState());
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
                String principalName;
                if (principal instanceof NuxeoPrincipal) {
                    principalName = ((NuxeoPrincipal) principal).getActingUser();
                } else {
                    principalName = principal.getName();
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

    public List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange, String category, String path,
            int pageNb, int pageSize) {
        String[] categories = { category };
        return queryLogsByPage(eventIds, dateRange, categories, path, pageNb, pageSize);
    }

    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit, String category, String path, int pageNb,
            int pageSize) {
        String[] categories = { category };
        return queryLogsByPage(eventIds, limit, categories, path, pageNb, pageSize);
    }

    @Override
    public LogEntry newLogEntry() {
        return new LogEntryImpl();
    }

    @Override
    public abstract ExtendedInfo newExtendedInfo(Serializable value);

    protected long syncLogCreationEntries(BaseLogEntryProvider provider, String repoId, String path, Boolean recurs) {

        provider.removeEntries(DocumentEventTypes.DOCUMENT_CREATED, path);
        try (CoreSession session = CoreInstance.openCoreSession(repoId)) {
            DocumentRef rootRef = new PathRef(path);
            DocumentModel root = guardedDocument(session, rootRef);
            long nbAddedEntries = doSyncNode(provider, session, root, recurs);

            if (log.isDebugEnabled()) {
                log.debug("synced " + nbAddedEntries + " entries on " + path);
            }

            return nbAddedEntries;
        }
    }

    protected long doSyncNode(BaseLogEntryProvider provider, CoreSession session, DocumentModel node, boolean recurs) {

        long nbSyncedEntries = 1;

        Principal principal = session.getPrincipal();
        List<DocumentModel> folderishChildren = new ArrayList<DocumentModel>();

        provider.addLogEntry(doCreateAndFillEntryFromDocument(node, session.getPrincipal()));

        for (DocumentModel child : guardedDocumentChildren(session, node.getRef())) {
            if (child.isFolder() && recurs) {
                folderishChildren.add(child);
            } else {
                provider.addLogEntry(doCreateAndFillEntryFromDocument(child, principal));
                nbSyncedEntries += 1;
            }
        }

        if (recurs) {
            for (DocumentModel folderChild : folderishChildren) {
                nbSyncedEntries += doSyncNode(provider, session, folderChild, recurs);
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
        return getLogEntriesFor(uuid, Collections.<String, FilterMapEntry> emptyMap(), false);
    }

    @Override
    public List<?> nativeQuery(String query, int pageNb, int pageSize) {
        return nativeQuery(query, Collections.<String, Object> emptyMap(), pageNb, pageSize);
    }

    @Override
    public List<LogEntry> queryLogs(final String[] eventIds, final String dateRange) {
        return queryLogsByPage(eventIds, (String) null, (String[]) null, null, 0, 10000);
    }

    public List<LogEntry> nativeQueryLogs(final String whereClause, final int pageNb, final int pageSize) {
        List<LogEntry> entries = new LinkedList<>();
        for (Object entry : nativeQuery(whereClause, pageNb, pageSize)) {
            if (entry instanceof LogEntry) {
                entries.add((LogEntry) entry);
            }
        }
        return entries;
    }

}
