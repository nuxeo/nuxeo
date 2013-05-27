/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSchedulerService extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
        deployContrib("org.nuxeo.ecm.core.event.test",
                "OSGI-INF/test-scheduler-eventlistener.xml");
        DummyEventListener.setCount(0);
        fireFrameworkStarted();
    }

    @Test
    public void testScheduleRegistration() throws Exception {
        deployContrib("org.nuxeo.ecm.core.event.test",
                "OSGI-INF/test-scheduler-config.xml");
        Thread.sleep(5000); // so that job is called at least once
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
        Thread.sleep(5000); // so that job is called at least once

        long count = DummyEventListener.getCount();
        assertTrue("count " + count, count >= 1);
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
        @SuppressWarnings("boxing")
        String cronExpression = String.format("%s %s %s %s %s ? %s",
                cal.get(Calendar.SECOND), //
                cal.get(Calendar.MINUTE), //
                cal.get(Calendar.HOUR_OF_DAY), //
                cal.get(Calendar.DAY_OF_MONTH), //
                cal.get(Calendar.MONTH) + 1, //
                cal.get(Calendar.YEAR));
        schedule.cronExpression = cronExpression;
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put("flag", "1");
        service.registerSchedule(schedule, parameters);
        Thread.sleep(5000); // so that job is called at least once
        long count = DummyEventListener.getCount();
        assertTrue("count " + count, count < 0);
        boolean unregistered = service.unregisterSchedule(schedule.id);
        // schedule should happen only one time, it has already been
        // unregistered
        assertFalse(unregistered);
    }

}
