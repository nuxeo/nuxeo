/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Adapt the tests with the limitation of the stream impl.
 *
 * @since 9.3
 */
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.ecm.core.event:test-stream-workmanager-service.xml")
public class StreamWorkManagerTest extends AbstractWorkManagerTest {

    @Override
    public boolean persistent() {
        return true;
    }

    @Override
    protected int getDurationMillis() {
        return 1000;
    }

    @Override
    protected void assertState(Work.State state, SleepWork work) {
        // Stream workmanager can not access scheduled work so no assertion are possible on state
    }

    @Override
    protected void assertMetrics(long scheduled, long running, long completed, long cancelled) {
        WorkQueueMetrics current = service.getMetrics(QUEUE);
        assertEquals("completed", completed, current.completed.longValue());
        // stream workmanager has only an estimation of the max running
        assertTrue("running", running <= current.running.longValue());
        assertEquals("scheduled or running", scheduled + running, current.scheduled.longValue());
        // stream workmanager has no canceled metrics
    }

    @Override
    @Ignore
    @Test
    public void testWorkManagerConfigDisableOneAfterStart() throws Exception {
        // for now processor can not be enable/disable once started
        super.testWorkManagerConfigDisableOneAfterStart();
    }

    @Override
    @Ignore
    @Test
    public void testWorkManagerConfigDisableAllAfterStart() throws Exception {
        // for now processor can not be enable/disable once started
        super.testWorkManagerConfigDisableAllAfterStart();
    }

    @Test
    public void testWorkIdempotent() throws InterruptedException {
        SleepWork work = new SleepWork(getDurationMillis());
        assertTrue(work.isIdempotent());
        service.schedule(work);
        assertTrue(service.awaitCompletion(getDurationMillis() * 5, TimeUnit.MILLISECONDS));
        tracker.assertDiff(0, 0, 1, 0);

        // schedule again the exact same work 3 times
        service.schedule(work);
        service.schedule(work);
        service.schedule(work);

        // works with the same id are skipped immediately and marked as completed, we don't have to wait 5s
        assertTrue(service.awaitCompletion(getDurationMillis() / 2, TimeUnit.MILLISECONDS));
        tracker.assertDiff(0, 0, 4, 0);
    }

    /**
     * This test cannot be run with storeState enabled.<br>
     * When the first work finishes, its status is not scheduled anymore and following works are not run.
     */
    @Test
    @Deploy("org.nuxeo.ecm.core.event:test-stream-workmanager-disable-storestate.xml")
    public void testWorkNonIdempotent() throws InterruptedException {
        SleepWork work = new SleepWork(getDurationMillis());
        work.setIdempotent(false);
        assertFalse(work.isIdempotent());
        service.schedule(work);
        assertTrue(service.awaitCompletion(getDurationMillis() * 10, TimeUnit.MILLISECONDS));
        tracker.assertDiff(0, 0, 1, 0);

        // schedule again the exact same work 3 times
        service.schedule(work);
        service.schedule(work);
        service.schedule(work);

        // works with the same id are not skipped we need to wait more
        assertFalse(service.awaitCompletion(getDurationMillis() / 2, TimeUnit.MILLISECONDS));

        assertTrue(service.awaitCompletion(getDurationMillis() * 10, TimeUnit.MILLISECONDS));
        tracker.assertDiff(0, 0, 4, 0);
    }

    @Test
    public void testWorkIdempotentConcurrent() throws InterruptedException {
        SleepWork work1 = new SleepWork(getDurationMillis());
        SleepWork work2 = new SleepWork(getDurationMillis());
        service.schedule(work1);
        service.schedule(work1);
        service.schedule(work1);
        service.schedule(work2);
        service.schedule(work2);
        service.schedule(work2);
        // we don't know if work1 and work2 are executed on the same thread
        // but we assume that the max duration is work1 + work2 because there is only one invocation of each
        assertTrue(service.awaitCompletion(getDurationMillis() * 3 - 500, TimeUnit.MILLISECONDS));
        tracker.assertDiff(0, 0, 6, 0);
    }

    @Override
    @Ignore
    @Test
    public void testNoConcurrentJobsWithSameId() throws Exception {
        // default workmanager guaranty that works with the same id can not be scheduled while another is running
        // stream impl provides stronger guaranty, works with same id are executed only once (scheduled, running or
        // completed)
        super.testNoConcurrentJobsWithSameId();
    }

}
