package org.nuxeo.template.context;

import java.util.HashMap;
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
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.fm.FMContextBuilder;

public abstract class AbstractContextBuilder {

    public static List<LogEntry> testAuditEntries;

    protected static final Log log = LogFactory.getLog(AbstractContextBuilder.class);

    public static final String[] RESERVED_VAR_NAMES = { "doc", "document",
            "auditEntries", "username" };

    public Map<String, Object> build(DocumentModel doc,
            DocumentWrapper nuxeoWrapper) throws Exception {

        Map<String, Object> ctx = new HashMap<String, Object>();

        ContextFunctions functions = new ContextFunctions(doc, nuxeoWrapper);

        CoreSession session = doc.getCoreSession();

        // doc infos
        ctx.put("doc", nuxeoWrapper.wrap(doc));
        ctx.put("document", nuxeoWrapper.wrap(doc));

        // blob wrapper
        ctx.put("blobHolder", new BlobHolderWrapper(doc));

        // add functions helper
        ctx.put("fn", functions);
        ctx.put("Fn", functions);
        ctx.put("fonctions", functions);

        // user info
        ctx.put("username", session.getPrincipal().getName());
        ctx.put("principal", session.getPrincipal());

        // add audit context info
        DocumentHistoryReader historyReader = Framework.getLocalService(DocumentHistoryReader.class);
        List<LogEntry> auditEntries = null;
        if (historyReader != null) {
            auditEntries = historyReader.getDocumentHistory(doc, 0, 1000);
        } else {
            if (Framework.isTestModeSet() && testAuditEntries != null) {
                auditEntries = testAuditEntries;
            } else {
                log.warn("Can not add Audit info to rendering context");
            }
        }
        if (auditEntries != null) {
            try {
                auditEntries = preprocessAuditEntries(auditEntries, session,
                        "en");
            } catch (Throwable e) {
                log.warn("Unable to preprocess Audit entries : "
                        + e.getMessage());
            }
            ctx.put("auditEntries", nuxeoWrapper.wrap(auditEntries));
        }
        return ctx;
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

    public Map<String, Object> build(
            TemplateBasedDocument templateBasedDocument, String templateName)
            throws Exception {

        DocumentModel doc = templateBasedDocument.getAdaptedDoc();

        Map<String, Object> context = build(doc);

        return context;
    }

    public abstract Map<String, Object> build(DocumentModel doc)
            throws Exception;
}
