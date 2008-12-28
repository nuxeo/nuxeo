package org.nuxeo.runtime.management;

import java.util.Set;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestManagementService extends NXRuntimeTestCase {

    protected static final String OSGI_BUNDLE_NAME = "org.nuxeo.runtime.management";

    protected static final String OSGI_BUNDLE_NAME_TESTS = OSGI_BUNDLE_NAME
            + ".tests";

    private Log log = LogFactory.getLog(TestManagementService.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib(OSGI_BUNDLE_NAME, "OSGI-INF/management-contrib.xml");
        deployContrib(OSGI_BUNDLE_NAME_TESTS,
                "OSGI-INF/management-tests-contrib.xml");

        fetchManagementService();
    }

    private ManagementService managementService = null;

    private ManagementService fetchManagementService() throws Exception {
        if (managementService != null) {
            return managementService;
        }

        managementService = Framework.getService(ManagementService.class);

        return managementService;
    }

    public void testQuery() {
        Set<ObjectName> services = managementService.getServicesName();
        assertNotNull(services);
        assertEquals(5, services.size());
    }

    public void testQueryResources() {
        Set<ObjectName> resources = managementService.getResourcesName();
        assertNotNull(resources);
        assertTrue(resources.size() > 5);
    }

}
