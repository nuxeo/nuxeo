package org.nuxeo.template.context.extensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    public Object getExtension(DocumentModel currentDocument,
            DocumentWrapper wrapper, Map<String, Object> ctx) {

        try {
            // add audit context info
            DocumentHistoryReader historyReader = Framework.getLocalService(DocumentHistoryReader.class);
            List<LogEntry> auditEntries = null;
            if (historyReader != null) {
                auditEntries = historyReader.getDocumentHistory(
                        currentDocument, 0, 1000);
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
                    auditEntries = preprocessAuditEntries(auditEntries,
                            currentDocument.getCoreSession(), "en");
                } catch (Throwable e) {
                    log.warn("Unable to preprocess Audit entries : "
                            + e.getMessage());
                }
                ctx.put("auditEntries", wrapper.wrap(auditEntries));
            }
        } catch (Exception e) {
            log.error("Error during Audit context extension", e);
            try {
                ctx.put("auditEntries", wrapper.wrap(new ArrayList<LogEntry>()));
            } catch (Exception e1) {
                log.error("Unable to fill context with mock AuditEntries", e1);
            }
        }
        return null;
    }

    protected List<LogEntry> preprocessAuditEntries(
            List<LogEntry> auditEntries, CoreSession session, String lang) {
        CommentProcessorHelper helper = new CommentProcessorHelper(session);
        for (LogEntry entry : auditEntries) {
            String comment = helper.getLogComment(entry);
            if (comment == null) {
                comment = "";
            } else {
                String i18nComment = I18NUtils.getMessageString("messages",
                        comment, null, new Locale(lang));
                if (i18nComment != null) {
                    comment = i18nComment;
                }
            }
            String eventId = entry.getEventId();
            String i18nEventId = I18NUtils.getMessageString("messages",
                    eventId, null, new Locale(lang));
            if (i18nEventId != null) {
                entry.setEventId(i18nEventId);
            }
            entry.setComment(comment);
        }
        return auditEntries;
    }
}
