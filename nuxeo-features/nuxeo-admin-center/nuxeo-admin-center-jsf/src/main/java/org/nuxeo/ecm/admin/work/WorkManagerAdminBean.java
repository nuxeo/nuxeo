/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam bean that wraps the {@link WorkManager} service to provide a JSF admin UI.
 */
@Name("workManagerAdmin")
@Scope(ScopeType.PAGE)
public class WorkManagerAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @RequestParameter("queueId")
    protected String queueId;

    protected WorkManager getWorkManager() {
        return Framework.getService(WorkManager.class);
    }

    public List<Map<String, Object>> getWorkQueuesInfo() {
        List<Map<String, Object>> info = new ArrayList<>();
        WorkManager workManager = getWorkManager();
        List<String> workQueueIds = workManager.getWorkQueueIds();
        Collections.sort(workQueueIds);
        for (String queueId : workQueueIds) {
            WorkQueueMetrics metrics = workManager.getMetrics(queueId);

            Map<String, Object> map = new HashMap<>();
            map.put("id", queueId);
            map.put("scheduled", metrics.scheduled);
            map.put("completed", metrics.completed);
            map.put("running", metrics.running);
            info.add(map);
        }
        return info;
    }

    public long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    public String startTestWork() {
        getWorkManager().schedule(new SleepWork(10000));
        return null;
    }

}
