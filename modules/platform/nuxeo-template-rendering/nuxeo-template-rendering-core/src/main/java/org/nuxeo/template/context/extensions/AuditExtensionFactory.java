/*
 * (C) Copyright 2012-2026 Nuxeo (http://nuxeo.com/) and others.
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.context.ContextExtensionFactory;
import org.nuxeo.template.api.context.DocumentWrapper;

public class AuditExtensionFactory implements ContextExtensionFactory {

    private static final Logger log = LogManager.getLogger(AuditExtensionFactory.class);

    public static List<LogEntry> testAuditEntries;

    @Override
    @SuppressWarnings("unchecked")
    public Object getExtension(DocumentModel currentDocument, DocumentWrapper wrapper, Map<String, Object> ctx) {
        // add audit context info
        List<LogEntry> auditEntries;
        var pps = Framework.getService(PageProviderService.class);
        if (pps != null) {
            var pp = pps.getPageProvider("DOCUMENT_HISTORY_PROVIDER", null, 0L, 1_000L, Map.of(), currentDocument);
            auditEntries = (List<LogEntry>) pp.getCurrentPage();
        } else {
            if (Framework.isTestModeSet() && testAuditEntries != null) {
                auditEntries = testAuditEntries;
            } else {
                auditEntries = new ArrayList<>();
                log.warn("Can not add Audit info to rendering context");
            }
        }
        try {
            auditEntries = preprocessAuditEntries(auditEntries, currentDocument.getCoreSession(), "en");
        } catch (MissingResourceException e) {
            log.warn("Unable to preprocess Audit entries : {}", e::getMessage);
        }
        ctx.put("auditEntries", wrapper.wrap(auditEntries));
        return null;
    }

    protected List<LogEntry> preprocessAuditEntries(List<LogEntry> auditEntries, CoreSession session, String lang)
            throws MissingResourceException {
        return auditEntries.stream().map(entry -> {
            String comment = getLogComment(entry, session);
            if (comment == null) {
                comment = "";
            } else {
                String i18nComment = I18NUtils.getMessageString("messages", comment, null, Locale.of(lang));
                if (i18nComment != null) {
                    comment = i18nComment;
                }
            }
            String eventId = I18NUtils.getMessageString("messages", entry.getEventId(), null, Locale.of(lang));
            if (eventId != null) {
                eventId = entry.getEventId();
            }
            return LogEntry.builder(eventId, entry.getEventDate())
                           .id(entry.getId())
                           .principalName(entry.getPrincipalName())
                           .logDate(entry.getLogDate())
                           .docUUID(entry.getDocUUID())
                           .docPath(entry.getDocPath())
                           .docType(entry.getDocType())
                           .category(entry.getCategory())
                           .comment(comment)
                           .docLifeCycle(entry.getDocLifeCycle())
                           .repositoryId(entry.getRepositoryId())
                           .extended(entry.getExtended())
                           .build();
        }).toList();
    }

    protected String getLogComment(LogEntry entry, CoreSession session) {
        String oldComment = entry.getComment();
        if (oldComment == null) {
            return null;
        }

        String newComment = oldComment;
        boolean targetDocExists = false;
        String[] split = oldComment.split(":");
        if (split.length >= 2) {
            String strDocRef = split[1];
            DocumentRef docRef = new IdRef(strDocRef);
            targetDocExists = session.exists(docRef);
        }

        if (targetDocExists) {
            String eventId = entry.getEventId();
            // update comment
            if (DocumentEventTypes.DOCUMENT_DUPLICATED.equals(eventId)) {
                newComment = "audit.duplicated_to";
            } else if (DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(eventId)) {
                newComment = "audit.copied_from";
            } else if (DocumentEventTypes.DOCUMENT_MOVED.equals(eventId)) {
                newComment = "audit.moved_from";
            }
        }

        return newComment;
    }
}
