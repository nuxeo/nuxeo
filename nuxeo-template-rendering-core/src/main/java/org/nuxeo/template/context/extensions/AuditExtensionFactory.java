/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.template.context.extensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.DocumentHistoryReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.comment.CommentProcessorHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.context.ContextExtensionFactory;
import org.nuxeo.template.api.context.DocumentWrapper;

public class AuditExtensionFactory implements ContextExtensionFactory {

    public static List<LogEntry> testAuditEntries;

    protected static final Log log = LogFactory.getLog(AuditExtensionFactory.class);

    @Override
    public Object getExtension(DocumentModel currentDocument, DocumentWrapper wrapper, Map<String, Object> ctx) {
        // add audit context info
        DocumentHistoryReader historyReader = Framework.getService(DocumentHistoryReader.class);
        List<LogEntry> auditEntries = null;
        if (historyReader != null) {
            auditEntries = historyReader.getDocumentHistory(currentDocument, 0, 1000);
        } else {
            if (Framework.isTestModeSet() && testAuditEntries != null) {
                auditEntries = testAuditEntries;
            } else {
                auditEntries = new ArrayList<LogEntry>();
                log.warn("Can not add Audit info to rendering context");
            }
        }
        if (auditEntries != null) {
            try {
                auditEntries = preprocessAuditEntries(auditEntries, currentDocument.getCoreSession(), "en");
            } catch (MissingResourceException e) {
                log.warn("Unable to preprocess Audit entries : " + e.getMessage());
            }
            ctx.put("auditEntries", wrapper.wrap(auditEntries));
        }
        return null;
    }

    protected List<LogEntry> preprocessAuditEntries(List<LogEntry> auditEntries, CoreSession session, String lang)
            throws MissingResourceException {
        CommentProcessorHelper helper = new CommentProcessorHelper(session);
        for (LogEntry entry : auditEntries) {
            String comment = helper.getLogComment(entry);
            if (comment == null) {
                comment = "";
            } else {
                String i18nComment = I18NUtils.getMessageString("messages", comment, null, new Locale(lang));
                if (i18nComment != null) {
                    comment = i18nComment;
                }
            }
            String eventId = entry.getEventId();
            String i18nEventId = I18NUtils.getMessageString("messages", eventId, null, new Locale(lang));
            if (i18nEventId != null) {
                entry.setEventId(i18nEventId);
            }
            entry.setComment(comment);
        }
        return auditEntries;
    }
}
