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

import static org.nuxeo.ecm.platform.audit.impl.LogEntrySequenceGenerator.USE_NUXEO_SEQUENCER_PROPERTY;

import java.io.Serializable;
import java.security.Principal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jakarta.el.ELException;

import org.apache.el.ExpressionFactoryImpl;
import org.apache.logging.log4j.Logger;
import org.nuxeo.audit.api.AuditQueryBuilder;
import org.nuxeo.audit.service.AuditService;
import org.nuxeo.audit.service.extension.AdapterDescriptor;
import org.nuxeo.audit.service.extension.ExtendedInfoDescriptor;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
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
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.platform.audit.api.AuditStorage;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract class to share code between {@link AuditBackend} implementations
 *
 * @author tiry
 * @param <L> to give the log entry type for the new {@link org.nuxeo.audit.service.AuditBackend} interface that defines
 *            a new entry type.
 * @deprecated since 2025.0, use {@link org.nuxeo.audit.service.AbstractAuditBackend} instead
 */
@SuppressWarnings("removal")
@Deprecated(since = "2025.0", forRemoval = true)
public abstract class AbstractAuditBackend<L extends LogEntry> implements AuditBackend<L>, AuditStorage {

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
        } else {
            entry.setEventDate(new Date());
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
        return Framework.getService(AuditService.class).getAuditableEventNames();
    }

    @Override
    @SuppressWarnings("unchecked")
    public L buildEntryFromEvent(Event event) {
        return (L) Framework.getService(AuditService.class).buildEntryFromEvent(event);
    }

    @Override
    @SuppressWarnings("unchecked")
    public L newLogEntry() {
        return (L) new LogEntryImpl();
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

        var logEntries = new ArrayList<LogEntry>();
        logEntries.add(doCreateAndFillEntryFromDocument(node, session.getPrincipal()));

        for (DocumentModel child : guardedDocumentChildren(session, node.getRef())) {
            if (child.isFolder() && recurs) {
                folderishChildren.add(child);
            } else {
                logEntries.add(doCreateAndFillEntryFromDocument(child, principal));
                nbSyncedEntries += 1;
            }
        }
        Consumer<LogEntry> persist = provider::addLogEntry;
        if (Framework.isBooleanPropertyTrue(USE_NUXEO_SEQUENCER_PROPERTY)) {
            var sequencer = Framework.getService(UIDGeneratorService.class).getSequencer();
            List<Long> block = sequencer.getNextBlock("audit", logEntries.size());
            Consumer<LogEntry> setId = entry -> ((LogEntryImpl) entry).setOriginalId(block.removeFirst());
            persist = setId.andThen(persist);
        }
        logEntries.forEach(persist);

        if (recurs) {
            for (DocumentModel folderChild : folderishChildren) {
                nbSyncedEntries += doSyncNode(provider, session, folderChild, recurs);
            }
        }

        return nbSyncedEntries;
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
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        return Framework.getService(AuditService.class).await(Duration.ofMillis(unit.toMillis(time)));
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
