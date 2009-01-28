package org.nuxeo.ecm.platform.audit;

import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestServiceAccess extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.audit.api");
        deployContrib("org.nuxeo.ecm.platform.audit",
                "OSGI-INF/nxaudit-service-definitions.xml");
    }

    public void testFullAccess() throws Exception {

        NXAuditEvents fullService = Framework.getLocalService(NXAuditEvents.class);
        assertNotNull(fullService);

        if (!(fullService instanceof NXAuditEventsService)) {
            fail("");
        }
    }

    public void testReadAccess() throws Exception {

        AuditReader reader= Framework.getLocalService(AuditReader.class);
        assertNotNull(reader);

        if (!(reader instanceof NXAuditEventsService)) {
            fail("");
        }
    }

    public void testWriteAccess() throws Exception {

        AuditLogger writer= Framework.getLocalService(AuditLogger.class);
        assertNotNull(writer);

        if (!(writer instanceof NXAuditEventsService)) {
            fail("");
        }
    }

}
