package org.nuxeo.ecm.platform.events.tests.mock;

import org.nuxeo.ecm.core.event.jms.JMSEventBundle;
import org.nuxeo.ecm.platform.events.EventBundlesJMSListener;

public class MockEventBundleJMSListener extends EventBundlesJMSListener {

    protected static int invocationCount = 0;

    public static int getInvocationCount() {
        return invocationCount;
    }

    public static void reset() {
        invocationCount=0;
    }


    public void onMessage(JMSEventBundle jmsEventBundle) {
        invocationCount+=1;
        processJMSEventBundle(jmsEventBundle);
    }
}

