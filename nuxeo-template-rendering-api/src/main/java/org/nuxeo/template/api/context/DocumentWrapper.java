package org.nuxeo.template.api.context;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 * Wrapper interface used to wrap the Object that will be put inside the
 * rendering context.
 * <p>
 * Because the rederning context wrapping requirements can depends on the actual
 * rendering engine implementation, this is just an interface so that several
 * implemenations can be provided
 * </p>
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public interface DocumentWrapper {

    Object wrap(DocumentModel doc) throws Exception;

    Object wrap(List<LogEntry> auditEntries) throws Exception;
}
