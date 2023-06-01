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
 *     pierre
 */
package org.nuxeo.ecm.core.work;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TestInMemoryWorkManager extends AbstractWorkManagerTest {

    @Override
    public boolean persistent() {
        return false;
    }

    @Test
    public void testNoConcurrentJobsWithSameId() throws InterruptedException {
        tracker.assertDiff(0, 0, 0, 0);

        // Schedule a first work
        int duration = getDurationMillis() * 3;
        SleepWork work = new SleepWork(duration);
        String workId = work.getId();
        service.schedule(work);

        // wait a bit to make sure it is running
        Thread.sleep(duration / 3);
        tracker.assertDiff(0, 1, 0, 0);

        // schedule another work with the same workId
        // don't try to put a different duration, same work id means same work serializatoin
        SleepWork workbis = new SleepWork(duration, workId);
        service.schedule(workbis);

        // wait a bit, the first work is still running, the scheduled work should wait
        // because we don't want concurrent execution of work with the same workId
        Thread.sleep(duration / 3);
        tracker.assertDiff(1, 1, 0, 0);

        // wait enough so the first work is done and the second should be running
        Thread.sleep(duration);
        tracker.assertDiff(0, 1, 1, 0);

        assertTrue(service.awaitCompletion(duration * 2L, TimeUnit.MILLISECONDS));
        tracker.assertDiff(0, 0, 2, 0);
    }
}
