package org.nuxeo.template.fm;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.platform.audit.api.DocumentHistoryReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.comment.CommentProcessorHelper;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.ContentInputType;
import org.nuxeo.template.InputType;
import org.nuxeo.template.TemplateInput;
import org.nuxeo.template.adapters.doc.TemplateBasedDocument;

public class FMContextBuilder {

    protected static final Log log = LogFactory.getLog(FMContextBuilder.class);

    public static final String[] RESERVED_VAR_NAMES = { "doc", "document",
            "auditEntries", "username" };

    public static List<LogEntry> testAuditEntries;

    public static Map<String, Object> build(DocumentModel doc) throws Exception {
        return build(doc, true);
    }

    public static Map<String, Object> build(DocumentModel doc,
            boolean wrapAuditEntries) throws Exception {

        Map<String, Object> ctx = new HashMap<String, Object>();
        DocumentObjectWrapper nuxeoWrapper = new DocumentObjectWrapper(null);

        ContextFunctions functions = new ContextFunctions(doc, nuxeoWrapper);

        CoreSession session = doc.getCoreSession();

        // doc infos
        ctx.put("doc", nuxeoWrapper.wrap(doc));
        ctx.put("document", nuxeoWrapper.wrap(doc));

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
            if (wrapAuditEntries) {
                ctx.put("auditEntries", nuxeoWrapper.wrap(auditEntries));
            } else {
                ctx.put("auditEntries", auditEntries);
            }
        }
        return ctx;
    }

    protected static List<LogEntry> preprocessAuditEntries(
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

    public static Map<String, Object> build(
            TemplateBasedDocument templateBasedDocument, String templateName)
            throws Exception {

        DocumentModel doc = templateBasedDocument.getAdaptedDoc();
        List<TemplateInput> params = templateBasedDocument.getParams(templateName);

        Map<String, Object> context = build(doc);
        DocumentObjectWrapper nuxeoWrapper = new DocumentObjectWrapper(null);

        for (TemplateInput param : params) {
            if (param.isSourceValue()) {
                if (param.getType() == InputType.Content
                        && ContentInputType.HtmlPreview.getValue().equals(
                                param.getSource())) {
                    HtmlPreviewAdapter preview = doc.getAdapter(HtmlPreviewAdapter.class);
                    String htmlValue = "";
                    if (preview != null) {
                        List<Blob> blobs = preview.getFilePreviewBlobs();
                        if (blobs.size() > 0) {
                            Blob htmlBlob = preview.getFilePreviewBlobs().get(0);
                            if (htmlBlob != null) {
                                htmlValue = htmlBlob.getString();
                            }
                        }
                    }
                    context.put(param.getName(), htmlValue);
                    // metadata.addFieldAsTextStyling(param.getName(),
                    // SyntaxKind.Html);
                    continue;
                }
                Property property = null;
                try {
                    property = doc.getProperty(param.getSource());
                } catch (Throwable e) {
                    log.warn("Unable to ready property " + param.getSource(), e);
                }
                if (property != null) {
                    Serializable value = property.getValue();
                    if (value != null) {
                        if (param.getType() == InputType.Content) {

                        } else {
                            if (Blob.class.isAssignableFrom(value.getClass())) {
                                Blob blob = (Blob) value;
                                context.put(param.getName(), blob);
                            } else {
                                context.put(param.getName(),
                                        nuxeoWrapper.wrap(property));
                            }
                        }
                    } else {
                        // no available value, try to find a default one ...
                        Type pType = property.getType();
                        if (pType.getName().equals(BooleanType.ID)) {
                            context.put(param.getName(), new Boolean(false));
                        } else if (pType.getName().equals(DateType.ID)) {
                            context.put(param.getName(), new Date());
                        } else if (pType.getName().equals(StringType.ID)) {
                            context.put(param.getName(), "");
                        } else if (pType.getName().equals(StringType.ID)) {
                            context.put(param.getName(), "");
                        } else {
                            context.put(param.getName(), new Object());
                        }
                    }
                }
            } else {
                if (InputType.StringValue.equals(param.getType())) {
                    context.put(param.getName(), param.getStringValue());
                } else if (InputType.BooleanValue.equals(param.getType())) {
                    context.put(param.getName(), param.getBooleanValue());
                } else if (InputType.DateValue.equals(param.getType())) {
                    context.put(param.getName(), param.getDateValue());
                }
            }
        }

        return context;
    }

}
