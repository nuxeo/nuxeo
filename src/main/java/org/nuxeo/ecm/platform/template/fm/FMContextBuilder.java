package org.nuxeo.ecm.platform.template.fm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.runtime.api.Framework;

public class FMContextBuilder {

    protected static final Log log = LogFactory.getLog(FMContextBuilder.class);

    public static final String[] RESERVED_VAR_NAMES = {"doc", "document", "auditEntries", "username"};

    public static List<LogEntry> testAuditEntries;

    public static Map<String, Object> build(DocumentModel doc) throws Exception {

        Map<String, Object> ctx = new HashMap<String, Object>();
        DocumentObjectWrapper nuxeoWrapper = new DocumentObjectWrapper(null);

        // doc infos
        ctx.put("doc", nuxeoWrapper.wrap(doc));
        ctx.put("document", nuxeoWrapper.wrap(doc));

        // user info
        ctx.put("username", doc.getCoreSession().getPrincipal().getName());
        ctx.put("principal", doc.getCoreSession().getPrincipal());

        // add audit context info
        AuditReader auditReader = Framework.getLocalService(AuditReader.class);
        List<LogEntry> auditEntries = null;
        if (auditReader!=null) {
            auditEntries = auditReader.getLogEntriesFor(doc.getId());
        } else {
            if (Framework.isTestModeSet() && testAuditEntries!=null ) {
                auditEntries = testAuditEntries;
            } else {
                log.warn("Can not add Audit info to rendering context");
            }
        }
        if (auditEntries!=null) {
            ctx.put("auditEntries", nuxeoWrapper.wrap(auditEntries));
        }
        return ctx;
    }

}
