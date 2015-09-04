package org.nuxeo.ecm.platform.query.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;

public class SearchEventsAccumulator implements EventListener {

    protected static List<Map<String, Serializable>> eventsData = new ArrayList<Map<String,Serializable>>();

    public static void reset() {
        eventsData.clear();
    }

    public static List<Map<String, Serializable>> getStackedEvents() {
        return eventsData;
    }

    @Override
    public void handleEvent(Event event) {

        if ("search".equals(event.getName())) {
            Map<String, Serializable> props = event.getContext().getProperties();
            props.remove("coreSession");
            eventsData.add(props);
        }

    }
}
