/**
 *
 */
package org.nuxeo.ecm.platform.management.probes;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.runtime.api.Framework;

public class ProbeScheduleListener implements EventListener {

    protected ProbeSchedulerService service;

    public void handleEvent(Event event)  {
        if (service == null) {
            service = (ProbeSchedulerService)Framework.getLocalService(ProbeScheduler.class);
        }
        service.runnerRegistry.doRun();
    }

}
