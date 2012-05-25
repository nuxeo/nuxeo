package org.nuxeo.template.context;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.template.api.context.DocumentWrapper;

public class SimpleBeanWrapper implements DocumentWrapper {

    @Override
    public Object wrap(DocumentModel doc) throws Exception {
        return new SimpleDocumentWrapper(doc);
    }

    @Override
    public Object wrap(List<LogEntry> auditEntries) throws Exception {
        return auditEntries;
    }

}
