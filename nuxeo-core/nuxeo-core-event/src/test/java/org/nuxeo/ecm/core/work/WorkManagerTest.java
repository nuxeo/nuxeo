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
import static org.nuxeo.ecm.core.work.api.Work.State.CANCELED;
import static org.nuxeo.ecm.core.work.api.Work.State.COMPLETED;
import static org.nuxeo.ecm.core.work.api.Work.State.RUNNING;
import static org.nuxeo.ecm.core.work.api.Work.State.SCHEDULED;
import static org.nuxeo.ecm.core.work.api.Work.State.SUSPENDED;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.work.api.Work;
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
        assertEquals(0, service.listWork(QUEUE, COMPLETED).size());
        assertEquals(0, service.listWork(QUEUE, RUNNING).size());
        assertEquals(0, service.listWork(QUEUE, SCHEDULED).size());
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
        assertEquals("Sleep 5000 ms", work.getTitle());
        assertEquals(CATEGORY, work.getCategory());
        assertEquals(SCHEDULED, work.getState());
        work.suspend();
        assertEquals(SUSPENDED, work.getState());
        Map<String, Serializable> data = work.getData();
        assertNotNull(data);
        long remaining = ((Long) data.get(SleepWork.STATE_DURATION)).longValue();
        assertEquals(duration, remaining);
    }

    @Test
    public void testWorkSuspendFromThread() throws Exception {
        long duration = 5000; // 5s
        SleepWork work = new SleepWork(duration, true);
        assertEquals(SCHEDULED, work.getState());

        // start work in thread
        Thread t = new Thread(work);
        t.start();
        try {
            work.beforeRun();
            work.debugWaitReady();
            assertEquals(RUNNING, work.getState());
            work.debugStart();

            Thread.sleep(50);

            // suspend work

            work.suspend();
            boolean terminated = work.awaitTermination(1, TimeUnit.SECONDS);
            assertTrue(terminated);
            assertEquals(SUSPENDED, work.getState());

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
        assertEquals(SCHEDULED, work.getState());
        service.schedule(work);

        work.debugWaitReady();
        assertEquals(RUNNING, work.getState());
        assertEquals(0, service.listWork(QUEUE, COMPLETED).size());
        assertEquals(1, service.listWork(QUEUE, RUNNING).size());
        assertEquals(0, service.listWork(QUEUE, SCHEDULED).size());
        assertEquals("Starting sleep work", work.getStatus());
        assertEquals(Progress.PROGRESS_0_PC, work.getProgress());
        work.debugStart();

        for (int i = 0; i < 20; i++) {
            // System.out.println(work.getStatus() + ": " + work.getProgress());
            Thread.sleep(100);
        }

        work.debugWaitDone();
        assertEquals(0, service.listWork(QUEUE, COMPLETED).size());
        assertEquals(1, service.listWork(QUEUE, RUNNING).size());
        assertEquals(0, service.listWork(QUEUE, SCHEDULED).size());
        assertEquals("Completed sleep work", work.getStatus());
        assertEquals(Progress.PROGRESS_100_PC, work.getProgress());
        work.debugFinish();

        Thread.sleep(1000);
        assertEquals(1, service.listWork(QUEUE, COMPLETED).size());
        assertEquals(0, service.listWork(QUEUE, RUNNING).size());
        assertEquals(0, service.listWork(QUEUE, SCHEDULED).size());
        assertEquals(COMPLETED, work.getState());
    }

    protected static class SleepWorkWithEquals extends SleepWork implements
            Comparable<SleepWorkWithEquals> {

        protected final String identity;

        public SleepWorkWithEquals(long durationMillis, boolean debug,
                String identity) {
            super(durationMillis, debug);
            this.identity = identity;
        }

        public SleepWorkWithEquals(long durationMillis, String category,
                boolean debug, String identity) {
            super(durationMillis, category, debug);
            this.identity = identity;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof SleepWorkWithEquals)) {
                return false;
            }
            return identity.equals(((SleepWorkWithEquals) other).identity);
        }

        @Override
        public int hashCode() {
            return identity.hashCode();
        }

        @Override
        public int compareTo(SleepWorkWithEquals o) {
            return identity.compareTo(o.identity);
        }
    }

    @Test
    public void testWorkManagerScheduling() throws Exception {
        int duration = 1000; // 1s
        SleepWork work1 = new SleepWorkWithEquals(duration, true, "1");
        SleepWork work2 = new SleepWorkWithEquals(duration, true, "2");
        SleepWork work3 = new SleepWorkWithEquals(duration, true, "3");
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);

        work1.debugWaitReady();
        work2.debugWaitReady();
        assertEquals(RUNNING, work1.getState());
        assertEquals(RUNNING, work2.getState());
        assertEquals(SCHEDULED, work3.getState());
        assertTrue(work1 == service.find(work1, RUNNING, false, null));
        assertTrue(work2 == service.find(work2, RUNNING, false, null));
        assertTrue(work3 == service.find(work3, SCHEDULED, false, null));
        assertTrue(work1 == service.find(work1, null, false, null));
        assertTrue(work2 == service.find(work2, null, false, null));
        assertTrue(work3 == service.find(work3, null, false, null));

        SleepWork work4 = new SleepWorkWithEquals(duration, true, "3"); // id=3
        service.schedule(work4, Scheduling.IF_NOT_SCHEDULED);
        assertEquals(CANCELED, work4.getState());

        SleepWork work5 = new SleepWorkWithEquals(duration, true, "1"); // id=1
        service.schedule(work5, Scheduling.IF_NOT_RUNNING);
        assertEquals(CANCELED, work5.getState());

        SleepWork work6 = new SleepWorkWithEquals(duration, true, "1"); // id=1
        service.schedule(work6, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
        assertEquals(CANCELED, work6.getState());

        SleepWork work7 = new SleepWorkWithEquals(duration, true, "3"); // id=3
        service.schedule(work7, Scheduling.CANCEL_SCHEDULED);
        assertEquals(CANCELED, work3.getState());
        assertEquals(SCHEDULED, work7.getState());

        work1.debugStart();
        work2.debugStart();
        work7.debugStart();
        work1.debugFinish(); // early
        work2.debugFinish(); // early
        work7.debugFinish(); // early

        boolean completed = service.awaitCompletion(3, TimeUnit.SECONDS);
        assertTrue(completed);

        assertEquals(COMPLETED, work1.getState());
        assertEquals(COMPLETED, work2.getState());
        assertEquals(COMPLETED, work7.getState());
        assertTrue(work1 == service.find(work1, COMPLETED, false, null));
        assertTrue(work2 == service.find(work2, COMPLETED, false, null));
        assertTrue(work7 == service.find(work7, COMPLETED, false, null));
    }

    @Test
    public void testWorkManagerPriority() throws Exception {
        int duration = 1000; // 1s
        Work work1 = new SleepWorkWithEquals(duration, "PrioritizedSleepWork",
                false, "1");
        Work work2 = new SleepWorkWithEquals(duration, "PrioritizedSleepWork",
                false, "2");
        Work work3 = new SleepWorkWithEquals(duration, "PrioritizedSleepWork",
                false, "3");
        Work work4 = new SleepWorkWithEquals(duration, "PrioritizedSleepWork",
                false, "4");
        service.schedule(work1); // 1 is immediately started
        service.schedule(work4); // schedule in reverse order
        service.schedule(work3); // but priority will reorder them
        service.schedule(work2);

        service.awaitCompletion("PrioritizedSleepWork", 5, TimeUnit.SECONDS);
        List<Work> list = service.listWork("PrioritizedSleepWork", COMPLETED);
        assertEquals(4, list.size());
        // check that execution was done in priority order, not scheduling order
        assertEquals("1", ((SleepWorkWithEquals) list.get(0)).identity);
        assertEquals("2", ((SleepWorkWithEquals) list.get(1)).identity);
        assertEquals("3", ((SleepWorkWithEquals) list.get(2)).identity);
        assertEquals("4", ((SleepWorkWithEquals) list.get(3)).identity);
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
        assertEquals(RUNNING, work1.getState());
        assertEquals(RUNNING, work2.getState());
        assertEquals(SCHEDULED, work3.getState());
        work1.debugStart();
        work2.debugStart();
        work3.debugStart();
        work1.debugFinish(); // early
        work2.debugFinish(); // early
        work3.debugFinish(); // early

        boolean completed = service.awaitCompletion(5, TimeUnit.SECONDS);
        assertTrue(completed);

        // check work state
        assertEquals(COMPLETED, work1.getState());
        assertEquals(COMPLETED, work2.getState());
        assertEquals(COMPLETED, work3.getState());
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
        assertEquals(RUNNING, work1.getState());
        assertEquals(RUNNING, work2.getState());
        assertEquals(SCHEDULED, work3.getState());
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
        assertEquals(SUSPENDED, work1.getState());
        assertEquals(SUSPENDED, work2.getState());
        assertEquals(SUSPENDED, work3.getState());
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
