/*
 * (C) Copyright 2007-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Munch
 */
package org.nuxeo.ecm.core.scheduler;

import java.io.Serializable;
import java.util.Map;
import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.ScheduleBuilder;
import org.quartz.TriggerBuilder;

/**
 * Default implementation of {@link EventJobFactory} instantiating an {@link EventJob} with a {@link CronTrigger}.
 *
 * @since 10.2
 */
public class DefaultEventJobFactory implements EventJobFactory {
    @Override
    public JobBuilder buildJob(Schedule schedule, Map<String, Serializable> parameters) {
        JobDataMap map = new JobDataMap();
        if (parameters != null) {
            map.putAll(parameters);
        }

        return JobBuilder.newJob(getJobClass())
                .withIdentity(schedule.getId(), "nuxeo")
                .usingJobData(map)
                .usingJobData("eventId", schedule.getEventId())
                .usingJobData("eventCategory", schedule.getEventCategory())
                .usingJobData("username", schedule.getUsername());
    }

    @Override
    public TriggerBuilder<?> buildTrigger(Schedule schedule) {
        return TriggerBuilder.newTrigger()
                .withIdentity(schedule.getId(), "nuxeo")
                .withSchedule(buildSchedule(schedule));
    }

    @Override
    public ScheduleBuilder<?> buildSchedule(Schedule schedule) {
        CronScheduleBuilder builder = CronScheduleBuilder.cronSchedule(schedule.getCronExpression());
        if (schedule.getTimeZone() != null) {
            builder.inTimeZone(TimeZone.getTimeZone(schedule.getTimeZone()));
        }
        return builder;
    }

    protected Class<? extends EventJob> getJobClass() {
        return EventJob.class;
    }
}
