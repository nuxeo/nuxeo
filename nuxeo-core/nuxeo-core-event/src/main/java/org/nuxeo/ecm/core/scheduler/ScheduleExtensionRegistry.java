/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.scheduler;

import java.util.Collection;

import org.nuxeo.runtime.model.SimpleContributionRegistry;

public class ScheduleExtensionRegistry extends SimpleContributionRegistry<Schedule> {

    @Override
    public String getContributionId(Schedule contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id, Schedule contrib, Schedule newOrigContrib) {
        if (contrib.isEnabled()) {
            currentContribs.put(id, contrib);
        } else {
            currentContribs.remove(id);
        }
    }

    protected Collection<Schedule> getSchedules() {
        return currentContribs.values();
    }

    protected Schedule getSchedule(Schedule schedule) {
        return currentContribs.get(getContributionId(schedule));
    }

    protected Schedule getSchedule(String id) {
        return currentContribs.get(id);
    }
}
