package org.nuxeo.ecm.core.management.works;

import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.AbstractResourceFactory;
import org.nuxeo.runtime.management.ObjectNameFactory;

public class WorksMonitoringFactory extends AbstractResourceFactory {

    protected WorkManager manager() {
        return Framework.getLocalService(WorkManager.class);
    }

    @Override
    public void registerResources() {
        WorkManager mgr = manager();
        service.registerResource("works", ObjectNameFactory.formatQualifiedName("org.nuxeo", "service", WorkManager.class.getName()),
                WorksMonitoringMBean.class, new WorksMonitoring());
        for (String eachId:mgr.getWorkQueueIds()) {
            service.registerResource(eachId,
                    ObjectNameFactory.formatQualifiedName("org.nuxeo", "service", WorkManager.class.getName() + "." + eachId),
                    WorksQueueMonitoringMBean.class, new WorksQueueMonitoring(eachId));
        }
    }

}
