package org.nuxeo.ecm.platform.publisher.test;

import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;

import java.util.List;

public class TestServiceRegistration extends SQLRepositoryTestCase {

    public TestServiceRegistration(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.publisher.core.contrib");
        deployBundle("org.nuxeo.ecm.platform.publisher.core");

        openSession();
        fireFrameworkStarted();
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
        assertEquals(1, treeNames.size());
    }

}
