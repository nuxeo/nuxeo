package org.nuxeo.template.context;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

public interface DocumentWrapper {

    Object wrap(DocumentModel doc) throws Exception;

    Object wrap(List<LogEntry> auditEntries) throws Exception;
}
