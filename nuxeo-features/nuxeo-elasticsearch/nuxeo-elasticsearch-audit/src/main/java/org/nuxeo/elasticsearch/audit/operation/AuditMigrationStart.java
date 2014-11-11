package org.nuxeo.elasticsearch.audit.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.runtime.api.Framework;

@Operation(id = AuditMigrationStart.ID, category = Constants.CAT_SERVICES, label = "Starts audit migration process from JPA to ElasticSearch")
public class AuditMigrationStart {

    public static final String ID = "Audit.StartMigration";

    @Context
    protected OperationContext ctx;

    protected static final Log log = LogFactory.getLog(AuditMigrationStart.class);

    @Param(name = "batchSize", required = false)
    protected int batchSize = 1000;

    @OperationMethod
    public String startMigration() throws Exception {

        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
        AuditBackend backend = audit.getBackend();

        if (!(backend instanceof ESAuditBackend)) {
            log.error("Unable to start migration : ES Audit backend is not activated");
            return "Unable to start migration : ES Audit backend is not activated";
        }

        ESAuditBackend esBackend = (ESAuditBackend) backend;
        if (batchSize == 0) {
            batchSize = 1000;
        }
        return esBackend.migrate(batchSize);

    }
}
