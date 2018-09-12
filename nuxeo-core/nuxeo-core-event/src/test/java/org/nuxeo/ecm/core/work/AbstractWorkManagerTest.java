/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.work.api.Work.State.RUNNING;
import static org.nuxeo.ecm.core.work.api.Work.State.SCHEDULED;
import static org.nuxeo.ecm.core.work.api.Work.State.UNKNOWN;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Assume;
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
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.FileEventsTrackingFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.trackers.files.FileEvent;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, FileEventsTrackingFeature.class })
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.ecm.core.event")
@Deploy("org.nuxeo.ecm.core.event.test:OSGI-INF/test-default-workmanager-config.xml")
@Deploy("org.nuxeo.ecm.core.event.test:test-workmanager-queue-config.xml")
public abstract class AbstractWorkManagerTest {

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

        /**
         * @since 10.2
         */
        public SleepAndFailWork(long durationMillis) {
            super(durationMillis);
        }

        /**
         * @deprecated since 10.2 debug flag is unused
         */
        @Deprecated
        public SleepAndFailWork(long durationMillis, boolean debug) {
            super(durationMillis, debug);
        }

        /**
         * @deprecated since 10.2 debug flag is unused
         */
        @Deprecated
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
            initialMetrics = service.getMetrics(QUEUE);
        }

        public void assertDiff(long scheduled, long running, long completed, long canceled) {
            long actualScheduled = initialMetrics.getScheduled().longValue() + scheduled;
            long actualRunning = initialMetrics.getRunning().longValue() + running;
            long actualCompleted = initialMetrics.getCompleted().longValue() + completed;
            long actualCanceled = initialMetrics.getCanceled().longValue() + canceled;
            assertMetrics(actualScheduled, actualRunning, actualCompleted, actualCanceled);
        }
    }

    protected static final String CATEGORY = SleepWork.CATEGORY;

    protected static final String QUEUE = SleepWork.CATEGORY;

    protected boolean dontClearCompletedWork;

    @Inject
    public WorkManager service;

    @Inject
    public EventService eventService;

    @Inject
    public FeaturesRunner runner;

    protected MetricsTracker tracker;

    protected int getDurationMillis() {
        return 200;
    }

    protected abstract boolean persistent();

    protected void assertMetrics(long scheduled, long running, long completed, long cancelled) {
        assertEquals(new WorkQueueMetrics(QUEUE, scheduled, running, completed, cancelled), service.getMetrics(QUEUE));
    }

    @Before
    public void before() {
        assertTrue(persistent() == service.supportsProcessingDisabling());
        try {
            service.awaitCompletion(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("works did not finish between tests");
        }
        tracker = new MetricsTracker();
        tracker.assertDiff(0, 0, 0, 0);
    }

    @Test
    public void testWorkManagerConfig() throws Exception {
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
        int duration = getDurationMillis() * 3;
        SleepWork work = new SleepWork(duration);
        service.schedule(work);

        assertTrue(work.getSchedulingTime() != 0);

        Thread.sleep(duration / 3);
        assertState(RUNNING, work);
        tracker.assertDiff(0, 1, 0, 0);

        Thread.sleep(duration);
        tracker.assertDiff(0, 0, 1, 0);
    }

    protected void assertState(Work.State state, SleepWork work) {
        assertEquals(state, service.getWorkState(work.getId()));
    }

    @Test
    public void testWorkManagerScheduling() throws Exception {
        int duration = getDurationMillis() * 5;
        SleepWork work1 = new SleepWork(duration);
        SleepWork work2 = new SleepWork(duration);
        SleepWork work3 = new SleepWork(duration);
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);

        Thread.sleep(duration / 2);
        tracker.assertDiff(1, 2, 0, 0);
        assertState(RUNNING, work1);
        assertState(RUNNING, work2);
        assertState(SCHEDULED, work3);

        // disabled IF_NOT_* features
        if (Boolean.FALSE.booleanValue()) {
            SleepWork work4 = new SleepWork(duration, work3.getId());
            service.schedule(work4, Scheduling.IF_NOT_SCHEDULED);
            assertState(UNKNOWN, work4);

            SleepWork work5 = new SleepWork(duration, work1.getId());
            service.schedule(work5, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
            assertState(UNKNOWN, work5);
        }

        SleepWork work7 = new SleepWork(duration, work3.getId());
        service.schedule(work7, Scheduling.CANCEL_SCHEDULED);
        assertState(SCHEDULED, work7);
        tracker.assertDiff(1, 2, 0, 1);

        service.schedule(new SleepAndFailWork(0));
        tracker.assertDiff(2, 2, 0, 1);

        assertTrue(service.awaitCompletion(duration * 3, TimeUnit.MILLISECONDS));
        tracker.assertDiff(0, 0, 4, 1);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.event:test-work-failure-listeners.xml")
    public void itCanFireWorkFailureEvent() throws Exception {
        int initCount = DummyPostCommitEventListener.handledCount();
        int initEvtCount = DummyPostCommitEventListener.eventCount();
        int initSyncEvntCount = WorkFailureEventListener.getCount();

        service.schedule(new SleepAndFailWork(0));

        assertTrue(service.awaitCompletion(6000, TimeUnit.MILLISECONDS));

        // synchronous listener
        assertEquals(1 + initSyncEvntCount, WorkFailureEventListener.getCount());

        eventService.waitForAsyncCompletion();

        assertEquals(1 + initCount, DummyPostCommitEventListener.handledCount());
        assertEquals(1 + initEvtCount, DummyPostCommitEventListener.eventCount());
    }

    @Test
    public void testWorkManagerCancelScheduling() throws Exception {
        int duration = getDurationMillis() * 5;
        SleepWork work1 = new SleepWork(duration);
        SleepWork work2 = new SleepWork(duration);
        SleepWork work3 = new SleepWork(duration);
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);
        Thread.sleep(duration / 2);
        tracker.assertDiff(1, 2, 0, 0);

        // cancel the scheduled task
        SleepWork work4 = new SleepWork(duration, work3.getId());
        service.schedule(work4, Scheduling.CANCEL_SCHEDULED);
        // wait
        assertTrue(service.awaitCompletion(duration * 2, TimeUnit.MILLISECONDS));
        // canceled are taken in account as completed and canceled
        tracker.assertDiff(0, 0, 3, 1);
    }

    @Test
    @Ignore("Why this test is not run")
    public void testDuplicatedWorks() throws Exception {
        service.enableProcessing("SleepWork", false);
        SleepWork work1 = new SleepWork(getDurationMillis(), "1");
        SleepWork work2 = new SleepWork(getDurationMillis(), "1");

        service.schedule(work1);
        service.schedule(work2);

        tracker.assertDiff(1, 0, 0, 0);

        service.enableProcessing("SleepWork", true);

        assertTrue(service.awaitCompletion("SleepWork", getDurationMillis() * 10, TimeUnit.MILLISECONDS));
        tracker.assertDiff(0, 0, 1, 0);
    }

    @Test
    @Ignore
    public void testWorkManagerShutdown() throws Exception {
        int duration = getDurationMillis() * 2;
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

        SleepWork work1 = new SleepWork(getDurationMillis());
        service.schedule(work1);

        Thread.sleep(getDurationMillis() / 2);

        // stays scheduled
        tracker.assertDiff(1, 0, 0, 0);

        Thread.sleep(getDurationMillis() * 2);
        // still scheduled
        tracker.assertDiff(1, 0, 0, 0);

        // now reactivate the queue
        // use a programmatic work queue descriptor
        WorkQueueDescriptor descr = new WorkQueueDescriptor();
        descr.id = "SleepWork";
        descr.processing = Boolean.TRUE;
        descr.categories = Collections.emptySet();
        ((WorkManagerImpl) service).activateQueue(descr);

        Thread.sleep(getDurationMillis() / 2);
        tracker.assertDiff(0, 1, 0, 0);

        Thread.sleep(getDurationMillis());
        tracker.assertDiff(0, 0, 1, 0);
    }

    @Ignore("NXP-15680")
    @Test
    @Deploy("org.nuxeo.ecm.core.event.test:test-workmanager-disablequeue2.xml")
    public void testWorkManagerDisableProcessing2() throws Exception {
        assumeTrue(persistent());
        SleepWork work1 = new SleepWork(getDurationMillis());
        service.schedule(work1);

        Thread.sleep(getDurationMillis() / 2);

        // stays scheduled
        tracker.assertDiff(1, 0, 0, 0);

        // check that we can reenable the queue
        Thread.sleep(getDurationMillis() * 2);
        // still scheduled
        tracker.assertDiff(1, 0, 0, 0);

        // now reactivate the queue
        // use a programmatic work queue descriptor
        WorkQueueDescriptor descr = new WorkQueueDescriptor();
        descr.id = "SleepWork";
        descr.processing = Boolean.TRUE;
        descr.categories = Collections.emptySet();
        ((WorkManagerImpl) service).activateQueue(descr);

        Thread.sleep(getDurationMillis() / 2);
        tracker.assertDiff(0, 1, 0, 0);

        Thread.sleep(getDurationMillis());
        tracker.assertDiff(0, 0, 1, 0);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.event.test:test-workmanager-disablequeue2.xml")
    public void testWorkManagerDisableProcessingAll() throws Exception {
        Assume.assumeTrue(service.supportsProcessingDisabling());
        SleepWork work1 = new SleepWork(getDurationMillis());
        service.schedule(work1);
        Thread.sleep(getDurationMillis() / 2);
        // stays scheduled
        tracker.assertDiff(1, 0, 0, 0);
    }

    @Test
    public void transientFilesWorkAreCleaned() throws Exception {
        FileEventsTrackingFeature feature = runner.getFeature(FileEventsTrackingFeature.class);
        final File file = feature.resolveAndCreate(new File("pfouh"));
        service.schedule(new CreateFile(file));
        service.awaitCompletion(5, TimeUnit.SECONDS);
    }

    @Test
    public void testNoConcurrentJobsWithSameId() throws Exception {
        // create an init work to warm up the service, this is needed only for the embedded redis mode
        // sometime embedded mode takes around 1s to init, this prevent to put reliable assertion on time execution
        SleepWork initWork = new SleepWork(1);
        service.schedule(initWork);
        assertTrue(service.awaitCompletion(5, TimeUnit.SECONDS));

        tracker.assertDiff(0, 0, 1, 0);

        // Schedule a first work
        int duration = getDurationMillis() * 3;
        SleepWork work = new SleepWork(duration);
        String workId = work.getId();
        service.schedule(work);

        // wait a bit to make sure it is running
        Thread.sleep(duration / 3);
        tracker.assertDiff(0, 1, 1, 0);

        // schedule another work with the same workId
        // don't try to put a different duration, same work id means same work serializatoin
        SleepWork workbis = new SleepWork(duration, workId);
        service.schedule(workbis);

        // wait a bit, the first work is still running, the scheduled work should wait
        // because we don't want concurrent execution of work with the same workId
        Thread.sleep(duration / 3);
        tracker.assertDiff(1, 1, 1, 0);

        // wait enough so the first work is done and the second should be running
        Thread.sleep(duration);
        tracker.assertDiff(0, 1, 2, 0);

        assertTrue(service.awaitCompletion(duration * 2, TimeUnit.MILLISECONDS));
        tracker.assertDiff(0, 0, 3, 0);
    }

    @Test
    public void testRunningWorkIsCanceled() throws InterruptedException {
        service.schedule(new SleepWork(10000, "1"));
        // ensure job is running
        Thread.sleep(100);
        tracker.assertDiff(0, 1, 0, 0);
        assertFalse(service.awaitCompletion(getDurationMillis(), TimeUnit.MILLISECONDS));
        service.schedule(new SleepWork(10000, "1"), WorkManager.Scheduling.CANCEL_SCHEDULED);
        assertTrue(WorkStateHelper.isCanceled("1"));
        assertTrue(service.awaitCompletion(1000, TimeUnit.MILLISECONDS));
    }

}
