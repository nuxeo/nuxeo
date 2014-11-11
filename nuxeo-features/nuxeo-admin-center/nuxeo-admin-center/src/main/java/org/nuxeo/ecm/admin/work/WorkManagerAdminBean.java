/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.admin.work;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.ecm.core.work.SleepWork;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam bean that wraps the {@link WorkManager} service to provide a JSF admin
 * UI.
 */
@Name("workManagerAdmin")
@Scope(ScopeType.PAGE)
public class WorkManagerAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @RequestParameter("queueId")
    protected String queueId;

    protected WorkManager getWorkManager() {
        return Framework.getLocalService(WorkManager.class);
    }

    public List<Map<String, Object>> getWorkQueuesInfo() {
        List<Map<String, Object>> info = new ArrayList<Map<String, Object>>();
        WorkManager workManager = getWorkManager();
        List<String> workQueueIds = workManager.getWorkQueueIds();
        Collections.sort(workQueueIds);
        for (String queueId : workQueueIds) {
            List<Work> running = workManager.listWork(queueId, State.RUNNING);
            int scheduled = workManager.getQueueSize(queueId, State.SCHEDULED);
            int completed = workManager.getQueueSize(queueId,
                    State.COMPLETED);
            Map<String, Object> map = new HashMap<String, Object>();
            info.add(map);
            map.put("id", queueId);
            map.put("scheduled", scheduled);
            map.put("running", running.size());
            map.put("completed", completed);
            map.put("runningWorks", running);
        }
        return info;
    }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public String clearQueueCompletedWork() {
        getWorkManager().clearCompletedWork(queueId);
        return null;
    }

    public String clearAllCompletedWork() {
        getWorkManager().clearCompletedWork(0);
        return null;
    }

    public String startTestWork() {
        getWorkManager().schedule(new SleepWork(10000));
        return null;
    }

}
