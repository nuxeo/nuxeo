package org.nuxeo.ecm.core.scheduler;

import java.util.Collection;

import org.nuxeo.runtime.model.SimpleContributionRegistry;

public class ScheduleExtensionRegistry extends
        SimpleContributionRegistry<Schedule> {


    @Override
    public String getContributionId(Schedule contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id, Schedule contrib,
            Schedule newOrigContrib) {
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
