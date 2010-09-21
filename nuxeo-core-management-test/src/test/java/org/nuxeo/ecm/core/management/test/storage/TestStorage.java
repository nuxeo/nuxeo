package org.nuxeo.ecm.core.management.test.storage;

import org.nuxeo.ecm.core.management.test.CoreManagementTestCase;
import org.nuxeo.ecm.core.management.test.FakeDocumentStoreHandler;

public class TestStorage extends CoreManagementTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.management");
        deployBundle("org.nuxeo.ecm.core.management.test");
        super.fireFrameworkStarted();
        openSession();
    }

    public void testRegistration() {
       assertNotNull("handler is not contributed", FakeDocumentStoreHandler.testInstance);
       assertTrue("handler is not invoked", FakeDocumentStoreHandler.testInstance.repositoryName != null);
       assertEquals("configuration is not contribued", "test", FakeDocumentStoreHandler.testInstance.repositoryName);
    }
}
