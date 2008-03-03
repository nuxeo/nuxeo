package org.nuxeo.ecm.platform.events.listener;

import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.NXCoreEvent;

public class EventCombo {

    private NXCoreEvent event;

    private EventMessage message;

    public EventCombo(NXCoreEvent event, EventMessage message) {
        this.event = event;
        this.message = message;
    }

    public NXCoreEvent getEvent() {
        return event;
    }

    public void setEvent(NXCoreEvent event) {
        this.event = event;
    }

    public EventMessage getMessage() {
        return message;
    }

    public void setMessage(EventMessage message) {
        this.message = message;
    }

}
