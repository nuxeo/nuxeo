package org.nuxeo.ecm.core.schema;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

public class FlushPendingsRegistrationOnReloadListener implements EventListener{

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return "reload".equals(event.getId());
    }

    @Override
    public void handleEvent(Event event) {
        if (!"reload".equals(event.getId())) {
            return;
        }
        SchemaManager mgr = Framework.getLocalService(SchemaManager.class);
        mgr.flushPendingsRegistration();
    }

}
