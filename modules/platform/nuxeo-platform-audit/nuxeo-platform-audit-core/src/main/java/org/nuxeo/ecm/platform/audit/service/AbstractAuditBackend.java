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

import static org.nuxeo.ecm.core.schema.FacetNames.SYSTEM_DOCUMENT;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_CATEGORY;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_PATH;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_REPOSITORY_ID;
import static org.nuxeo.ecm.platform.audit.impl.StreamAuditWriter.COMPUTATION_NAME;
import static org.nuxeo.ecm.platform.audit.listener.StreamAuditEventListener.STREAM_AUDIT_ENABLED_PROP;
import static org.nuxeo.ecm.platform.audit.listener.StreamAuditEventListener.STREAM_NAME;

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
import org.apache.logging.log4j.Logger;
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
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.AuditStorage;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.ecm.platform.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

/**
 * Abstract class to share code between {@link AuditBackend} implementations
 *
 * @author tiry
 */
public abstract class AbstractAuditBackend implements AuditBackend, AuditStorage {

    protected static final Logger log = org.apache.logging.log4j.LogManager.getLogger(AbstractAuditBackend.class);

    public static final String FORCE_AUDIT_FACET = "ForceAudit";

    protected final NXAuditEventsService component;

    protected final AuditBackendDescriptor config;

    protected AbstractAuditBackend(NXAuditEventsService component, AuditBackendDescriptor config) {
        this.component = component;
        this.config = config;
    }

