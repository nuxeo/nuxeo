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
package org.nuxeo.retention.adapters;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.retention.RetentionConstants;

/**
 * @since 11.1
 */
public class Record {

    private static final Logger log = LogManager.getLogger(Record.class);

    protected DocumentModel document;

    public Record(final DocumentModel doc) {
        document = doc;
    }

    public DocumentModel getDocument() {
        return document;
    }

    public RetentionRule getRule(CoreSession session) {
        List<String> ruleIds = getRuleIds();
        if (ruleIds == null) {
            return null;
        }
        for (String ruleId : ruleIds) {
            IdRef ruleRef = new IdRef(ruleId);
            if (!session.exists(ruleRef)) {
                log.trace("Rule {} does not exist", ruleRef);
                continue;
            }
            DocumentModel ruleDoc = session.getDocument(ruleRef);
            RetentionRule rule = ruleDoc.getAdapter(RetentionRule.class);
            if (!rule.isEnabled()) {
                log.debug("Rule {} id disabled", ruleDoc::getPathAsString);
                continue;
            }
            return rule;
        }
        log.debug("No active rules found for {}", document::getPathAsString);
        return null;

    }

    protected List<String> getRuleIds() {
        Serializable propertyValue = document.getPropertyValue(RetentionConstants.RECORD_RULE_IDS_PROP);
        if (propertyValue == null) {
            return Collections.emptyList();
        }
        return Arrays.asList((String[]) propertyValue);
    }

    public Calendar getSavedRetainUntil() {
        Serializable savedRetainUntil = document.getPropertyValue(RetentionConstants.RETAIN_UNTIL_PROP);
        if (savedRetainUntil != null) {
            return (Calendar) savedRetainUntil;
        }
        return null;
    }

    public boolean isRetainUntilInderterminate() {
        if (!getDocument().isUnderRetentionOrLegalHold()) {
            return false;
        }
        Calendar retainUntil = getDocument().getRetainUntil();
        return retainUntil != null
                ? CoreSession.RETAIN_UNTIL_INDETERMINATE.getTimeInMillis() == retainUntil.getTimeInMillis()
                : false;
    }

    public boolean isRetentionExpired() {
        if (!getDocument().isUnderRetentionOrLegalHold()) {
            return true;
        }
        Calendar retainUntil;
        return (retainUntil = getDocument().getRetainUntil()) == null || !Calendar.getInstance().before(retainUntil);
    }

    protected void save(CoreSession session) {
        document.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, true);
        document.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, true);
        document.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, true);
        document.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, true);
        document.putContextData(RetentionConstants.RETENTION_CHECKER_LISTENER_IGNORE, true);
        session.saveDocument(document);
        document.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER, null);
        document.putContextData(NotificationConstants.DISABLE_NOTIFICATION_SERVICE, null);
        document.putContextData(NXAuditEventsService.DISABLE_AUDIT_LOGGER, null);
        document.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, null);
        document.getContextData().remove(RetentionConstants.RETENTION_CHECKER_LISTENER_IGNORE);
    }

    public void saveRetainUntil(Calendar retainUntil) {
        document.setPropertyValue(RetentionConstants.RETAIN_UNTIL_PROP, retainUntil);
    }

    public void setRule(RetentionRule rule, CoreSession session) {
        setRuleIds(Arrays.asList(rule.getDocument().getId()));
        save(session);
    }

    protected void setRuleIds(final List<String> ruleIds) {
        document.setPropertyValue(RetentionConstants.RECORD_RULE_IDS_PROP, (Serializable) ruleIds);
    }
}