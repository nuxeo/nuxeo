/*******************************************************************************
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.nuxeo.ecm.core.management.works;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

public class WorksQueueMonitoring implements WorksQueueMonitoringMBean  {

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
        for (Work work:manager().listWork(queueId, state)) {
            works.add(work.toString());
        }
        return works.toArray(new String[works.size()]);
    }
}