    protected AbstractAuditBackend() {
        this(null, null);
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
                Serializable value;
                try {
                    value = expressionEvaluator.evaluateExpression(context, exp, Serializable.class);
                } catch (PropertyException | UnsupportedOperationException e) {
                    if (source instanceof DeletedDocumentModel) {
                        log.debug("Can not evaluate the expression: {} on a DeletedDocumentModel, skipping.", exp);
                    }
                    continue;
                } catch (DocumentNotFoundException e) {
                    if (!DocumentEventTypes.DOCUMENT_REMOVED.equals(entry.getEventId())) {
                        log.error("Not found: {}, entry: {}", e.getMessage(), entry, e);
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

    @Override
    public LogEntry buildEntryFromEvent(Event event) {
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
            if (disabled != null && disabled.booleanValue()) {
                // don't log events with this flag
                return null;
            }
            NuxeoPrincipal principal = docCtx.getPrincipal();
            Map<String, Serializable> properties = docCtx.getProperties();

            entry.setDocUUID(document.getId());
            entry.setDocPath(document.getPathAsString());
            entry.setDocType(document.getType());
            entry.setRepositoryId(document.getRepositoryName());
            if (principal != null) {
                entry.setPrincipalName(principal.getActingUser());
            } else {
                log.warn("received event {} with null principal", eventName);
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
            NuxeoPrincipal principal = ctx.getPrincipal();
            Map<String, Serializable> properties = ctx.getProperties();

            if (principal != null) {
                entry.setPrincipalName(principal.getActingUser());
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
        CoreSession session = CoreInstance.getCoreSession(repoId);
        DocumentRef rootRef = new PathRef(path);
        DocumentModel root = guardedDocument(session, rootRef);
        long nbAddedEntries = doSyncNode(provider, session, root, recurs);

        log.debug("synced {}  entries on {}", nbAddedEntries, path);
        return nbAddedEntries;
    }

    protected long doSyncNode(BaseLogEntryProvider provider, CoreSession session, DocumentModel node, boolean recurs) {

        long nbSyncedEntries = 1;

        NuxeoPrincipal principal = session.getPrincipal();
        List<DocumentModel> folderishChildren = new ArrayList<>();

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
    @Deprecated
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
    @Deprecated
    public void logEvent(Event event) {
        if (!getAuditableEventNames().contains(event.getName())) {
            return;
        }
        LogEntry entry = buildEntryFromEvent(event);
        if (entry == null) {
            return;
        }
        if (Framework.isBooleanPropertyFalse(STREAM_AUDIT_ENABLED_PROP)) {
            component.bulker.offer(entry);
        } else {
            log.error("Usage of AuditLogger#logEvent while AuditBulker is disabled", new Exception());
        }
    }

    @SuppressWarnings("resource") // LogManager not ours to close
    @Override
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        if (Framework.isBooleanPropertyFalse(STREAM_AUDIT_ENABLED_PROP)) {
            return component.bulker.await(time, unit);
        } else {
            StreamService service = Framework.getService(StreamService.class);
            LogManager logManager = service.getLogManager();
            // when there is no lag between producer and consumer we are done
            long deadline = System.currentTimeMillis() + unit.toMillis(time);
            while (logManager.getLag(Name.ofUrn(STREAM_NAME), Name.ofUrn(COMPUTATION_NAME)).lag() > 0) {
                if (System.currentTimeMillis() > deadline) {
                    return false;
                }
                Thread.sleep(50);
            }
            return true;
        }
    }

    @Override
    @Deprecated
    public List<LogEntry> getLogEntriesFor(String uuid, Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        // create builder
        QueryBuilder builder = new AuditQueryBuilder();
        // create predicates
        builder.predicate(Predicates.eq(LOG_DOC_UUID, uuid));
        filterMap.values().stream().map(this::convert).forEach(builder::and);
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
        QueryBuilder builder = new AuditQueryBuilder();
        if (ArrayUtils.isNotEmpty(eventIds)) {
            if (eventIds.length == 1) {
                builder.predicate(Predicates.eq(LOG_EVENT_ID, eventIds[0]));
            } else {
                builder.predicate(Predicates.in(LOG_EVENT_ID, eventIds[0]));
            }
        }
        if (ArrayUtils.isNotEmpty(categories)) {
            if (categories.length == 1) {
                builder.predicate(Predicates.eq(LOG_CATEGORY, categories[0]));
            } else {
                builder.predicate(Predicates.in(LOG_CATEGORY, categories[0]));
            }
        }
        if (path != null) {
            builder.predicate(Predicates.eq(LOG_DOC_PATH, path));
        }
        if (limit != null) {
            builder.predicate(Predicates.lt(LOG_EVENT_DATE, limit));
        }
        builder.offset(pageNb * pageSize).limit(pageSize);
        return queryLogs(builder);
    }

    @Override
    public long getLatestLogId(String repositoryId, String... eventIds) {
        QueryBuilder builder = new AuditQueryBuilder().predicate(Predicates.eq(LOG_REPOSITORY_ID, repositoryId))
                                                      .and(Predicates.in(LOG_EVENT_ID, eventIds))
                                                      .order(OrderByExprs.desc(LOG_ID))
                                                      .limit(1);
        return queryLogs(builder).stream().mapToLong(LogEntry::getId).findFirst().orElse(0L);
    }

    @Override
    public List<LogEntry> getLogEntriesAfter(long logIdOffset, int limit, String repositoryId, String... eventIds) {
        QueryBuilder builder = new AuditQueryBuilder().predicate(Predicates.eq(LOG_REPOSITORY_ID, repositoryId))
                                                      .and(Predicates.in(LOG_EVENT_ID, eventIds))
                                                      .and(Predicates.gte(LOG_ID, logIdOffset))
                                                      .order(OrderByExprs.asc(LOG_ID))
                                                      .limit(limit);
        return queryLogs(builder);
    }

    @Override
    public void restore(AuditStorage auditStorage, int batchSize, int keepAlive) {

        QueryBuilder builder = new AuditQueryBuilder();
        ScrollResult<String> scrollResult = auditStorage.scroll(builder, batchSize, keepAlive);
        long t0 = System.currentTimeMillis();
        int total = 0;

        log.info("Starting audit restoration");

        while (scrollResult.hasResults()) {
            List<String> jsonEntries = scrollResult.getResults();
            log.debug("Appending {} entries", jsonEntries::size);
            total += jsonEntries.size();
            append(jsonEntries);

            double dt = (System.currentTimeMillis() - t0) / 1000.0;
            if (dt != 0) {
                log.debug("Restoration speed: {} entries/s", total / dt);
            }

            scrollResult = auditStorage.scroll(scrollResult.getScrollId());
        }

        log.info("Audit restoration done: {} entries migrated from the audit storage", total);

    }

}
