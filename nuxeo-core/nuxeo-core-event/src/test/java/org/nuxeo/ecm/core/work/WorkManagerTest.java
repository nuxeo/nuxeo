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
import org.nuxeo.common.logging.SequenceTracer;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
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

    void assertSetEquals(List<String> expected, List<String> actual) {
        assertEquals(new HashSet<>(expected), new HashSet<>(actual));
    }

    void assertMetrics(long scheduled, long running, long completed, long cancelled) {
        assertEquals(new WorkQueueMetrics(QUEUE, scheduled, running, completed, cancelled), service.getMetrics(QUEUE));
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
        assertMetrics(0, 0, 0, 0);
    }

    // overridden for persistence
    public boolean persistent() {
        return false; // in-memory, no persistence
    }

    @Test
    public void testBasics() throws Exception {
        deployAndStart();

        assertNotNull(service);
        assertMetrics(0, 0, 0, 0);
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
        assertEquals(Collections.singleton("SleepWork"), qd.categories);
    }

    @Test
    public void testWorkManagerWork() throws Exception {
        deployAndStart();

        int duration = 3000; // ms
        SleepWork work = new SleepWork(duration, false);
        service.schedule(work);

        assertTrue(work.getSchedulingTime() != 0);

        Thread.sleep(duration / 3);
        assertEquals(RUNNING, service.getWorkState(work.getId()));
        assertMetrics(0, 1, 0, 0);

        Thread.sleep(duration);
        assertMetrics(0, 0, 1, 0);

        // assertTrue(work.getStartTime() != 0);
        // assertTrue(work.getCompletionTime() != 0);
        // assertTrue(work.getCompletionTime() - work.getStartTime() > 0);
    }

    @Test
    public void testWorkManagerScheduling() throws Exception {
        deployAndStart();

        int duration = 5000; // 2s
        SleepWork work1 = new SleepWork(duration, false, "1");
        SleepWork work2 = new SleepWork(duration, false, "2");
        SleepWork work3 = new SleepWork(duration, false, "3");
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work3);

        Thread.sleep(duration / 2);
        assertMetrics(1, 2, 0, 0);
        assertEquals(RUNNING, service.getWorkState("1"));
        assertEquals(RUNNING, service.getWorkState("2"));
        assertEquals(SCHEDULED, service.getWorkState("3"));
        assertEquals(Collections.singletonList("3"), service.listWorkIds(QUEUE, SCHEDULED));
        assertSetEquals(Arrays.asList("1", "2"), service.listWorkIds(QUEUE, RUNNING));
        assertSetEquals(Arrays.asList("1", "2", "3"), service.listWorkIds(QUEUE, null));

        // disabled IF_NOT_* features
        if (Boolean.FALSE.booleanValue()) {
            SleepWork work4 = new SleepWork(duration, false, "3"); // id=3
            service.schedule(work4, Scheduling.IF_NOT_SCHEDULED);
            assertEquals(UNKNOWN, work4.getWorkInstanceState());

            SleepWork work5 = new SleepWork(duration, false, "1"); // id=1
            service.schedule(work5, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
            assertEquals(UNKNOWN, work5.getWorkInstanceState());
        }

        SleepWork work7 = new SleepWork(duration, false, "3"); // id=3
        service.schedule(work7, Scheduling.CANCEL_SCHEDULED);
        assertEquals(SCHEDULED, work7.getWorkInstanceState());
        assertMetrics(1, 2, 0, 1);

        SleepAndFailWork work8 = new SleepAndFailWork(0, false, "4");
        service.schedule(work8);
        assertMetrics(2, 2, 0, 1);

        assertTrue(service.awaitCompletion(duration * 3, TimeUnit.MILLISECONDS));
        assertMetrics(0, 0, 4, 1);

        assertEquals(Collections.emptyList(), service.listWorkIds(QUEUE, SCHEDULED));
        assertEquals(Collections.emptyList(), service.listWorkIds(QUEUE, RUNNING));
        assertEquals(Collections.emptyList(), service.listWorkIds(QUEUE, null));
    }

    public void testDuplicatedWorks() throws Exception {
        deployAndStart();
        int duration = 2000; // 2s

        service.enableProcessing("SleepWork", false);
        SleepWork work1 = new SleepWork(duration, false, "1");
        SleepWork work2 = new SleepWork(duration, false, "1");

        service.schedule(work1);
        service.schedule(work2);

        assertMetrics(1, 0, 0, 0);

        service.enableProcessing("SleepWork", true);

        assertTrue(service.awaitCompletion("SleepWork", duration * 10, TimeUnit.MILLISECONDS));
        assertMetrics(0, 0, 1, 0);
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
        assertEquals(persistent() ? SCHEDULED : UNKNOWN, work3.getWorkInstanceState());
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
        deployAndStart();
        // after first applicationStarted:
        // disable SleepWork queue
        service.enableProcessing("SleepWork", false);
        assertTrue(service.isProcessingEnabled("default"));
        assertFalse(service.isProcessingEnabled("SleepWork"));
    }

    @Test
    public void testWorkManagerConfigDisableAllBeforeStart() throws Exception {
        doDeploy();
        deployContrib("org.nuxeo.ecm.core.event.test", "test-workmanager-disablequeue1.xml");
        fireFrameworkStarted();

        assertFalse(service.isProcessingEnabled("default"));
        assertTrue(service.isProcessingEnabled("SleepWork"));
    }

    @Test
    public void testWorkManagerConfigDisableAllAfterStart() throws Exception {
        deployAndStart();
        service.enableProcessing(false);
        assertFalse(service.isProcessingEnabled());
        service.enableProcessing("SleepWork", true);
        assertTrue(service.isProcessingEnabled());
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

}
