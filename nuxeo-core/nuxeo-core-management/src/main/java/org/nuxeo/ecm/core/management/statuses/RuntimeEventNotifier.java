package org.nuxeo.ecm.core.management.statuses;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;

public class RuntimeEventNotifier implements Notifier {

    public static final String RUNTIME_EVENT_TOPIC ="administrativeStatus";

    @Override
    public void notifyEvent(String eventName, String instanceIdentifier, String serviceIdentifier) {

        Event evnt = new Event(RUNTIME_EVENT_TOPIC, eventName, instanceIdentifier, serviceIdentifier);
        EventService evtService = Framework.getLocalService(EventService.class);
        evtService.sendEvent(evnt);

    }

}
