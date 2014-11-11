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
package org.nuxeo.ecm.core.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.work.api.Work.State.CANCELED;
import static org.nuxeo.ecm.core.work.api.Work.State.COMPLETED;
import static org.nuxeo.ecm.core.work.api.Work.State.RUNNING;
import static org.nuxeo.ecm.core.work.api.Work.State.SCHEDULED;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.work.api.Work.Progress;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class WorkManagerTest extends NXRuntimeTestCase {

    protected static final String CATEGORY = "SleepWork";

    protected static final String QUEUE = "SleepWork";

    protected WorkManager service;

    protected boolean dontClearCompletedWork;

    private static void assertSetEquals(List<String> expected,
            List<String> actual) {
        assertEquals(new HashSet<String>(expected), new HashSet<String>(actual));
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        doDeploy();
        service = Framework.getLocalService(WorkManager.class);
    }

    protected void doDeploy() throws Exception {
        deployBundle("org.nuxeo.ecm.core.event");
        deployContrib("org.nuxeo.ecm.core.event.test",
                "test-workmanager-config.xml");
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (service != null && !dontClearCompletedWork) {
            service.clearCompletedWork(0);
        }
        super.tearDown();
    }

    // overridden for persistence
    public boolean persistent() {
        return false; // in-memory, no persistence
    }

    @Test
    public void testBasics() {
        assertNotNull(service);
        service.clearCompletedWork(0);
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(0, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
    }

    @Test
    public void testWorkSuspendFromThread() throws Exception {
        long duration = 5000; // 5s
        SleepWork work = new SleepWork(duration, true);
        assertEquals(duration, work.durationMillis);

        // start work in thread
        Thread t = new Thread(new WorkHolder(work));
        t.start();
        try {
            work.debugWaitReady();
            work.debugStart();

            Thread.sleep(50);

            // suspend work manually
            work.setWorkInstanceSuspending(); // manual
            long delay = 1000; // 1s
            long t0 = System.currentTimeMillis();
            for (;;) {
                if (work.isWorkInstanceSuspended()) { // manual
                    break;
                }
                if (System.currentTimeMillis() - t0 > delay) {
                    fail("took too long");
                }
                Thread.sleep(50);
            }
            work.debugFinish();
        } finally {
            t.join();
        }

        assertTrue("remaining " + work.durationMillis,
                work.durationMillis < duration);
    }

    @Test
    public void testWorkManagerConfig() throws Exception {
        SleepWork work = new SleepWork(1);
        assertEquals(CATEGORY, work.getCategory());
        assertEquals(QUEUE, service.getCategoryQueueId(CATEGORY));
        WorkQueueDescriptor qd = service.getWorkQueueDescriptor(QUEUE);
        assertEquals("SleepWork", qd.id);
        assertEquals("Sleep Work Queue", qd.name);
        assertEquals(2, qd.maxThreads);
        assertFalse(qd.usePriority);
        assertEquals(1234, qd.clearCompletedAfterSeconds);
        assertEquals(Collections.singleton("SleepWork"), qd.categories);
    }

    @Test
    public void testWorkManagerWork() throws Exception {
        int duration = 2000; // 2s
        SleepWork work = new SleepWork(duration, true);
        service.schedule(work);

        work.debugWaitReady();
        assertEquals(RUNNING, work.getWorkInstanceState());
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(1, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals("Starting sleep work", work.getStatus());
        assertEquals(Progress.PROGRESS_0_PC, work.getProgress());
        work.debugStart();

        for (int i = 0; i < 20; i++) {
            // System.out.println(work.getStatus() + ": " + work.getProgress());
            Thread.sleep(100);
        }

        work.debugWaitDone();
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(1, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals("Completed sleep work", work.getStatus());
        assertEquals(Progress.PROGRESS_100_PC, work.getProgress());
        work.debugFinish();

        Thread.sleep(1000);
        assertEquals(1, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(0, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(COMPLETED, work.getWorkInstanceState());
    }

    @Test
    public void testWorkManagerScheduling() throws Exception {
        int duration = 1000; // 1s
        SleepWork work1 = new SleepWork(duration, true, "1");
        SleepWork work2 = new SleepWork(duration, true, "2");
        SleepWork work3 = new SleepWork(duration, true, "3");
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);

        work1.debugWaitReady();
        work2.debugWaitReady();
        assertEquals(RUNNING, work1.getWorkInstanceState());
        assertEquals(RUNNING, work2.getWorkInstanceState());
        assertEquals(SCHEDULED, work3.getWorkInstanceState());
        assertEquals(RUNNING, service.getWorkState("1"));
        assertEquals(RUNNING, service.getWorkState("2"));
        assertEquals(SCHEDULED, service.getWorkState("3"));
        // assertTrue(work1 == service.find(work1, RUNNING, false, null));
        // assertTrue(work2 == service.find(work2, RUNNING, false, null));
        // assertTrue(work3 == service.find(work3, SCHEDULED, false, null));
        // assertTrue(work1 == service.find(work1, null, false, null));
        // assertTrue(work2 == service.find(work2, null, false, null));
        // assertTrue(work3 == service.find(work3, null, false, null));
        assertEquals(Arrays.asList("3"), service.listWorkIds(QUEUE, SCHEDULED));
        assertSetEquals(Arrays.asList("1", "2"),
                service.listWorkIds(QUEUE, RUNNING));
        assertSetEquals(Arrays.asList("1", "2", "3"),
                service.listWorkIds(QUEUE, null));
        assertEquals(Collections.emptyList(),
                service.listWorkIds(QUEUE, COMPLETED));

        // disabled IF_NOT_* features
        if (Boolean.FALSE.booleanValue()) {
        SleepWork work4 = new SleepWork(duration, true, "3"); // id=3
        service.schedule(work4, Scheduling.IF_NOT_SCHEDULED);
        assertEquals(CANCELED, work4.getWorkInstanceState());

        SleepWork work5 = new SleepWork(duration, true, "1"); // id=1
        service.schedule(work5, Scheduling.IF_NOT_RUNNING);
        assertEquals(CANCELED, work5.getWorkInstanceState());

        SleepWork work6 = new SleepWork(duration, true, "1"); // id=1
        service.schedule(work6, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
        assertEquals(CANCELED, work6.getWorkInstanceState());
        }

        SleepWork work7 = new SleepWork(duration, true, "3"); // id=3
        service.schedule(work7, Scheduling.CANCEL_SCHEDULED);
        // assertEquals(CANCELED, work3.getState()); // not for redis
        assertEquals(SCHEDULED, work7.getWorkInstanceState());

        work1.debugStart();
        work2.debugStart();
        work7.debugStart();
        work1.debugFinish(); // early
        work2.debugFinish(); // early
        work7.debugFinish(); // early

        boolean completed = service.awaitCompletion(3, TimeUnit.SECONDS);
        assertTrue(completed);

        assertEquals(COMPLETED, work1.getWorkInstanceState());
        assertEquals(COMPLETED, work2.getWorkInstanceState());
        // assertEquals(COMPLETED, work7.getState()); // not for redis
        assertEquals(COMPLETED, service.getWorkState("1"));
        assertEquals(COMPLETED, service.getWorkState("2"));
        assertEquals(COMPLETED, service.getWorkState("3"));
        // assertTrue(work1 == service.find(work1, COMPLETED, false, null));
        // assertTrue(work2 == service.find(work2, COMPLETED, false, null));
        // assertTrue(work7 == service.find(work7, COMPLETED, false, null));
        assertEquals(Collections.emptyList(),
                service.listWorkIds(QUEUE, SCHEDULED));
        assertEquals(Collections.emptyList(),
                service.listWorkIds(QUEUE, RUNNING));
        assertEquals(Collections.emptyList(),
                service.listWorkIds(QUEUE, null));
        assertSetEquals(Arrays.asList("1", "2", "3"),
                service.listWorkIds(QUEUE, COMPLETED));
    }

    @Test
    public void testWorkManagerWorkCompletion() throws Exception {
        int duration = 2000; // 2s
        SleepWork work1 = new SleepWork(duration, true);
        SleepWork work2 = new SleepWork(duration, true);
        SleepWork work3 = new SleepWork(duration, true);
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);

        work1.debugWaitReady();
        work2.debugWaitReady();
        assertEquals(RUNNING, work1.getWorkInstanceState());
        assertEquals(RUNNING, work2.getWorkInstanceState());
        assertEquals(SCHEDULED, work3.getWorkInstanceState());
        work1.debugStart();
        work2.debugStart();
        work3.debugStart();
        work1.debugFinish(); // early
        work2.debugFinish(); // early
        work3.debugFinish(); // early

        boolean completed = service.awaitCompletion(5, TimeUnit.SECONDS);
        assertTrue(completed);

        // check work state
        assertEquals(COMPLETED, work1.getWorkInstanceState());
        assertEquals(COMPLETED, work2.getWorkInstanceState());
        // assertEquals(COMPLETED, work3.getState()); // not for redis
    }

    @Test
    public void testWorkManagerShutdown() throws Exception {
        int duration = 5000; // 5s
        SleepWork work1 = new SleepWork(duration, true);
        SleepWork work2 = new SleepWork(duration, true);
        SleepWork work3 = new SleepWork(duration, true);
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);

        work1.debugWaitReady();
        work2.debugWaitReady();
        assertEquals(RUNNING, work1.getWorkInstanceState());
        assertEquals(RUNNING, work2.getWorkInstanceState());
        assertEquals(SCHEDULED, work3.getWorkInstanceState());
        work1.debugStart();
        work2.debugStart();
        work1.debugFinish(); // early
        work2.debugFinish(); // early

        Thread.sleep(50);

        // shutdown workmanager service
        // work1 and work2 get a suspended notice and stop
        // work3 then gets scheduled immediately
        // and is either discarded (memory)
        // or put in the suspended queue (persistent)

        dontClearCompletedWork = true;
        boolean terminated = service.shutdown(1, TimeUnit.SECONDS);
        assertTrue(terminated);

        // check work state
        assertEquals(SCHEDULED, work1.getWorkInstanceState());
        assertEquals(SCHEDULED, work2.getWorkInstanceState());
        assertEquals(persistent() ? SCHEDULED : CANCELED,
                work3.getWorkInstanceState());
        long remaining1 = work1.durationMillis;
        long remaining2 = work2.durationMillis;
        long remaining3 = work3.durationMillis;
        assertTrue("remaining1 " + remaining1, remaining1 < duration);
        assertTrue("remaining2 " + remaining2, remaining2 < duration);
        assertEquals(duration, remaining3);
    }

}
