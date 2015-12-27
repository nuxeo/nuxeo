/*******************************************************************************
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *******************************************************************************/
package org.nuxeo.ecm.core.management.works;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
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
    public int getScheduledCount() {
        return manager().getQueueSize(queueId, State.SCHEDULED);
    }

    @Override
    public int getRunningCount() {
        return manager().getQueueSize(queueId, State.RUNNING);
    }

    @Override
    public int getCompletedCount() {
        return manager().getQueueSize(queueId, State.COMPLETED);
    }

    @Override
    public String[] getScheduledWorks() {
        return listWorks(State.SCHEDULED);
    }

    @Override
    public String[] getRunningWorks() {
        return listWorks(State.RUNNING);
    }

    protected String[] listWorks(State state) {
        List<String> works = new ArrayList<String>();
        for (Work work : manager().listWork(queueId, state)) {
            works.add(work.toString());
        }
        return works.toArray(new String[works.size()]);
    }
}
