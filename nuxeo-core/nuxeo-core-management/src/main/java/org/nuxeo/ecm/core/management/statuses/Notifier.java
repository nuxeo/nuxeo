package org.nuxeo.ecm.core.management.statuses;

public interface Notifier {

    void notifyEvent(String eventName, String instanceIdentifier, String serviceIdentifier);

}
