package org.nuxeo.ecm.platform.template.fm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.runtime.api.Framework;

public class FMContextBuilder {

    public static final String[] RESERVED_VAR_NAMES = {"doc", "document", "auditEntries", "username"};

    public static Map<String, Object> build(DocumentModel doc) throws Exception {

        Map<String, Object> ctx = new HashMap<String, Object>();
        DocumentObjectWrapper nuxeoWrapper = new DocumentObjectWrapper(null);

        ctx.put("doc", nuxeoWrapper.wrap(doc));
        ctx.put("document", nuxeoWrapper.wrap(doc));
        ctx.put("username", doc.getCoreSession().getPrincipal().getName());
        ctx.put("principal", doc.getCoreSession().getPrincipal());

        AuditReader auditReader = Framework.getLocalService(AuditReader.class);
        if (auditReader!=null) {
            List<LogEntry> auditEntries = auditReader.getLogEntriesFor(doc.getId());
            ctx.put("auditEntries", nuxeoWrapper.wrap(auditEntries));
        }

        return ctx;
    }

}
