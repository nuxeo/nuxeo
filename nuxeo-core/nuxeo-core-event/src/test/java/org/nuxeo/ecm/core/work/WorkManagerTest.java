/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.work.api.Work.State.CANCELED;
import static org.nuxeo.ecm.core.work.api.Work.State.COMPLETED;
import static org.nuxeo.ecm.core.work.api.Work.State.FAILED;
import static org.nuxeo.ecm.core.work.api.Work.State.RUNNING;
import static org.nuxeo.ecm.core.work.api.Work.State.SCHEDULED;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.common.logging.SequenceTracer;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.FileEventsTrackingFeature;
import org.nuxeo.runtime.trackers.files.FileEvent;

@Features(FileEventsTrackingFeature.class)
public class WorkManagerTest extends NXRuntimeTestCase {

    protected static class CreateFile extends AbstractWork implements Serializable {
        private final File file;

        private static final long serialVersionUID = 1L;

        protected CreateFile(File file) {
            this.file = file;
        }

        @Override
        public String getTitle() {
            return "pfouh";
        }

        @Override
        public void work() {
            FileEvent.onFile(this, file, this).send();
            SequenceTracer.mark("send event");
        }
    }

    protected static class SleepAndFailWork extends SleepWork {
        private static final long serialVersionUID = 1L;

        public SleepAndFailWork(long durationMillis, boolean debug, String id) {
            super(durationMillis, debug, id);
        }

        @Override
        public void work() {
            super.work();
            throw new RuntimeException(getTitle());
        }
    }

    protected static final String CATEGORY = "SleepWork";

    protected static final String QUEUE = "SleepWork";

    protected WorkManagerImpl service;

    protected boolean dontClearCompletedWork;

    private static void assertSetEquals(List<String> expected, List<String> actual) {
        assertEquals(new HashSet<String>(expected), new HashSet<String>(actual));
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
        service = (WorkManagerImpl) Framework.getLocalService(WorkManager.class);
    }

    protected void doDeploy() throws Exception {
        deployContrib("org.nuxeo.ecm.core.event.test", "test-workmanager-config.xml");
    }

