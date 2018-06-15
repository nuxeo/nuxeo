/*
 * (C) Copyright 2012-2017 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.core.work.api.Work.State.RUNNING;
import static org.nuxeo.ecm.core.work.api.Work.State.SCHEDULED;
import static org.nuxeo.ecm.core.work.api.Work.State.UNKNOWN;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.logging.SequenceTracer;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.WorkFailureEventListener;
import org.nuxeo.ecm.core.event.test.DummyPostCommitEventListener;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.FileEventsTrackingFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.trackers.files.FileEvent;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, FileEventsTrackingFeature.class })
@Deploy({ "org.nuxeo.ecm.core.event", "org.nuxeo.ecm.core.event.test:test-workmanager-config.xml" })
public class WorkManagerTest {

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

        public SleepAndFailWork(long durationMillis, boolean debug) {
            super(durationMillis, debug);
        }

        public SleepAndFailWork(long durationMillis, boolean debug, String id) {
            super(durationMillis, debug, id);
        }

        @Override
        public void work() {
            super.work();
            throw new RuntimeException(getTitle());
        }
    }

    protected class MetricsTracker {
        protected String queueId;

        protected WorkQueueMetrics initialMetrics;

        protected MetricsTracker() {
            this(QUEUE);
        }

        protected MetricsTracker(String queueId) {
            this.queueId = queueId;
            this.initialMetrics = service.getMetrics(QUEUE);
        }

        public void assertDiff(long scheduled, long running, long completed, long canceled) {
            long actualScheduled = initialMetrics.getScheduled().longValue() + scheduled;
            long actualRunning = initialMetrics.getRunning().longValue() + running;
            long actualCompleted = initialMetrics.getCompleted().longValue() + completed;
            long actualCanceled = initialMetrics.getCanceled().longValue() + canceled;
            assertMetrics(actualScheduled, actualRunning, actualCompleted, actualCanceled);
        }
    }

    protected static final String CATEGORY = "SleepWork";

    protected static final String QUEUE = "SleepWork";

    protected WorkManagerImpl service;

    protected boolean dontClearCompletedWork;

    @Inject
    protected EventService eventService;

    @Inject
    public FeaturesRunner runner;

    @Inject
    protected RuntimeHarness harness;

    protected FileEventsTrackingFeature feature;

    @Before
    public void setUp() throws Exception {
        feature = runner.getFeature(FileEventsTrackingFeature.class);
        service = (WorkManagerImpl) Framework.getService(WorkManager.class);
    }

    void assertWorkIdsEquals(List<String> expected, Work.State state) {
        List<String> actual = service.listWorkIds(QUEUE, state);
        assertEquals(new HashSet<>(expected), new HashSet<>(actual));
    }

    void assertMetrics(long scheduled, long running, long completed, long cancelled) {
        assertEquals(new WorkQueueMetrics(QUEUE, scheduled, running, completed, cancelled), service.getMetrics(QUEUE));
    }

    // overridden for persistence
    public boolean persistent() {
        return false; // in-memory, no persistence
    }

    @Test
    public void testBasics() throws Exception {
        new MetricsTracker().assertDiff(0, 0, 0, 0);
        assertNotNull(service);
    }

    @Test
    public void testWorkManagerConfig() throws Exception {
        new MetricsTracker().assertDiff(0, 0, 0, 0);
        SleepWork work = new SleepWork(1);
        assertEquals(CATEGORY, work.getCategory());
        assertEquals(QUEUE, service.getCategoryQueueId(CATEGORY));

        WorkQueueDescriptor qd = service.getWorkQueueDescriptor(QUEUE);
        assertEquals("SleepWork", qd.id);
        assertEquals("Sleep Work Queue", qd.name);
        assertEquals(2, qd.getMaxThreads());
        assertEquals(Collections.singleton("SleepWork"), qd.categories);
    }

    @Test
    public void testWorkManagerWork() throws Exception {
        MetricsTracker tracker = new MetricsTracker();
        tracker.assertDiff(0, 0, 0, 0);
        int duration = 3000; // ms
        SleepWork work = new SleepWork(duration, false);
        service.schedule(work);

        assertTrue(work.getSchedulingTime() != 0);

        Thread.sleep(duration / 3);
        assertState(RUNNING, work);
        tracker.assertDiff(0, 1, 0, 0);

        Thread.sleep(duration);
        tracker.assertDiff(0, 0, 1, 0);

        // assertTrue(work.getStartTime() != 0);
        // assertTrue(work.getCompletionTime() != 0);
        // assertTrue(work.getCompletionTime() - work.getStartTime() > 0);
    }

    protected void assertState(Work.State state, SleepWork work) {
        assertEquals(state, service.getWorkState(work.getId()));
    }

    @Test
    public void testWorkManagerScheduling() throws Exception {
        MetricsTracker tracker = new MetricsTracker();
        tracker.assertDiff(0, 0, 0, 0);
        int duration = 5000; // 2s
        SleepWork work1 = new SleepWork(duration, false);
        SleepWork work2 = new SleepWork(duration, false);
        SleepWork work3 = new SleepWork(duration, false);
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);

        Thread.sleep(duration / 2);
        tracker.assertDiff(1, 2, 0, 0);
        assertState(RUNNING, work1);
        assertState(RUNNING, work2);
        assertState(SCHEDULED, work3);
        assertWorkIdsEquals(Collections.singletonList(work3.getId()), SCHEDULED);
        assertWorkIdsEquals(Arrays.asList(work1.getId(), work2.getId()), RUNNING);
        assertWorkIdsEquals(Arrays.asList(work1.getId(), work2.getId(), work3.getId()), null);

        // disabled IF_NOT_* features
        if (Boolean.FALSE.booleanValue()) {
            SleepWork work4 = new SleepWork(duration, false, work3.getId());
            service.schedule(work4, Scheduling.IF_NOT_SCHEDULED);
            assertState(UNKNOWN, work4);

            SleepWork work5 = new SleepWork(duration, false, work1.getId());
            service.schedule(work5, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
            assertState(UNKNOWN, work5);
        }

        SleepWork work7 = new SleepWork(duration, false, work3.getId());
        service.schedule(work7, Scheduling.CANCEL_SCHEDULED);
        assertState(SCHEDULED, work7);
        tracker.assertDiff(1, 2, 0, 1);

        SleepAndFailWork work8 = new SleepAndFailWork(0, false);
        service.schedule(work8);
        tracker.assertDiff(2, 2, 0, 1);

        assertTrue(service.awaitCompletion(duration * 3, TimeUnit.MILLISECONDS));
        tracker.assertDiff(0, 0, 4, 1);

        assertWorkIdsEquals(Collections.emptyList(), SCHEDULED);
        assertWorkIdsEquals(Collections.emptyList(), RUNNING);
        assertWorkIdsEquals(Collections.emptyList(), null);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.event:test-work-failure-listeners.xml")
    public void itCanFireWorkFailureEvent() throws Exception {
        int initCount = DummyPostCommitEventListener.handledCount();
        int initEvtCount = DummyPostCommitEventListener.eventCount();
        int initSyncEvntCount = WorkFailureEventListener.getCount();

        int duration = 2000; // 2s

        SleepAndFailWork work = new SleepAndFailWork(0, false);
        service.schedule(work);

        assertTrue(service.awaitCompletion(duration * 3, TimeUnit.MILLISECONDS));

        // synchronous listener
        assertEquals(1 + initSyncEvntCount, WorkFailureEventListener.getCount());

        eventService.waitForAsyncCompletion();
        
        assertEquals(1 + initCount, DummyPostCommitEventListener.handledCount());
        assertEquals(1 + initEvtCount, DummyPostCommitEventListener.eventCount());
    }

    @Test
    public void testWorkManagerCancelScheduling() throws Exception {
        MetricsTracker tracker = new MetricsTracker();
        int duration = 5000; // 2s
        SleepWork work1 = new SleepWork(duration, false);
        SleepWork work2 = new SleepWork(duration, false);
        SleepWork work3 = new SleepWork(duration, false);
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);
        Thread.sleep(duration / 2);
        tracker.assertDiff(1, 2, 0, 0);

        // cancel the scheduled task
        SleepWork work4 = new SleepWork(duration, false, work3.getId());
        service.schedule(work4, Scheduling.CANCEL_SCHEDULED);
        // wait
        assertTrue(service.awaitCompletion(2 * duration, TimeUnit.MILLISECONDS));
        // canceled are taken in account as completed and canceled
        tracker.assertDiff(0, 0, 3, 1);
    }

    @Test
    @Ignore("Why this test is not run")
    public void testDuplicatedWorks() throws Exception {
        MetricsTracker tracker = new MetricsTracker();
        int duration = 2000; // 2s

        service.enableProcessing("SleepWork", false);
        SleepWork work1 = new SleepWork(duration, false, "1");
        SleepWork work2 = new SleepWork(duration, false, "1");

        service.schedule(work1);
        service.schedule(work2);

        tracker.assertDiff(1, 0, 0, 0);

        service.enableProcessing("SleepWork", true);

        assertTrue(service.awaitCompletion("SleepWork", duration * 10, TimeUnit.MILLISECONDS));
        tracker.assertDiff(0, 0, 1, 0);
    }

    @Test
    @Ignore
    public void testWorkManagerShutdown() throws Exception {
        int duration = 2000; // 2s
        SleepWork work1 = new SleepWork(duration, false);
        SleepWork work2 = new SleepWork(duration, false);
        SleepWork work3 = new SleepWork(duration, false);
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);

        Thread.sleep(duration / 2);
        assertState(RUNNING, work1);
        assertState(RUNNING, work2);
        assertState(SCHEDULED, work3);

        // shutdown workmanager service
        // work1 and work2 get a suspended notice and stop
        // work3 then gets scheduled immediately
        // and is either discarded (memory)
        // or put in the suspended queue (persistent)

        dontClearCompletedWork = true;
        boolean terminated = service.shutdown(duration * 2, TimeUnit.MILLISECONDS);
        assertTrue(terminated);

        // check work state
        assertState(SCHEDULED, work1);
        assertState(SCHEDULED, work2);
        assertState(persistent() ? SCHEDULED : UNKNOWN, work3);
        long remaining1 = work1.durationMillis;
        long remaining2 = work2.durationMillis;
        long remaining3 = work3.durationMillis;
        assertTrue("remaining1 " + remaining1, remaining1 < duration);
        assertTrue("remaining2 " + remaining2, remaining2 < duration);
        assertEquals(duration, remaining3);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.event.test:test-workmanager-disablequeue.xml")
    public void testWorkManagerConfigDisableOneBeforeStart() throws Exception {
        // before first applicationStarted:
        // disable SleepWork queue
        assertTrue(service.isProcessingEnabled("default"));
        assertFalse(service.isProcessingEnabled("SleepWork"));
    }

    @Test
    public void testWorkManagerConfigDisableOneAfterStart() throws Exception {
        // after first applicationStarted:
        // disable SleepWork queue
        service.enableProcessing("SleepWork", false);
        assertTrue(service.isProcessingEnabled("default"));
        assertFalse(service.isProcessingEnabled("SleepWork"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.event.test:test-workmanager-disablequeue1.xml")
    public void testWorkManagerConfigDisableAllBeforeStart() throws Exception {
        assertFalse(service.isProcessingEnabled("default"));
        assertTrue(service.isProcessingEnabled("SleepWork"));
    }

    @Test
    public void testWorkManagerConfigDisableAllAfterStart() throws Exception {
        try {
            service.enableProcessing(false);
            assertFalse(service.isProcessingEnabled());
            service.enableProcessing("SleepWork", true);
            assertTrue(service.isProcessingEnabled());
        } finally {
            // first disable SleepWork queue, otherwise work manager will register again metrics
            service.enableProcessing("SleepWork", false);
            // now re-enable all queues
            service.enableProcessing(true);
        }
    }

    @Ignore("NXP-15680")
    @Test
    @Deploy("org.nuxeo.ecm.core.event.test:test-workmanager-disablequeue.xml")
    public void testWorkManagerDisableProcessing() throws Exception {
        assumeTrue(persistent());

        int duration = 2000; // 2s
        SleepWork work1 = new SleepWork(duration, false);
        service.schedule(work1);

        Thread.sleep(duration / 2);

        // stays scheduled
        assertMetrics(1, 0, 0, 0);

        Thread.sleep(2 * duration);
        // still scheduled
        assertMetrics(1, 0, 0, 0);

        // now reactivate the queue
        // use a programmatic work queue descriptor
        WorkQueueDescriptor descr = new WorkQueueDescriptor();
        descr.id = "SleepWork";
        descr.processing = Boolean.TRUE;
        descr.categories = Collections.emptySet();
        service.activateQueue(descr);

        Thread.sleep(duration / 2);
        assertMetrics(0, 1, 0, 0);

        Thread.sleep(duration);
        assertMetrics(0, 0, 1, 0);
    }

    @Ignore("NXP-15680")
    @Test
    @Deploy("org.nuxeo.ecm.core.event.test:test-workmanager-disablequeue2.xml")
    public void testWorkManagerDisableProcessing2() throws Exception {
        assumeTrue(persistent());

        int duration = 2000; // 2s
        SleepWork work1 = new SleepWork(duration, false);
        service.schedule(work1);

        Thread.sleep(duration / 2);

        // stays scheduled
        assertMetrics(1, 0, 0, 0);

        // check that we can reenable the queue
        Thread.sleep(2 * duration);
        // still scheduled
        assertMetrics(1, 0, 0, 0);

        // now reactivate the queue
        // use a programmatic work queue descriptor
        WorkQueueDescriptor descr = new WorkQueueDescriptor();
        descr.id = "SleepWork";
        descr.processing = Boolean.TRUE;
        descr.categories = Collections.emptySet();
        service.activateQueue(descr);

        Thread.sleep(duration / 2);
        assertMetrics(0, 1, 0, 0);

        Thread.sleep(duration);
        assertMetrics(0, 0, 1, 0);
    }

    @Test
    public void transientFilesWorkAreCleaned() throws Exception {
        final File file = feature.resolveAndCreate(new File("pfouh"));
        service.schedule(new CreateFile(file));
        service.awaitCompletion(5, TimeUnit.SECONDS);
    }

    @Test
    public void testNoConcurrentJobsWithSameId() throws Exception {

        // create an init work to warm up the service, this is needed only for the embedded redis mode
        // sometime embedded mode takes around 1s to init, this prevent to put reliable assertion on time execution
        SleepWork initWork = new SleepWork(1, false);
        service.schedule(initWork);
        assertTrue(service.awaitCompletion(5, TimeUnit.SECONDS));
        assertMetrics(0, 0, 1, 0);

        // Schedule a first work
        int durationMS = 3000;
        SleepWork work = new SleepWork(durationMS, false);
        String workId = work.getId();
        service.schedule(work);

        // wait a bit to make sure it is running
        Thread.sleep(durationMS / 3);
        assertMetrics(0, 1, 1, 0);

        // schedule another work with the same workId
        // don't try to put a different duration, same work id means same work serializatoin
        SleepWork workbis = new SleepWork(durationMS, false, workId);
        service.schedule(workbis);

        // wait a bit, the first work is still running, the scheduled work should wait
        // because we don't want concurrent execution of work with the same workId
        Thread.sleep(durationMS / 3);
        assertMetrics(1, 1, 1, 0);

        // wait enough so the first work is done and the second should be running
        Thread.sleep(durationMS);
        assertMetrics(0, 1, 2, 0);

        assertTrue(service.awaitCompletion(2 * durationMS, TimeUnit.MILLISECONDS));
        assertMetrics(0, 0, 3, 0);
    }

}
