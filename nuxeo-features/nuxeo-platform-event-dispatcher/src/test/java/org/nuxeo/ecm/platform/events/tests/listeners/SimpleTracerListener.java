package org.nuxeo.ecm.platform.events.tests.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;

public class SimpleTracerListener implements PostCommitEventListener {

    protected static int invocationCount = 0;
    protected static int jmsInvocationCount = 0;

    protected static Map<String, List<String>> collectedEvents = new HashMap<String, List<String>>();
    protected static List<String> collectedBundleNames = new ArrayList<String>();

    public static void reset() {
        invocationCount = 0;
        collectedEvents = new HashMap<String, List<String>>();
        collectedBundleNames = new ArrayList<String>();
    }

    public void handleEvent(EventBundle events) throws ClientException {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        invocationCount+=1;
        String eventBundleName = events.getName();
        collectedBundleNames.add(eventBundleName);
        collectedEvents.put(eventBundleName, Arrays.asList(events.getEventNames()));

        if (events instanceof ReconnectedEventBundle) {
            ReconnectedEventBundle bundle = (ReconnectedEventBundle) events;
            if (bundle.comesFromJMS()) {
                jmsInvocationCount+=1;
            }
        }
    }


    public static int getInvocationCount() {
        return invocationCount;
    }

    public static int getJMSInvocationCount() {
        return jmsInvocationCount;
    }

    public static List<String> getCollectedBundleNames() {
        return collectedBundleNames;
    }


    public static Map<String, List<String>> getCollectedEvents() {
        return collectedEvents;
    }

    public static int getEventsCount() {
        int eventCount = 0;
        for (String bundle : collectedBundleNames) {
            eventCount = eventCount + collectedEvents.get(bundle).size();
        }
        return eventCount;
    }

}
