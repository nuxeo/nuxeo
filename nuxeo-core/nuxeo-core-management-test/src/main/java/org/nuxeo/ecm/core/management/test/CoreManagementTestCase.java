package org.nuxeo.ecm.core.management.test;

import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public abstract class CoreManagementTestCase extends SQLRepositoryTestCase {

        @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.ecm.core.management");
        deployBundle("org.nuxeo.ecm.core.management.test");
        deployOtherBundles();
        super.fireFrameworkStarted();
        openSession();
    }

        protected  void deployOtherBundles() {
            ;
        }

}
