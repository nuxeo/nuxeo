/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.FileEventsTrackingFeature;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Adapt the tests with the limitation of the stream impl.
 *
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, FileEventsTrackingFeature.class })
@Deploy({ "org.nuxeo.runtime.stream", "org.nuxeo.ecm.core.event",
        "org.nuxeo.ecm.core.event.test:test-workmanager-config.xml" })
@LocalDeploy("org.nuxeo.ecm.core.event:test-stream-workmanager-service.xml")
public class StreamWorkManagerTest extends WorkManagerTest {

    @Override
    protected void assertState(Work.State state, SleepWork work) {
        // Stream workmanager can not access scheduled work so no assertion are possible on state
    }

    @Override
    void assertMetrics(long scheduled, long running, long completed, long cancelled) {
        WorkQueueMetrics current = service.getMetrics(QUEUE);
        assertEquals("completed", completed, current.completed.longValue());
        // stream workmanager has only an estimation of the max running
        assertTrue("running", running <= current.running.longValue());
        assertEquals("scheduled or running", scheduled + running, current.scheduled.longValue());
        // stream workmanager has no canceled metrics
    }

    @Override
    void assertWorkIdsEquals(List<String> expected, Work.State state) {
        // we can not get a list of work id with the stream impl
    }

    @Ignore()
    @Test
    public void testWorkManagerConfigDisableOneAfterStart() throws Exception {
        // for now processor can not be enable/disable once started
        super.testWorkManagerConfigDisableOneAfterStart();
    }

    @Ignore()
    @Test
    public void testWorkManagerConfigDisableAllAfterStart() throws Exception {
        // for now processor can not be enable/disable once started
        super.testWorkManagerConfigDisableAllAfterStart();
    }

}
