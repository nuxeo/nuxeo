package org.nuxeo.ecm.platform.audit;

import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.extension.AdapterDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestAdapterRegistration extends NXRuntimeTestCase{
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
                deployBundle("org.nuxeo.runtime.management");
                deployBundle("org.nuxeo.ecm.core.event");
		deployBundle("org.nuxeo.ecm.platform.audit.api");
                deployBundle("org.nuxeo.ecm.platform.audit"); // the audit.core
                deployBundle("org.nuxeo.ecm.platform.audit.tests"); // the audit.core
                //deployTestContrib("org.nuxeo.ecm.platform.audit", "OSGI-INF/test-audit-contrib.xml");
                deployTestContrib("org.nuxeo.ecm.platform.audit.tests", "test-audit-contrib.xml");
	}

	
	public void testAuditContribution() throws Exception {
		NXAuditEventsService auditService = (NXAuditEventsService) Framework.getLocalService(NXAuditEvents.class);
		assertNotNull(auditService);
		AdapterDescriptor[] registeredAdapters = auditService.getRegisteredAdapters();
		assertEquals(1, registeredAdapters.length);
		
		AdapterDescriptor ad = registeredAdapters[0];
		assertEquals("myadapter", ad.getName());
		
	}
	
}
