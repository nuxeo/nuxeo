package org.nuxeo.runtime.management;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestManagementService extends NXRuntimeTestCase {

    protected static final String OSGI_BUNDLE_NAME = "org.nuxeo.runtime.management";

    protected static final String OSGI_BUNDLE_NAME_TESTS = OSGI_BUNDLE_NAME
            + ".tests";

    @SuppressWarnings("unused")
    private Log log = LogFactory.getLog(TestManagementService.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        deployContrib(OSGI_BUNDLE_NAME, "OSGI-INF/management-contrib.xml");

        managementService = (ManagementServiceImpl) Framework.getRuntime().getComponent(
                ManagementServiceImpl.NAME);
    }
    
    @Override
    public void tearDown() throws Exception {
        Framework.getRuntime().stop();
        super.tearDown();
    }

    protected final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    private ManagementServiceImpl managementService = null;

    public void testRegisteredService() throws Exception {
        assertNotNull(Framework.getService(ManagementService.class));
    }

    @SuppressWarnings("unchecked")
    public void testRegisterResource() throws Exception {
        ResourceDescriptor descriptor = new ResourceDescriptor(
                ObjectNameFactory.getObjectName("dummy"),
                DummyServiceImpl.class, DummyService.class, true);
        managementService.registerContribution(descriptor, "resources", null);
        String qualifiedName = ObjectNameFactory.formatName("dummy");
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        Set<ObjectName> registeredNames = mbeanServer.queryNames(objectName,
                null);
        assertNotNull(registeredNames);
        assertEquals(registeredNames.size(), 1);
        assertEquals(registeredNames.iterator().next(), objectName);
    }

    @SuppressWarnings("unchecked")
    public void testRegisterFactory() throws Exception {
        ResourceFactoryDescriptor descriptor = new ResourceFactoryDescriptor(
                ObjectNameFactory.getObjectName("dummy"),
                DummyMBeanFactory.class);
        managementService.registerContribution(descriptor, "factories", null);
        String qualifiedName = ObjectNameFactory.formatName("dummy");
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        Set<ObjectName> registeredNames = mbeanServer.queryNames(objectName,
                null);
        assertNotNull(registeredNames);
        assertEquals(registeredNames.size(), 1);
        assertEquals(registeredNames.iterator().next(), objectName);
    }

    @SuppressWarnings("unchecked")
    public void testXMLConfiguration() throws Exception {
        deployContrib(OSGI_BUNDLE_NAME_TESTS,
                "OSGI-INF/management-tests-contrib.xml");
        String qualifiedName = ObjectNameFactory.formatTypeQuery("service");
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        Set<ObjectName> registeredNames = mbeanServer.queryNames(objectName,
                null);
        assertNotNull(registeredNames);
        assertEquals(6, registeredNames.size());
    }

}
