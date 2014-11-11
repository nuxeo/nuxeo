package org.nuxeo.ecm.platform.events.tests;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.jms.AsyncProcessorConfig;
import org.nuxeo.ecm.platform.events.tests.listeners.SimpleTracerListener;
import org.nuxeo.ecm.platform.events.tests.mock.MockEventBundleJMSListener;
import org.nuxeo.ecm.platform.events.tests.mock.MockJMSEventForwarder;

public class TestListenersWithJMS extends ListenerIntegrationTestCase {

    @Override
    protected void setUp() throws Exception {
        useJMS=true;
        super.setUp();
        SimpleTracerListener.reset();
    }


    protected void doCreateADoc() throws Exception {
           DocumentModel doc = getCoreSession().createDocumentModel("File");

           doc.setProperty("dublincore", "title", "MonTitre");
           doc.setPathInfo("/", "TestFile");

           doc = getCoreSession().createDocument(doc);
           getCoreSession().save();
           AsyncProcessorConfig.setForceJMSUsage(false);
    }

    public void testPostCommitSyncTracer() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.event-bridge.tests", "OSGI-INF/test-debuglistener-contrib.xml");
        doCreateADoc();
        waitForAsyncExec();
        assertEquals(1, SimpleTracerListener.getInvocationCount());
        assertEquals(4, SimpleTracerListener.getEventsCount());

        assertEquals(1, MockEventBundleJMSListener.getInvocationCount());
        assertEquals(1, MockJMSEventForwarder.getInvocationCount());
    }

    public void testPostCommitAsyncTracer() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.event-bridge.tests", "OSGI-INF/test-debuglistener-async-contrib.xml");
        doCreateADoc();
        waitForAsyncExec();
        assertEquals(1, SimpleTracerListener.getInvocationCount());
        assertEquals(4, SimpleTracerListener.getEventsCount());

        // ensure postCommit async listerners are only executed once
        assertEquals(1, MockEventBundleJMSListener.getInvocationCount());
        assertEquals(1, MockJMSEventForwarder.getInvocationCount());

        // by default invocation should be done via the core directly
        assertEquals(0, SimpleTracerListener.getJMSInvocationCount());
    }

    public void testPostCommitAsyncTracerViaJMS() throws Exception {

        AsyncProcessorConfig.setForceJMSUsage(true);

        deployContrib("org.nuxeo.ecm.platform.event-bridge.tests", "OSGI-INF/test-debuglistener-async-contrib.xml");
        doCreateADoc();
        waitForAsyncExec();
        assertEquals(1, SimpleTracerListener.getInvocationCount());
        assertEquals(4, SimpleTracerListener.getEventsCount());

        // ensure postCommit async listerners are only executed once
        assertEquals(1, MockEventBundleJMSListener.getInvocationCount());
        assertEquals(1, MockJMSEventForwarder.getInvocationCount());

        // invocation should now be done via JMS
        assertEquals(1, SimpleTracerListener.getJMSInvocationCount());
    }



}
