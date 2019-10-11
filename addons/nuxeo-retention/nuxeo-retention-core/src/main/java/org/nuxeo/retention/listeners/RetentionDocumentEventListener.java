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
package org.nuxeo.retention.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.service.RetentionManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener processing events with a {@link org.nuxeo.ecm.core.event.impl.DocumentEventContext}). If the targeted
 * document is a record and has an attached event-based, the rule is then evaluated to check if the retention expiration
 * date has to be recomputed.
 *
 * @since 11.1
 */
public class RetentionDocumentEventListener implements PostCommitFilteringEventListener {

    private static final Logger log = LogManager.getLogger(RetentionDocumentEventListener.class);

    @Override
    public boolean acceptEvent(Event event) {
        EventContext eventCtx = event.getContext();
        if (!(eventCtx instanceof DocumentEventContext)) {
            return false;
        }
        RetentionManager retentionManager = Framework.getService(RetentionManager.class);
        if (retentionManager == null) {
            log.trace("RetentionManager not started yet?!");
            return false;
        }
        return retentionManager.getAcceptedEvents().contains(event.getName());
    }

    @Override
    public void handleEvent(EventBundle events) {

        RetentionManager retentionManager = Framework.getService(RetentionManager.class);

        Map<String, Set<String>> docsToCheckAndEvents = new HashMap<String, Set<String>>();

        Map<String, Boolean> documentModifiedIgnored = new HashMap<String, Boolean>();
        for (Event event : events) {
            log.trace("Proceeding event " + event.getName());
            EventContext evtCtx = event.getContext();
            DocumentEventContext docEventCtx = (DocumentEventContext) evtCtx;
            DocumentModel doc = docEventCtx.getSourceDocument();
            String docId = doc.getId();
            if (docEventCtx.getProperties().containsKey(RetentionConstants.RETENTION_CHECKER_LISTENER_IGNORE)
                    && !documentModifiedIgnored.containsKey(docId)) {
                // ignore only once per document per bundle, the rule can be attached and document later modified
                // into
                // the same transaction
                documentModifiedIgnored.put(docId, true);
                continue;
            }
            if (!doc.hasFacet(RetentionConstants.RECORD_FACET)) {
                log.trace("Document is not a record", doc::getPathAsString);
                continue;
            }
            Record record = doc.getAdapter(Record.class);
            RetentionRule rule = record.getRule(docEventCtx.getCoreSession());
            if (rule == null) {
                log.trace("Record {} does not have rule", doc::getPathAsString);
                continue;
            }
            if (!rule.isEventBased()) {
                log.trace("Rule {} is not event-based", () -> rule.getDocument().getPathAsString());
                continue;
            }
            if (record.isRetentionExpired()) {
                log.trace("Record already expired", doc::getPathAsString);
                retentionManager.proceedRetentionExpired(record, event.getContext().getCoreSession());
                // XXX should we check if the record should be under retention again?
                continue;
            }
            if (docsToCheckAndEvents.containsKey(docId)) {
                Set<String> eventsToCheck = docsToCheckAndEvents.get(docId);
                if (!eventsToCheck.contains(event.getName())) {
                    eventsToCheck.add(event.getName());
                }
                docsToCheckAndEvents.put(docId, eventsToCheck);
            } else {
                Set<String> evts = new HashSet<String>();
                evts.add(event.getName());
                docsToCheckAndEvents.put(docId, evts);
            }
        }
        if (!docsToCheckAndEvents.isEmpty()) {
            retentionManager.evalRules(docsToCheckAndEvents);
        }

    }

}
