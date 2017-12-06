/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.core.management.works;

import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.AbstractResourceFactory;
import org.nuxeo.runtime.management.ObjectNameFactory;

public class WorksMonitoringFactory extends AbstractResourceFactory {

    protected WorkManager manager() {
        return Framework.getService(WorkManager.class);
    }

    @Override
    public void registerResources() {
        WorkManager mgr = manager();
        service.registerResource("works",
                ObjectNameFactory.formatQualifiedName("org.nuxeo", "service", WorkManager.class.getName()),
                WorksMonitoringMBean.class, new WorksMonitoring());
        for (String eachId : mgr.getWorkQueueIds()) {
            service.registerResource(
                    eachId,
                    ObjectNameFactory.formatQualifiedName("org.nuxeo", "service", WorkManager.class.getName() + "."
                            + eachId), WorksQueueMonitoringMBean.class, new WorksQueueMonitoring(eachId));
        }
    }

}
