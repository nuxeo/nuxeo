package org.nuxeo.runtime.management;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.ReflectionException;

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

        deployContrib(OSGI_BUNDLE_NAME, "OSGI-INF/management-resource-publisher-service.xml");

        managementService = (ResourcePublisherService) Framework.getRuntime().getComponent(
                ResourcePublisherService.NAME);
    }

    @Override
    public void tearDown() throws Exception {
        Framework.getRuntime().stop();
        super.tearDown();
    }

    private ResourcePublisherService managementService = null;

    public void testRegisteredService() throws Exception {
        assertNotNull(Framework.getService(ResourcePublisher.class));
    }

    protected final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

    protected void doBindResources() throws InstanceNotFoundException,
            ReflectionException, MBeanException {
        String qualifiedName = ObjectNameFactory.formatQualifiedName(ResourcePublisherService.NAME);
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        mbeanServer.invoke(objectName, "bindResources", null, null);
    }

    @SuppressWarnings("unchecked")
    protected Set<ObjectName> doQuery(String name) {
        String qualifiedName = ObjectNameFactory.getQualifiedName(name);
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        return mbeanServer.queryNames(objectName, null);
    }
    
    public void testRegisterResource() throws Exception {
        managementService.registerResource("dummy", "nx:name=dummy",
                DummyMBean.class, new DummyService());
        Set<ObjectName> registeredNames = doQuery("nx:name=dummy");
        assertNotNull(registeredNames);
        assertEquals(1, registeredNames.size());
    }

    public void testRegisterFactory() throws Exception {
        ResourceFactoryDescriptor descriptor = new ResourceFactoryDescriptor(
                DummyFactory.class);
        managementService.registerContribution(descriptor, "factories", null);
        Set<ObjectName> registeredNames = doQuery("nx:name=dummy");
        assertNotNull(registeredNames);
        assertEquals(registeredNames.size(), 1);
    }

    @SuppressWarnings("unchecked")
    public void testMBeanLocator() throws Exception {
        MBeanServer myServer = MBeanServerFactory.createMBeanServer("test");
        managementService.registerContribution(
                new MBeanServerLocatorDescriptor("test"), "locators",
                null);
        managementService.registerResource("dummy", "nx:name=dummy",
                DummyMBean.class, new DummyService());
        Set<ObjectName> registeredNames = myServer.queryNames(
                ObjectNameFactory.getObjectName("nx:name=dummy"), null);
        assertNotNull(registeredNames);
        assertEquals(1, registeredNames.size());
        assertEquals(registeredNames.iterator().next().getCanonicalName(),
                "nx:name=dummy");
    }

    public void testXMLConfiguration() throws Exception {
        deployContrib(OSGI_BUNDLE_NAME, "management-tests-service.xml");
        deployContrib(OSGI_BUNDLE_NAME, "management-tests-contrib.xml");
        String qualifiedName = ObjectNameFactory.formatTypeQuery("service");

        Set<ObjectName> registeredNames = doQuery(qualifiedName);
        assertNotNull(registeredNames);
        assertEquals(4, registeredNames.size());

        Set<String> shortcutsName = managementService.getShortcutsName();
        assertNotNull(shortcutsName);
        assertEquals(4, shortcutsName.size());
        assertEquals("dummy", shortcutsName.iterator().next());
    }

}
