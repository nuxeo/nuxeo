package org.nuxeo.ecm.platform.events.tests;

import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventServiceImpl;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.events.tests.mock.MockEventBundleJMSListener;
import org.nuxeo.ecm.platform.events.tests.mock.MockJMSEventForwarder;
import org.nuxeo.runtime.api.Framework;

public abstract class ListenerIntegrationTestCase extends RepositoryOSGITestCase {

    protected boolean useJMS = true;

    protected boolean asyncJMS = false;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.event-bridge");
        if (useJMS) {
            //deployBundle("org.nuxeo.ecm.core.event.jms");
            if (asyncJMS) {
                deployContrib("org.nuxeo.ecm.platform.event-bridge.tests", "OSGI-INF/test-jmslistener-async-contrib.xml");
            }
            else {
                deployContrib("org.nuxeo.ecm.platform.event-bridge.tests", "OSGI-INF/test-jmslistener-contrib.xml");
            }
            MockEventBundleJMSListener.reset();
            MockJMSEventForwarder.reset();
        }
        openRepository();
    }


    protected void waitForAsyncExec() {

        EventServiceImpl evtService = (EventServiceImpl) Framework.getLocalService(EventService.class);
        int runningTasks = evtService.getActiveAsyncTaskCount();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (evtService.getActiveAsyncTaskCount()>0) {
            try {
                Thread.sleep(100);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
