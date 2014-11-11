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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
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
import org.junit.Ignore;
import org.junit.Test;
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
        assertNull(qd.queuing);
        assertNull(qd.processing);
        assertEquals("Sleep Work Queue", qd.name);
        assertEquals(2, qd.maxThreads);
        assertFalse(qd.usePriority);
        assertEquals(1234, qd.clearCompletedAfterSeconds);
        assertEquals(Collections.singleton("SleepWork"), qd.categories);
    }

    @Test
    public void testWorkManagerWork() throws Exception {
        int duration = 2000; // 2s
        SleepWork work = new SleepWork(duration, false);
        service.schedule(work);

        Thread.sleep(duration / 2);
        assertEquals(RUNNING, service.getWorkState(work.getId()));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(1, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));

        Thread.sleep(duration);
        assertEquals(1, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(0, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(COMPLETED, service.getWorkState(work.getId()));

        assertTrue(work.getSchedulingTime() != 0);
//        assertTrue(work.getStartTime() != 0);
//        assertTrue(work.getCompletionTime() != 0);
//        assertTrue(work.getCompletionTime() - work.getStartTime() > 0);
    }

    @Test
    public void testWorkManagerScheduling() throws Exception {
        int duration = 5000; // 2s
        SleepWork work1 = new SleepWork(duration, false, "1");
        SleepWork work2 = new SleepWork(duration, false, "2");
        SleepWork work3 = new SleepWork(duration, false, "3");
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);

        Thread.sleep(duration / 2);
        assertEquals(RUNNING, service.getWorkState("1"));
        assertEquals(RUNNING, service.getWorkState("2"));
        assertEquals(SCHEDULED, service.getWorkState("3"));
        assertEquals(Arrays.asList("3"), service.listWorkIds(QUEUE, SCHEDULED));
        assertSetEquals(Arrays.asList("1", "2"),
                service.listWorkIds(QUEUE, RUNNING));
        assertSetEquals(Arrays.asList("1", "2", "3"),
                service.listWorkIds(QUEUE, null));
        assertEquals(Collections.emptyList(),
                service.listWorkIds(QUEUE, COMPLETED));

        // disabled IF_NOT_* features
        if (Boolean.FALSE.booleanValue()) {
        SleepWork work4 = new SleepWork(duration, false, "3"); // id=3
        service.schedule(work4, Scheduling.IF_NOT_SCHEDULED);
        assertEquals(CANCELED, work4.getWorkInstanceState());

        SleepWork work5 = new SleepWork(duration, false, "1"); // id=1
        service.schedule(work5, Scheduling.IF_NOT_RUNNING);
        assertEquals(CANCELED, work5.getWorkInstanceState());

        SleepWork work6 = new SleepWork(duration, false, "1"); // id=1
        service.schedule(work6, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
        assertEquals(CANCELED, work6.getWorkInstanceState());
        }

        SleepWork work7 = new SleepWork(duration, false, "3"); // id=3
        service.schedule(work7, Scheduling.CANCEL_SCHEDULED);
        assertEquals(SCHEDULED, work7.getWorkInstanceState());

        boolean completed = service.awaitCompletion(duration * 2,
                TimeUnit.MILLISECONDS);
        assertTrue(completed);

        assertEquals(COMPLETED, service.getWorkState("1"));
        assertEquals(COMPLETED, service.getWorkState("2"));
        assertEquals(COMPLETED, service.getWorkState("3"));
        assertEquals(Collections.emptyList(),
                service.listWorkIds(QUEUE, SCHEDULED));
        assertEquals(Collections.emptyList(),
                service.listWorkIds(QUEUE, RUNNING));
        assertEquals(Collections.emptyList(), service.listWorkIds(QUEUE, null));
        assertSetEquals(Arrays.asList("1", "2", "3"),
                service.listWorkIds(QUEUE, COMPLETED));
    }

    @Test
    @Ignore
    public void testWorkManagerShutdown() throws Exception {
        int duration = 2000; // 2s
        SleepWork work1 = new SleepWork(duration, false, "1");
        SleepWork work2 = new SleepWork(duration, false, "2");
        SleepWork work3 = new SleepWork(duration, false, "3");
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);

        Thread.sleep(duration / 2);
        assertEquals(RUNNING, service.getWorkState("1"));
        assertEquals(RUNNING, service.getWorkState("2"));
        assertEquals(SCHEDULED, service.getWorkState("3"));

        // shutdown workmanager service
        // work1 and work2 get a suspended notice and stop
        // work3 then gets scheduled immediately
        // and is either discarded (memory)
        // or put in the suspended queue (persistent)

        dontClearCompletedWork = true;
        boolean terminated = service.shutdown(duration * 2,
                TimeUnit.MILLISECONDS);
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

    @Test
    public void testWorkManagerDisableProcessing() throws Exception {
        assumeTrue(persistent());

        // disable SleepWork queue
        deployContrib("org.nuxeo.ecm.core.event.test",
                "test-workmanager-disablequeue.xml");

        int duration = 2000; // 2s
        SleepWork work1 = new SleepWork(duration, false);
        service.schedule(work1);

        Thread.sleep(duration / 2);

        // stays scheduled
        assertEquals(1, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));

        Thread.sleep(2 * duration);
        // still scheduled
        assertEquals(1, service.getQueueSize(QUEUE, SCHEDULED));

        // now reactivate the queue
        // use a programmatic work queue descriptor
        WorkQueueDescriptor descr = new WorkQueueDescriptor();
        descr.id = "SleepWork";
        descr.processing = Boolean.TRUE;
        descr.categories = Collections.emptySet();
        ((WorkManagerImpl) service).registerWorkQueueDescriptor(descr);

        Thread.sleep(duration / 2);
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(1, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        Thread.sleep(duration);
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(1, service.getQueueSize(QUEUE, COMPLETED));
    }

    @Test
    public void testWorkManagerDisableProcessing2() throws Exception {
        assumeTrue(persistent());

        // disable all queues
        deployContrib("org.nuxeo.ecm.core.event.test",
                "test-workmanager-disablequeue2.xml");
        int duration = 2000; // 2s
        SleepWork work1 = new SleepWork(duration, false);
        service.schedule(work1);

        Thread.sleep(duration / 2);

        // stays scheduled
        assertEquals(1, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));

        // check that we can reenable the queue
        Thread.sleep(2 * duration);
        // still scheduled
        assertEquals(1, service.getQueueSize(QUEUE, SCHEDULED));

        // now reactivate the queue
        // use a programmatic work queue descriptor
        WorkQueueDescriptor descr = new WorkQueueDescriptor();
        descr.id = "SleepWork";
        descr.processing = Boolean.TRUE;
        descr.categories = Collections.emptySet();
        ((WorkManagerImpl) service).registerWorkQueueDescriptor(descr);

        Thread.sleep(duration / 2);
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(1, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        Thread.sleep(duration);
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(1, service.getQueueSize(QUEUE, COMPLETED));
    }

}
