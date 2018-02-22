/*
 * (C) Copyright 2007-2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.event")
@Deploy("org.nuxeo.ecm.core.event.test:OSGI-INF/test-scheduler-eventlistener.xml")
public class TestSchedulerService {

    protected void waitUntilDummyEventListenerIsCalled(int maxRetry) throws Exception {
        waitUntilDummyEventListenerIsCalled(maxRetry, 1);
    }

    protected void waitUntilDummyEventListenerIsCalled(int maxRetry, int minCountValue) throws Exception {
        long count = DummyEventListener.getCount();
        int retry = 0;
        while (count < minCountValue && retry < maxRetry * 2) {
            Thread.sleep(500);
            count = DummyEventListener.getCount();
            retry++;
        }
    }

    @Inject
    protected HotDeployer hotDeployer;

    @Before
    public void setUp() throws Exception {
        DummyEventListener.setCount(0);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.event.test:OSGI-INF/test-scheduler-config.xml")
    public void testScheduleRegistration() throws Exception {

        waitUntilDummyEventListenerIsCalled(10); // so that job is called at least once
        long count = DummyEventListener.getCount();
        assertTrue("count " + count, count >= 1);
    }

    @Test
    public void testScheduleManualRegistration() throws Exception {
        SchedulerService service = Framework.getService(SchedulerService.class);
        ScheduleImpl schedule = new ScheduleImpl();
        schedule.cronExpression = "*/1 * * * * ?";
        schedule.id = "testing";
        schedule.username = "Administrator";
        schedule.eventId = "testEvent";
        schedule.eventCategory = "default";
        service.registerSchedule(schedule);
        waitUntilDummyEventListenerIsCalled(10); // so that job is called at least once

        boolean unregistered = service.unregisterSchedule(schedule.id);
        // schedule can happen again, it hasn't been unregistered after first
        // launch.
        assertTrue(unregistered);
    }

    @Test
    public void testScheduleManualRegistrationWithParameters() throws Exception {
        SchedulerService service = Framework.getService(SchedulerService.class);
        ScheduleImpl schedule = new ScheduleImpl();
        schedule.id = "testing";
        schedule.username = "Administrator";
        schedule.eventId = "testEvent";
        schedule.eventCategory = "default";
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, 3);
        String cronExpression = String.format("%s %s %s %s %s ? %s", cal.get(Calendar.SECOND), //
                cal.get(Calendar.MINUTE), //
                cal.get(Calendar.HOUR_OF_DAY), //
                cal.get(Calendar.DAY_OF_MONTH), //
                cal.get(Calendar.MONTH) + 1, //
                cal.get(Calendar.YEAR));
        schedule.cronExpression = cronExpression;
        Map<String, Serializable> parameters = new HashMap<>();
        parameters.put("flag", "1");
        service.registerSchedule(schedule, parameters);
        waitUntilDummyEventListenerIsCalled(10); // so that job is called at least once
        long count = DummyEventListener.getCount();
        assertTrue("count " + count, count < 0);
        boolean unregistered = service.unregisterSchedule(schedule.id);
        // schedule should happen only one time, it has already been
        // unregistered
        assertFalse(unregistered);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.event.test:OSGI-INF/test-scheduler-config.xml")
    public void testDisableSchedule() throws Exception {

        waitUntilDummyEventListenerIsCalled(10); // so that job is called at least once
        long count = DummyEventListener.getCount();
        assertTrue(count >= 1);
        hotDeployer.deploy("org.nuxeo.ecm.core.event.test:OSGI-INF/test-scheduler-disabled-config.xml");

        count = DummyEventListener.getCount();
        Thread.sleep(5000);
        long newCount = DummyEventListener.getCount();
        assertTrue(count == newCount);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.event.test:OSGI-INF/test-scheduler-config.xml")
    public void testOverrideSchedule() throws Exception {

        waitUntilDummyEventListenerIsCalled(10); // so that job is called at least once
        long count = DummyEventListener.getCount();
        assertTrue(count >= 1);
        hotDeployer.deploy("org.nuxeo.ecm.core.event.test:OSGI-INF/test-scheduler-override-config.xml");

        long newCount = DummyEventListener.getNewCount();
        int retry = 0;
        while (newCount <= 0 && retry < 20) {
            Thread.sleep(500);
            newCount = DummyEventListener.getNewCount();
            retry++;
        }
        newCount = DummyEventListener.getNewCount();
        assertTrue(newCount >= 1);
    }

}
