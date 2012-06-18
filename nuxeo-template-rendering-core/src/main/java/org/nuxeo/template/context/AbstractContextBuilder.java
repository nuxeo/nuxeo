package org.nuxeo.template.context;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.context.DocumentWrapper;

public abstract class AbstractContextBuilder {

    protected static final Log log = LogFactory.getLog(AbstractContextBuilder.class);

    public static final String[] RESERVED_VAR_NAMES = { "doc", "document",
            "auditEntries", "username" };

    public Map<String, Object> build(DocumentModel doc,
            DocumentWrapper nuxeoWrapper, String templateName) throws Exception {

        Map<String, Object> ctx = new HashMap<String, Object>();

        CoreSession session = doc.getCoreSession();

        // doc infos
        ctx.put("doc", nuxeoWrapper.wrap(doc));
        ctx.put("document", nuxeoWrapper.wrap(doc));

        // blob wrapper
        ctx.put("blobHolder", new BlobHolderWrapper(doc));

        // user info
        ctx.put("username", session.getPrincipal().getName());
        ctx.put("principal", session.getPrincipal());

        ctx.put("templateName", templateName);

        // fetch extensions
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        tps.addContextExtensions(doc, nuxeoWrapper, ctx);

        return ctx;
    }

    public Map<String, Object> build(
            TemplateBasedDocument templateBasedDocument, String templateName)
            throws Exception {

        DocumentModel doc = templateBasedDocument.getAdaptedDoc();

        Map<String, Object> context = build(doc, templateName);

        return context;
    }

    protected abstract DocumentWrapper getWrapper();

    public Map<String, Object> build(DocumentModel doc, String templateName)
            throws Exception {

        return build(doc, getWrapper(), templateName);
    }
}
