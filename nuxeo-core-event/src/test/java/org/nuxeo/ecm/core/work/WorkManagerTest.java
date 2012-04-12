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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.work.api.Work.Progress;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class WorkManagerTest extends NXRuntimeTestCase {

    protected static final String CATEGORY = "SleepWork";

    protected static final String QUEUE = "SleepWork";

    protected WorkManager service;

    protected boolean dontClearCompletedWork;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
        deployContrib("org.nuxeo.ecm.core.event.test",
                "test-workmanager-config.xml");
        service = Framework.getLocalService(WorkManager.class);
        assertNotNull(service);
        service.clearCompletedWork(0);
        assertEquals(0, service.getCompletedWork(QUEUE).size());
        assertEquals(0, service.getRunningWork(QUEUE).size());
        assertEquals(0, service.getScheduledWork(QUEUE).size());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (!dontClearCompletedWork) {
            service.clearCompletedWork(0);
        }
        super.tearDown();
    }

    @Test
    public void testWorkSuspendFromQueue() throws Exception {
        long duration = 5000; // 5s
        SleepWork work = new SleepWork(duration);
        assertEquals(CATEGORY, work.getCategory());
        assertEquals(State.QUEUED, work.getState());
        work.suspend();
        assertEquals(State.SUSPENDED, work.getState());
        Map<String, Serializable> data = work.getData();
        assertNotNull(data);
        long remaining = ((Long) data.get(SleepWork.STATE_DURATION)).longValue();
        assertEquals(duration, remaining);
    }

    @Test
    public void testWorkSuspendFromThread() throws Exception {
        long duration = 5000; // 5s
        SleepWork work = new SleepWork(duration, true);
        assertEquals(State.QUEUED, work.getState());

        // start work in thread
        Thread t = new Thread(work);
        t.start();
        try {
            work.beforeRun();
            work.debugWaitReady();
            assertEquals(State.RUNNING, work.getState());
            work.debugStart();

            Thread.sleep(50);

            // suspend work

            work.suspend();
            boolean terminated = work.awaitTermination(1, TimeUnit.SECONDS);
            assertTrue(terminated);
            assertEquals(State.SUSPENDED, work.getState());

            work.debugFinish();
        } finally {
            t.interrupt();
            t.join();
        }

        Map<String, Serializable> data = work.getData();
        assertNotNull(data);
        long remaining = ((Long) data.get(SleepWork.STATE_DURATION)).longValue();
        assertTrue("remaining " + remaining, remaining < duration);
    }

    @Test
    public void testWorkManagerConfig() throws Exception {
        SleepWork work = new SleepWork(1);
        assertEquals(CATEGORY, work.getCategory());
        assertEquals(QUEUE, service.getCategoryQueueId(CATEGORY));
        WorkQueueDescriptor qd = service.getWorkQueueDescriptor(QUEUE);
        assertEquals(2, qd.maxThreads);
    }

    @Test
    public void testWorkManagerWork() throws Exception {
        int duration = 2000; // 2s
        SleepWork work = new SleepWork(duration, true);
        assertEquals(State.QUEUED, work.getState());
        service.schedule(work);

        work.debugWaitReady();
        assertEquals(State.RUNNING, work.getState());
        assertEquals(0, service.getCompletedWork(QUEUE).size());
        assertEquals(1, service.getRunningWork(QUEUE).size());
        assertEquals(0, service.getScheduledWork(QUEUE).size());
        assertEquals("Starting sleep work", work.getStatus());
        assertEquals(Progress.PROGRESS_0_PC, work.getProgress());
        work.debugStart();

        for (int i = 0; i < 20; i++) {
            // System.out.println(work.getStatus() + ": " + work.getProgress());
            Thread.sleep(100);
        }

        work.debugWaitDone();
        assertEquals(0, service.getCompletedWork(QUEUE).size());
        assertEquals(1, service.getRunningWork(QUEUE).size());
        assertEquals(0, service.getScheduledWork(QUEUE).size());
        assertEquals("Completed sleep work", work.getStatus());
        assertEquals(Progress.PROGRESS_100_PC, work.getProgress());
        work.debugFinish();

        Thread.sleep(1000);
        assertEquals(1, service.getCompletedWork(QUEUE).size());
        assertEquals(0, service.getRunningWork(QUEUE).size());
        assertEquals(0, service.getScheduledWork(QUEUE).size());
        assertEquals(State.COMPLETED, work.getState());
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
        assertEquals(State.RUNNING, work1.getState());
        assertEquals(State.RUNNING, work2.getState());
        assertEquals(State.QUEUED, work3.getState());
        work1.debugStart();
        work2.debugStart();
        work3.debugStart();
        work1.debugFinish(); // early
        work2.debugFinish(); // early
        work3.debugFinish(); // early

        boolean completed = service.awaitCompletion(5, TimeUnit.SECONDS);
        assertTrue(completed);

        // check work state
        assertEquals(State.COMPLETED, work1.getState());
        assertEquals(State.COMPLETED, work2.getState());
        assertEquals(State.COMPLETED, work3.getState());
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
        assertEquals(State.RUNNING, work1.getState());
        assertEquals(State.RUNNING, work2.getState());
        assertEquals(State.QUEUED, work3.getState());
        work1.debugStart();
        work2.debugStart();
        work1.debugFinish(); // early
        work2.debugFinish(); // early

        Thread.sleep(50);

        // shutdown workmanager service

        dontClearCompletedWork = true;
        boolean terminated = service.shutdown(1, TimeUnit.SECONDS);
        assertTrue(terminated);

        // check work state
        assertEquals(State.SUSPENDED, work1.getState());
        assertEquals(State.SUSPENDED, work2.getState());
        assertEquals(State.SUSPENDED, work3.getState());
        Map<String, Serializable> data1 = work1.getData();
        Map<String, Serializable> data2 = work2.getData();
        Map<String, Serializable> data3 = work3.getData();
        assertNotNull(data1);
        assertNotNull(data2);
        assertNotNull(data3);
        long remaining1 = ((Long) data1.get(SleepWork.STATE_DURATION)).longValue();
        long remaining2 = ((Long) data2.get(SleepWork.STATE_DURATION)).longValue();
        long remaining3 = ((Long) data3.get(SleepWork.STATE_DURATION)).longValue();
        assertTrue("remaining1 " + remaining1, remaining1 < duration);
        assertTrue("remaining2 " + remaining2, remaining2 < duration);
        assertEquals(duration, remaining3);
    }

}
