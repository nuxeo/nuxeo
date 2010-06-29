package org.nuxeo.ecm.core.api.repository.cache;

import org.nuxeo.ecm.core.api.operation.OperationEvent;

public interface OperationEventsListener {

    void handleEvents(OperationEvent[] events, boolean isUrgent);

}
