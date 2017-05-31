/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.runtime.api.Framework;

public class WorksQueueMonitoring implements WorksQueueMonitoringMBean {

    protected final String queueId;

    public WorksQueueMonitoring(String id) {
        queueId = id;
    }

    protected WorkManager manager() {
        return Framework.getLocalService(WorkManager.class);
    }

    @Override
    public long[] getMetrics() {
        WorkQueueMetrics metrics = manager().getMetrics(queueId);
        return new long[] { metrics.scheduled.longValue(), metrics.running.longValue(), metrics.completed.longValue(),
                metrics.canceled.longValue() };
    }

    @Override
    public boolean isProcessing() {
        return manager().isProcessingEnabled(queueId);
    }

    @Override
    public boolean toggleProcessing() throws InterruptedException {
        WorkManager manager = manager();
        boolean enabled = !manager.isProcessingEnabled(queueId);
        manager.enableProcessing(queueId, enabled);
        return enabled;
    }

}
