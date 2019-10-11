/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */
package org.nuxeo.retention.service;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.document.DeleteDocument;
import org.nuxeo.ecm.automation.core.operations.document.LockDocument;
import org.nuxeo.ecm.automation.core.operations.document.TrashDocument;
import org.nuxeo.ecm.automation.core.operations.document.UnlockDocument;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.workers.RuleEvaluationWorker;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 11.1
 */
public class RetentionManagerImpl extends DefaultComponent implements RetentionManager {

    private static final Logger log = LogManager.getLogger(RetentionManagerImpl.class);

    @Override
    public DocumentModel attachRule(DocumentModel document, RetentionRule rule, CoreSession session) {
        checkCanAttachRule(document, rule, session);
        session.makeRecord(document.getRef());
        final Calendar retainUntil;
        if (rule.isImmediate()) {
            retainUntil = rule.getRetainUntilDateFromNow();
            log.debug("Attaching immediate rule until {}",
                    () -> RetentionConstants.DEFAULT_DATE_FORMAT.format(retainUntil.getTime()));
        } else if (rule.isAfterDely()) {
            log.debug("Attaching after delay rule");
            throw new UnsupportedOperationException("After delay not yet implemented");
        } else if (rule.isEventBased()) {
            retainUntil = CoreSession.RETAIN_UNTIL_INDETERMINATE;
            log.debug("Attaching event-based rule on {} matching \"{}\"", rule.getStartingPointEvent(),
                    rule.getStartingPointExpression());
        } else if (rule.isMetadataBased()) {
            String xpath = rule.getMetadataXpath();
            if (StringUtils.isBlank(xpath)) {
                throw new NuxeoException("Metadata field is null");
            }
            Property prop = document.getProperty(xpath);
            if (!(prop.getType() instanceof DateType)) {
                throw new NuxeoException(
                        String.format("Field %s of type % is expected to have a DateType", xpath, prop.getType()));
            }
            Calendar value = (Calendar) prop.getValue();
            if (value != null) {
                Calendar retainUntilCandidate = rule.getRetainUntilDateFrom(value);
                Calendar now = Calendar.getInstance();
                if (now.after(retainUntilCandidate)) {
                    log.info(
                            "Metabased-based rule found past date {} as retention expiration date on {} from {} property. Ignoring...",
                            () -> RetentionConstants.DEFAULT_DATE_FORMAT.format(retainUntilCandidate.getTime()),
                            document::getPathAsString, () -> xpath);
                    retainUntil = null;
                } else {
                    retainUntil = retainUntilCandidate;
                    log.debug("Attaching rule based on {} with value {}", () -> xpath,
                            () -> RetentionConstants.DEFAULT_DATE_FORMAT.format(retainUntil.getTime()));
                }
            } else {
                retainUntil = null;
                log.info("Attaching rule based on {}: empty value", xpath);
            }
        } else {
            throw new IllegalArgumentException("Unknown starting point policy: " + rule.getStartingPointPolicy());
        }
        document.addFacet(RetentionConstants.RECORD_FACET);
        Record record = document.getAdapter(Record.class);
        record.setRule(rule, session);
        executeRuleBeginActions(record, session);
        if (retainUntil != null) {
            session.setRetainUntil(document.getRef(), retainUntil, null);
        }
        notifyAttachRule(record, rule, session);
        return session.getDocument(document.getRef());
    }

    protected void notifyAttachRule(Record record, RetentionRule rule, CoreSession session) {
        DocumentModel doc = record.getDocument();
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, session.getRepositoryName());
        ctx.setProperty(CoreEventConstants.SESSION_ID, session.getSessionId());
        ctx.setProperty(DocumentEventContext.CATEGORY_PROPERTY_KEY, DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
        ctx.setProperty(DocumentEventContext.COMMENT_PROPERTY_KEY, rule.getDocument().getPathAsString());
        Event event = ctx.newEvent(RetentionConstants.RULE_ATTACHED_EVENT);
        Framework.getService(EventService.class).fireEvent(event);
    }

