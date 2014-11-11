package org.nuxeo.ecm.platform.audit.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.runtime.api.Framework;

/**
 * PostCommit async listener that pushes {@link EventBundle} into the Audit log
 *
 * @author tiry
 *
 */
public class AuditEventLogger implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(AuditEventLogger.class);

    protected static AuditLogger auditLogger=null;

    protected AuditLogger getAuditLogger() {
        if (auditLogger==null) {
            try {
                auditLogger = Framework.getService(AuditLogger.class);
            } catch (Exception e) {
                log.error("Error while getting AuditLogger", e);
            }
        }
        return auditLogger;
    }

    public void handleEvent(EventBundle events) throws ClientException {

        AuditLogger logger = getAuditLogger();
        if (logger!=null) {
            try {
                logger.logEvents(events);
            }
            catch (AuditException e) {
                log.error("Unable to persist event bundle into audit log", e);
            }
        } else {
            log.error("Can not reach AuditLogger");
        }
    }
}
