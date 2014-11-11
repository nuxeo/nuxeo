/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: $
 */
package org.nuxeo.ecm.platform.scheduler.core;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.scheduler.core.interfaces.SchedulerRegistry;
import org.nuxeo.ecm.platform.scheduler.core.service.ScheduleImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestScheduler extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(TestScheduler.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // scheduler service
        deployContrib("org.nuxeo.ecm.platform.scheduler.core",
                "OSGI-INF/nxscheduler-service.xml");
        // our event listener
        deployContrib("org.nuxeo.ecm.platform.scheduler.core.tests",
                "test-eventlistener.xml");
        deployBundle("org.nuxeo.ecm.core.event");
    }

    @Override
    public void tearDown() throws Exception {
        undeployContrib("org.nuxeo.ecm.platform.scheduler.core.tests",
                "test-nxscheduler-service.xml");
        super.tearDown();
    }

    public void testScheduleRegistration() throws Exception {
        Whiteboard whiteboard = Whiteboard.getWhiteboard();
        whiteboard.setCount(0);

        deployContrib("org.nuxeo.ecm.platform.scheduler.core.tests",
                "test-nxscheduler.xml");
        try {
            Thread.sleep(2000); // 1s so that job is called at least once
        } catch (InterruptedException e) {
            log.error(e);
            fail("Timer failed");
        }

        Integer count = whiteboard.getCount();
        log.info("count " + count);
        undeployContrib("org.nuxeo.ecm.platform.scheduler.core.tests",
                "test-nxscheduler.xml");
        assertTrue("count " + count, count >= 1);
    }

    public void testScheduleManualRegistration() throws Exception {
        Whiteboard whiteboard = Whiteboard.getWhiteboard();
        whiteboard.setCount(0);
        SchedulerRegistry service = Framework.getService(SchedulerRegistry.class);
        ScheduleImpl schedule = new ScheduleImpl();
        schedule.cronExpression = "*/1 * * * * ?";
        schedule.id = "testing";
        schedule.username = "Administrator";
        schedule.password = "Administrator";
        schedule.eventId = "testEvent";
        schedule.eventCategory = "default";
        service.registerSchedule(schedule);
        try {
            Thread.sleep(2000); // 1s so that job is called at least once
        } catch (InterruptedException e) {
            log.error(e);
            fail("Timer failed");
        }

        Integer count = whiteboard.getCount();
        log.info("count " + count);
        undeployContrib("org.nuxeo.ecm.platform.scheduler.core.tests",
                "test-nxscheduler.xml");
        assertTrue("count " + count, count >= 1);
        Boolean unregistered = service.unregisterSchedule(schedule.id);
        // schedule can happen again, it hasn't benn unregistered after first launch.
        assertTrue(unregistered);
    }

    public void testScheduleManualRegistrationWithParameters() throws Exception {
        Whiteboard whiteboard = Whiteboard.getWhiteboard();
        whiteboard.setCount(0);
        SchedulerRegistry service = Framework.getService(SchedulerRegistry.class);
        ScheduleImpl schedule = new ScheduleImpl();
        schedule.id = "testing";
        schedule.username = "Administrator";
        schedule.password = "Administrator";
        schedule.eventId = "testEvent";
        schedule.eventCategory = "default";
        Calendar now = Calendar.getInstance();
        int second = now.get(Calendar.SECOND);
        int minute= now.get(Calendar.MINUTE);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int dayOfMonth= now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH) + 1;
        int year = now.get(Calendar.YEAR);
        String cronExpression = String.format("%s %s %s %s %s ? %s", second+1, minute, hour, dayOfMonth, month, year);
        schedule.cronExpression = cronExpression;
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put("flag", "1");
        service.registerSchedule(schedule, parameters);
        try {
            Thread.sleep(2000); // 1s so that job is called at least once
        } catch (InterruptedException e) {
            log.error(e);
            fail("Timer failed");
        }
        Integer count = whiteboard.getCount();
        log.info("count " + count);
        assertTrue("count " + count, count < 0);
        Boolean unregistered = service.unregisterSchedule(schedule.id);
        // schedule should happen only one time, it has already been unregistered
        assertFalse(unregistered);
    }
}