    protected void checkCanAttachRule(DocumentModel document, RetentionRule rule, CoreSession session) {
        NuxeoPrincipal principal = session.getPrincipal();
        if (!principal.isAdministrator() && !principal.isMemberOf(RetentionConstants.RECORD_MANAGER_GROUP_NAME)) {
            if (!session.hasPermission(document.getRef(), SecurityConstants.MAKE_RECORD)
                    || !session.hasPermission(document.getRef(), SecurityConstants.SET_RETENTION))
                throw new NuxeoException("User is not authorized to attach retention rule", SC_FORBIDDEN);
        }
        if (!rule.isEnabled()) {
            throw new NuxeoException(String.format("Rule is disabled"));
        }
        if (!rule.isDocTypeAccepted(document.getType())) {
            throw new NuxeoException("Rule does not accept this document type");
        }
        if (rule.isMetadataBased()) {
            document.getProperty(rule.getMetadataXpath());
            // above throw an exception if property not found
        }
        if (document.hasFacet(RetentionConstants.RECORD_FACET)) {
            throw new NuxeoException("Document is already a record");
        }
    }

    @Override
    public boolean canAttachRule(DocumentModel document, RetentionRule rule, CoreSession session) {
        try {
            checkCanAttachRule(document, rule, session);
            return true;
        } catch (NuxeoException e) {
            log.info("Cannot attach rule {} on document {}", () -> rule.getDocument().getPathAsString(),
                    document::getPathAsString);
            return false;
        }
    }

    public void executeRuleBeginActions(Record record, CoreSession session) {
        RetentionRule rule = record.getRule(session);
        if (rule != null) {
            executeRuleActions(record.getDocument(), rule.getBeginActions(), session);
        }
    }

    public void executeRuleEndActions(Record record, CoreSession session) {
        RetentionRule rule = record.getRule(session);
        if (rule != null) {
            executeRuleActions(record.getDocument(), rule.getEndActions(), session);
        }
    }

    protected void executeRuleActions(DocumentModel doc, List<String> actionIds, CoreSession session) {
        if (actionIds != null) {
            AutomationService automationService = Framework.getService(AutomationService.class);
            for (String operationId : actionIds) {
                log.debug("Executing {} action on {}", () -> operationId, doc::getPathAsString);
                // Do not lock document if already locked, and unlock if already unlocked (triggers an error)
                // Also, if it's time to delete, unlock it first, etc.
                // (more generally, be ready to handle specific operations and context)
                switch (operationId) {
                case LockDocument.ID:
                    if (doc.isLocked()) {
                        continue;
                    }
                    break;

                case UnlockDocument.ID:
                    if (!doc.isLocked()) {
                        continue;
                    }
                    break;

                case DeleteDocument.ID:
                case TrashDocument.ID:
                    if (doc.isLocked()) {
                        session.removeLock(doc.getRef());
                        doc = session.getDocument(doc.getRef());
                    }
                    break;
                }
                OperationContext context = getExecutionContext(doc, session);
                try {
                    automationService.run(context, operationId);
                } catch (OperationException e) {
                    throw new NuxeoException("Error running operation: " + operationId, e);
                }
            }
        }
    }

    protected OperationContext getExecutionContext(DocumentModel doc, CoreSession session) {
        OperationContext context = new OperationContext(session);
        context.put("document", doc);
        context.setCommit(false); // no session save at end
        context.setInput(doc);
        return context;
    }

    @Override
    public void evalRules(Map<String, Set<String>> docsToCheckAndEvents) {
        if (docsToCheckAndEvents.isEmpty()) {
            return;
        }
        RuleEvaluationWorker work = new RuleEvaluationWorker(docsToCheckAndEvents);
        Framework.getService(WorkManager.class).schedule(work, WorkManager.Scheduling.ENQUEUE);
    }

    protected ELActionContext initActionContext(DocumentModel doc, CoreSession session) {
        ELActionContext ctx = new ELActionContext(new ExpressionContext(), new ExpressionFactoryImpl());
        ctx.setCurrentPrincipal(session.getPrincipal());
        doc.detach(true);
        ctx.setCurrentDocument(doc);
        return ctx;
    }

