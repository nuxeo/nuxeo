package org.nuxeo.ecm.platform.publisher.test;

import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import java.util.List;

public class TestServiceRegistration extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.publisher.core",
                "OSGI-INF/publisher-framework.xml");
    }

    public void testMainService() throws Exception {
        PublisherService service = Framework.getLocalService(PublisherService.class);
        assertNotNull(service);
    }

    public void testTreeService() throws Exception {
        RemotePublicationTreeManager service = Framework.getLocalService(RemotePublicationTreeManager.class);
        assertNotNull(service);
    }

    public void testContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.publisher.core",
                "OSGI-INF/publisher-contrib.xml");
        PublisherService service = Framework.getLocalService(PublisherService.class);
        List<String> treeNames = service.getAvailablePublicationTree();

        assertTrue(treeNames.contains("DefaultSectionsTree"));

    }

}