    protected void deployAndStart() throws Exception {
        doDeploy();
        fireFrameworkStarted();
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
    public void testBasics() throws Exception {
        deployAndStart();

        assertNotNull(service);
        service.clearCompletedWork(0);
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        assertEquals(0, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
    }

    @Test
    public void testWorkManagerConfig() throws Exception {
        deployAndStart();

        SleepWork work = new SleepWork(1);
        assertEquals(CATEGORY, work.getCategory());
        assertEquals(QUEUE, service.getCategoryQueueId(CATEGORY));
        WorkQueueDescriptor qd = service.getWorkQueueDescriptor(QUEUE);
        assertEquals("SleepWork", qd.id);
        assertEquals("Sleep Work Queue", qd.name);
        assertEquals(2, qd.getMaxThreads());
        assertEquals(1234, qd.getClearCompletedAfterSeconds());
        assertEquals(Collections.singleton("SleepWork"), qd.categories);
    }

    @Test
    public void testWorkManagerWork() throws Exception {
        deployAndStart();

        int duration = 3000; // ms
        SleepWork work = new SleepWork(duration, false);
        service.schedule(work);

        Thread.sleep(duration / 3);
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
        // assertTrue(work.getStartTime() != 0);
        // assertTrue(work.getCompletionTime() != 0);
        // assertTrue(work.getCompletionTime() - work.getStartTime() > 0);
    }

    @Test
    public void testWorkManagerScheduling() throws Exception {
        deployAndStart();

        assertEquals(Collections.emptyList(), service.listWorkIds(QUEUE, COMPLETED));
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
        assertSetEquals(Arrays.asList("1", "2"), service.listWorkIds(QUEUE, RUNNING));
        assertSetEquals(Arrays.asList("1", "2", "3"), service.listWorkIds(QUEUE, null));
        assertEquals(Collections.emptyList(), service.listWorkIds(QUEUE, COMPLETED));

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

        SleepAndFailWork work8 = new SleepAndFailWork(0, false, "4");
        service.schedule(work8);

        boolean completed = service.awaitCompletion(duration * 2, TimeUnit.MILLISECONDS);
        assertTrue(completed);

        assertEquals(COMPLETED, service.getWorkState("1"));
        assertEquals(COMPLETED, service.getWorkState("2"));
        assertEquals(COMPLETED, service.getWorkState("3"));
        assertEquals(COMPLETED, service.getWorkState("4"));
        assertEquals(FAILED, service.find("4", COMPLETED).getWorkInstanceState());
        assertEquals(Collections.emptyList(), service.listWorkIds(QUEUE, SCHEDULED));
        assertEquals(Collections.emptyList(), service.listWorkIds(QUEUE, RUNNING));
        assertEquals(Collections.emptyList(), service.listWorkIds(QUEUE, null));
        assertSetEquals(Arrays.asList("1", "2", "3", "4"), service.listWorkIds(QUEUE, COMPLETED));
    }

    @Test
    @Ignore
    public void testWorkManagerShutdown() throws Exception {
        deployAndStart();

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
        boolean terminated = service.shutdown(duration * 2, TimeUnit.MILLISECONDS);
        assertTrue(terminated);

        // check work state
        assertEquals(SCHEDULED, work1.getWorkInstanceState());
        assertEquals(SCHEDULED, work2.getWorkInstanceState());
        assertEquals(persistent() ? SCHEDULED : CANCELED, work3.getWorkInstanceState());
        long remaining1 = work1.durationMillis;
        long remaining2 = work2.durationMillis;
        long remaining3 = work3.durationMillis;
        assertTrue("remaining1 " + remaining1, remaining1 < duration);
        assertTrue("remaining2 " + remaining2, remaining2 < duration);
        assertEquals(duration, remaining3);
    }

    @Test
    public void testWorkManagerConfigDisableOneBeforeStart() throws Exception {
        doDeploy();
        // before first applicationStarted:
        // disable SleepWork queue
        deployContrib("org.nuxeo.ecm.core.event.test", "test-workmanager-disablequeue.xml");
        fireFrameworkStarted();

        assertTrue(service.isProcessingEnabled("default"));
        assertFalse(service.isProcessingEnabled("SleepWork"));
    }

    @Test
    public void testWorkManagerConfigDisableOneAfterStart() throws Exception {
        doDeploy();
        fireFrameworkStarted();
        // after first applicationStarted:
        // disable SleepWork queue
        deployContrib("org.nuxeo.ecm.core.event.test", "test-workmanager-disablequeue.xml");

        assertTrue(service.isProcessingEnabled("default"));
        assertFalse(service.isProcessingEnabled("SleepWork"));
    }

    @Test
    public void testWorkManagerConfigDisableAllBeforeStart() throws Exception {
        doDeploy();
        // before first applicationStarted:
        // disable * then enable SleepWork queue
        deployContrib("org.nuxeo.ecm.core.event.test", "test-workmanager-disablequeue1.xml");
        fireFrameworkStarted();

        assertFalse(service.isProcessingEnabled("default"));
        assertTrue(service.isProcessingEnabled("SleepWork"));
    }

    @Test
    public void testWorkManagerConfigDisableAllAfterStart() throws Exception {
        doDeploy();
        fireFrameworkStarted();
        // after first applicationStarted:
        // disable * then enable SleepWork queue
        deployContrib("org.nuxeo.ecm.core.event.test", "test-workmanager-disablequeue1.xml");

        assertFalse(service.isProcessingEnabled("default"));
        assertTrue(service.isProcessingEnabled("SleepWork"));
    }

    @Ignore("NXP-15680")
    @Test
    public void testWorkManagerDisableProcessing() throws Exception {
        deployAndStart();
        assumeTrue(persistent());

        // disable SleepWork queue
        deployContrib("org.nuxeo.ecm.core.event.test", "test-workmanager-disablequeue.xml");

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
        ((WorkManagerImpl) service).activateQueue(descr);

        Thread.sleep(duration / 2);
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(1, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        Thread.sleep(duration);
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(1, service.getQueueSize(QUEUE, COMPLETED));
    }

    @Ignore("NXP-15680")
    @Test
    public void testWorkManagerDisableProcessing2() throws Exception {
        deployAndStart();
        assumeTrue(persistent());

        // disable all queues
        deployContrib("org.nuxeo.ecm.core.event.test", "test-workmanager-disablequeue2.xml");
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
        ((WorkManagerImpl) service).activateQueue(descr);

        Thread.sleep(duration / 2);
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(1, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
        Thread.sleep(duration);
        assertEquals(0, service.getQueueSize(QUEUE, SCHEDULED));
        assertEquals(0, service.getQueueSize(QUEUE, RUNNING));
        assertEquals(1, service.getQueueSize(QUEUE, COMPLETED));
    }

    @Inject
    public FeaturesRunner runner;

    protected FileEventsTrackingFeature feature;

    @Before
    public void injectFeature() {
        feature = runner.getFeature(FileEventsTrackingFeature.class);
    }

    @Test
    public void transientFilesWorkAreCleaned() throws Exception {
        deployAndStart();

        final File file = feature.resolveAndCreate(new File("pfouh"));
        service.schedule(new CreateFile(file));
        service.awaitCompletion(5, TimeUnit.SECONDS);
    }

    @Test
    public void testClearCompleted() throws Exception {
        deployAndStart();

        int N = 20;
        int duration = 100; // ms
        for (int i = 0; i < N; i++) {
            SleepWork work = new SleepWork(duration, false);
            service.schedule(work);
        }
        Thread.sleep(duration * 2 * N);
        assertEquals(N, service.getQueueSize(QUEUE, COMPLETED));

        service.clearCompletedWork(0); // all
        assertEquals(0, service.getQueueSize(QUEUE, COMPLETED));
    }

    @Test
    public void testClearCompletedBefore() throws Exception {
        deployAndStart();

        int duration = 1000; // ms
        SleepWork work = new SleepWork(duration, false);
        service.schedule(work);
        Thread.sleep(duration * 2);
        long cutoff = System.currentTimeMillis();
        Thread.sleep(duration);
        work = new SleepWork(duration, false);
        service.schedule(work);
        Thread.sleep(duration * 2);
        assertEquals(2, service.getQueueSize(QUEUE, COMPLETED));

        service.clearCompletedWork(cutoff);
        assertEquals(1, service.getQueueSize(QUEUE, COMPLETED));
    }

}
