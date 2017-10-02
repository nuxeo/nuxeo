/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.schema.FacetNames.SYSTEM_DOCUMENT;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_CATEGORY;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_PATH;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.el.ELException;

import org.apache.commons.lang3.ArrayUtils;
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
import org.nuxeo.ecm.core.api.NuxeoException;
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
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Predicates;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
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

    protected final NXAuditEventsService component;

    protected final AuditBackendDescriptor config;

    protected AbstractAuditBackend(NXAuditEventsService component, AuditBackendDescriptor config) {
        this.component = component;
        this.config = config;
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
        populateExtendedInfo(entry, source, context, component.getExtendedInfoDescriptors());
        // Event id related extended info
        populateExtendedInfo(entry, source, context,
                component.getEventExtendedInfoDescriptors().get(entry.getEventId()));

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

    @Override
    public void logEvents(EventBundle bundle) {
        if (!isAuditable(bundle)) {
            return;
        }
        for (Event event : bundle) {
            logEvent(event);
        }
    }

    protected boolean isAuditable(EventBundle eventBundle) {
        for (String name : getAuditableEventNames()) {
            if (eventBundle.containsEventName(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void logEvent(Event event) {
        if (!getAuditableEventNames().contains(event.getName())) {
            return;
        }
        LogEntry entry = buildEntryFromEvent(event);
        if (entry == null) {
            return;
        }
        component.bulker.offer(entry);
    }

    @Override
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        return component.bulker.await(time, unit);
    }

    @Override
    @Deprecated
    public List<LogEntry> getLogEntriesFor(String uuid, Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        // create builder
        AuditQueryBuilder builder = new AuditQueryBuilder();
        // create predicates
        builder.addAndPredicate(Predicates.eq(LOG_DOC_UUID, uuid));
        filterMap.values()
                 .stream()
                 .map(this::convert)
                 .forEach(builder::addAndPredicate);
        if (doDefaultSort) {
            builder.defaultOrder();
        }
        return queryLogs(builder);
    }

    protected Predicate convert(FilterMapEntry entry) {
        String name = entry.getColumnName();
        String operator = entry.getOperator();
        Object value = entry.getObject();
        if (Operator.EQ.toString().equals(operator)) {
            return Predicates.eq(name, value);
        } else if (Operator.LT.toString().equals(operator)) {
            return Predicates.lt(name, value);
        } else if (Operator.LTEQ.toString().equals(operator)) {
            return Predicates.lte(name, value);
        } else if (Operator.GTEQ.toString().equals(operator)) {
            return Predicates.gte(name, value);
        } else if (Operator.GT.toString().equals(operator)) {
            return Predicates.gt(name, value);
        } else if (Operator.IN.toString().equals(operator)) {
            return Predicates.in(name, (List<?>) value);
        }
        throw new NuxeoException(String.format("Audit backend search doesn't handle '%s' operator", operator));
    }

    @Override
    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit, String[] categories, String path, int pageNb,
            int pageSize) {
        AuditQueryBuilder builder = new AuditQueryBuilder();
        if (ArrayUtils.isNotEmpty(eventIds)) {
            if (eventIds.length == 1) {
                builder.addAndPredicate(Predicates.eq(LOG_EVENT_ID, eventIds[0]));
            } else {
                builder.addAndPredicate(Predicates.in(LOG_EVENT_ID, eventIds[0]));
            }
        }
        if (ArrayUtils.isNotEmpty(categories)) {
            if (categories.length == 1) {
                builder.addAndPredicate(Predicates.eq(LOG_CATEGORY, categories[0]));
            } else {
                builder.addAndPredicate(Predicates.in(LOG_CATEGORY, categories[0]));
            }
        }
        if (path != null) {
            builder.addAndPredicate(Predicates.eq(LOG_DOC_PATH, path));
        }
        if (limit != null) {
            builder.addAndPredicate(Predicates.lt(LOG_EVENT_DATE, limit));
        }
        builder.offset(pageNb * pageSize).limit(pageSize);
        return queryLogs(builder);
    }

}
