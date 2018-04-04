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

import javax.inject.Inject;
import java.lang.reflect.Field;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link EventJobFactory} by contributing a job using {@link org.quartz.DisallowConcurrentExecution}.
 *
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.event")
@LocalDeploy("org.nuxeo.ecm.core.event.test:test-jobfactory.xml") // jobs
public class EventJobFactoryTest {
    @Inject
    private SchedulerService schedulerService;

    @Test
    public void test() throws ReflectiveOperationException, SchedulerException {
        Field schedulerField = SchedulerServiceImpl.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        Scheduler scheduler = (Scheduler) schedulerField.get(schedulerService);

        // job without the DisallowConcurrentExecution annotation
        JobKey jobKey = new JobKey("testSchedulerMultipleExecutions", "nuxeo");
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        assertThat(jobDetail).isNotNull();
        assertThat(jobDetail.isConcurrentExectionDisallowed()).isFalse();

        // job with the DisallowConcurrentExecution annotation
        jobKey = new JobKey("testSchedulerSingleExecution", "nuxeo");
        jobDetail = scheduler.getJobDetail(jobKey);
        assertThat(jobDetail).isNotNull();
        assertThat(jobDetail.isConcurrentExectionDisallowed()).isTrue();
    }
}
