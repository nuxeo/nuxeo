package org.nuxeo.runtime.management;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.modelmbean.ModelMBeanInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.inspector.ModelMBeanInfoFactory;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestResourcePublisherService extends NXRuntimeTestCase {

    protected static final String OSGI_BUNDLE_NAME = "org.nuxeo.runtime.management";

    protected static final String OSGI_BUNDLE_NAME_TESTS = OSGI_BUNDLE_NAME
            + ".tests";

    @SuppressWarnings("unused")
    private Log log = LogFactory.getLog(TestResourcePublisherService.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib(OSGI_BUNDLE_NAME, "OSGI-INF/management-server-locator-service.xml");
        deployContrib(OSGI_BUNDLE_NAME, "OSGI-INF/management-resource-publisher-service.xml");

        locatorService = (ServerLocatorService) Framework.getLocalService(ServerLocator.class);
        publisherService = (ResourcePublisherService) Framework.getLocalService(ResourcePublisher.class);
    }

    @Override
    public void tearDown() throws Exception {
        Framework.getRuntime().stop();
        super.tearDown();
    }

    protected ResourcePublisherService publisherService;
    protected ServerLocatorService locatorService;

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
        publisherService.registerResource("dummy", "nx:name=dummy",
                DummyMBean.class, new DummyService());
        Set<ObjectName> registeredNames = doQuery("nx:name=dummy");
        assertNotNull(registeredNames);
        assertEquals(1, registeredNames.size());
    }

    public void testRegisterFactory() throws Exception {
        ResourceFactoryDescriptor descriptor = new ResourceFactoryDescriptor(
                DummyFactory.class);
        publisherService.registerContribution(descriptor, "factories", null);
        Set<ObjectName> registeredNames = doQuery("nx:name=dummy");
        assertNotNull(registeredNames);
        assertEquals(registeredNames.size(), 1);
    }

    public void testServerLocator() throws Exception {
        MBeanServer testServer = MBeanServerFactory.createMBeanServer("test");
        ObjectName testName = new ObjectName("test:test=test");
        publisherService.bindForTest(testServer, testName, new DummyService(), DummyMBean.class);
        locatorService.registerLocator("test", true);
        MBeanServer locatedServer = locatorService.lookupServer(testName);
        assertNotNull(locatedServer);
        assertTrue(locatedServer.isRegistered(testName));
    }

    public void testXMLConfiguration() throws Exception {
        deployContrib(OSGI_BUNDLE_NAME_TESTS, "management-tests-service.xml");
        deployContrib(OSGI_BUNDLE_NAME_TESTS, "management-tests-contrib.xml");
        String qualifiedName = ObjectNameFactory.formatTypeQuery("service");

        Set<ObjectName> registeredNames = doQuery(qualifiedName);
        assertNotNull(registeredNames);
        assertEquals(4, registeredNames.size());

        Set<String> shortcutsName = publisherService.getShortcutsName();
        assertNotNull(shortcutsName);
        assertEquals(5, shortcutsName.size());
        assertEquals("dummy", shortcutsName.iterator().next());
    }

}