    protected Boolean evaluateConditionExpression(ELActionContext ctx, String expression) {
        Calendar now = Calendar.getInstance();
        if (StringUtils.isEmpty(expression)) {
            return true;
        }
        ctx.putLocalVariable("currentDate", now);
        return ctx.checkCondition(expression);
    }

    @Override
    public void evalExpressionEventBasedRules(Record record, Set<String> events, CoreSession session) {
        if (record == null) {
            return; // nothing to do
        }
        RetentionRule rule = record.getRule(session);
        if (rule == null) {
            return; // nothing to do
        }
        if (!rule.isEventBased()) {
            log.trace("Record is not event-based");
            return;
        }
        log.debug("Evaluating event-based rules for record {}", () -> record.getDocument().getPathAsString());
        if (record.isRetentionExpired()) {
            // retention expired, nothing to do
            log.debug("Evaluating event-based found retention expired");
            proceedRetentionExpired(record, session);
            return;

        }
        String startingPointEvent = rule.getStartingPointEvent();
        if (StringUtils.isBlank(startingPointEvent)) {
            log.warn("Evaluating event-based rules  on record {} found no event specified",
                    () -> record.getDocument().getPathAsString());
            return;
        }
        if (events.contains(startingPointEvent)) {
            ELActionContext actionContext = initActionContext(record.getDocument(), session);
            String expression = rule.getStartingPointExpression();
            Boolean startNow = evaluateConditionExpression(actionContext, expression);
            if (startNow) {
                session.setRetainUntil(record.getDocument().getRef(), rule.getRetainUntilDateFromNow(), null);
                log.debug("Evaluating event-based rules: expression {} matched on event {}", startingPointEvent);
            } else {
                log.debug("Evaluating event-based rules: expression {} did not match on event {}", expression,
                        startingPointEvent);
            }
        }
    }

    @Override
    public void proceedRetentionExpired(Record record, CoreSession session) {
        executeRuleEndActions(record, session);
    }

    protected List<String> acceptedEvents;

    @Override
    public List<String> getAcceptedEvents() {
        if (acceptedEvents == null) {
            synchronized (this) {
                if (acceptedEvents == null) {
                    DirectoryService directoryService = Framework.getService(DirectoryService.class);
                    Directory dir = directoryService.getDirectory(RetentionConstants.EVENTS_DIRECTORY_NAME);
                    try (Session session = dir.getSession()) {
                        Map<String, Serializable> filter = new HashMap<>();
                        filter.put(RetentionConstants.OBSOLETE_FIELD_ID, Long.valueOf(0));
                        List<String> evts = session.getProjection(filter, session.getIdField());
                        if (evts.isEmpty()) {
                            log.trace("Empty accepted events, not chaching anything");
                            return evts;
                        }
                        acceptedEvents = evts;
                        log.debug("Accepted events {}", acceptedEvents::toString);
                    }
                }

            }
        }
        return acceptedEvents;
    }

    @Override
    public void invalidate() {
        acceptedEvents = null;
    }

    @Override
    public void start(ComponentContext context) {
        Framework.doPrivileged(() -> {
            UserManager userManager = Framework.getService(UserManager.class);
            if (userManager.getGroup(RetentionConstants.RECORD_MANAGER_GROUP_NAME) == null) {
                DocumentModel groupModel = userManager.getBareGroupModel();
                String groupSchemaName = userManager.getGroupSchemaName();
                groupModel = userManager.getBareGroupModel();
                groupModel.setPropertyValue(userManager.getGroupIdField(),
                        RetentionConstants.RECORD_MANAGER_GROUP_NAME);
                groupModel.setProperty(groupSchemaName, "groupname", RetentionConstants.RECORD_MANAGER_GROUP_NAME);
                groupModel.setProperty(groupSchemaName, "grouplabel", RetentionConstants.RECORD_MANAGER_GROUP_NAME);
                userManager.createGroup(groupModel);
                log.debug("Created new {} group", RetentionConstants.RECORD_MANAGER_GROUP_NAME);
            }
        });
    }

}
