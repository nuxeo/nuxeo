package org.nuxeo.ecm.core.management.test.statuses;

import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

public class RuntimeListener implements EventListener {

    protected static boolean serverActivatedEventTriggered = false;
    protected static boolean serverPassivatedEventTriggered = false;

    public static void init() {
        serverActivatedEventTriggered = false;
        serverPassivatedEventTriggered = false;
    }

    public static boolean isServerActivatedEventTriggered() {
        return serverActivatedEventTriggered;
    }

    public static boolean isServerPassivatedEventTriggered() {
        return serverPassivatedEventTriggered;
    }

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

    @Override
    public void handleEvent(Event event) {
        String eventId = event.getId();
        String instanceId = (String) event.getSource();
        String serviceId = (String) event.getData();

        if (serviceId.equals(AdministrativeStatusManager.GLOBAL_INSTANCE_AVAILABILITY)) {
            if (eventId.equals(AdministrativeStatusManager.ACTIVATED_EVENT)) {
                serverActivatedEventTriggered = true;
            }
            if (eventId.equals(AdministrativeStatusManager.PASSIVATED_EVENT)) {
                serverPassivatedEventTriggered = true;
            }
        }

    }

}
