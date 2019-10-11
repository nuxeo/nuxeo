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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.RetentionRule;

/**
 * Retention service.
 *
 * @since 11.1
 */
public interface RetentionManager {

    /**
     * Attaches a retention rule on a document. Only
     * #{@link org.nuxeo.retention.adapters.RetentionRule.ApplicationPolicy#MANUAL} rules can be attached through this
     * method. Depending on the #{@link org.nuxeo.retention.adapters.RetentionRule.StartingPointPolicy}. The document is
     * turned into a record and a retention date is computed according to the rule's
     * #{@link org.nuxeo.retention.adapters.RetentionRule.StartingPointPolicy} and settings.
     *
     * @param document the document
     * @param rule the retention rule
     * @param session the core session
     * @return the record document with a retention expiration date
     * @since 11.1
     */
    DocumentModel attachRule(DocumentModel document, RetentionRule rule, CoreSession session);

    /**
     * Checks that the session has sufficient permission to attach the rule to the document.
     *
     * @param document the document
     * @param rule the rule
     * @param session the session
     * @return
     * @since 11.1
     */
    boolean canAttachRule(DocumentModel document, RetentionRule rule, CoreSession session);

    /**
     * Triggers the evaluation of event-based retention rules that may be attached to the document ids.
     *
     * @param docsToCheckAndEvents map of document ids and set of events
     * @since 11.1
     */
    void evalRules(Map<String, Set<String>> docsToCheckAndEvents);

    /**
     * Evaluates the event-based retention rules that may be attached to the given record document.
     *
     * @param record the record document
     * @param events the set of events
     * @param session the session
     * @since 11.1
     */
    void evalExpressionEventBasedRules(Record record, Set<String> events, CoreSession session);

    /**
     * Returns the list of accepted platform core events for event-based retention rules.
     *
     * @return the list of accepted events.
     * @since 11.1
     */
    List<String> getAcceptedEvents();

    /**
     * Invalidate service (useful in a test context).
     *
     * @since 11.1
     */
    void invalidate();

    /**
     * Proceeds the post-action that a retention rule attached to the document may have.
     *
     * @param record the record document
     * @param coreSession the core session
     * @since 11.1
     */
    void proceedRetentionExpired(Record record, CoreSession coreSession);

}
