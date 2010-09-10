package org.nuxeo.ecm.platform.queue.core;

import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

public abstract class QueueTestCase extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        NamingContextFactory.setAsInitial();
        NuxeoContainer.initTransactionManagement();
        super.setUp();
        deployBundle("org.nuxeo.runtime.management");
        deployBundle("org.nuxeo.runtime.jtajca");
        deployBundle("org.nuxeo.ecm.core.management");
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.lock.api");
        deployBundle("org.nuxeo.ecm.platform.lock.core");
        // deploying datasource for tests
        deployTestContrib("org.nuxeo.ecm.platform.lock.core",
                "nxlocks-tests.xml");
        deployBundle("org.nuxeo.ecm.platform.heartbeat.api");
        deployBundle("org.nuxeo.ecm.platform.heartbeat");
        deployBundle("org.nuxeo.ecm.platform.queue.api");
        deployBundle("org.nuxeo.ecm.platform.queue");
        deployBundle("org.nuxeo.ecm.platform.queue.test");
        super.fireFrameworkStarted();
        openSession();
    }

}
