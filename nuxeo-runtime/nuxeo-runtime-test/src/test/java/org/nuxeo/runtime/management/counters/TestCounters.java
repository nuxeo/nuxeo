/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.management.counters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Deploy("org.nuxeo.runtime.management")
@Features(RuntimeFeature.class)
public class TestCounters {

    @Test
    public void verifyServiceBinding() {

        CounterManager cm = Framework.getService(CounterManager.class);
        assertNotNull(cm);
    }

    protected class CounterCaller implements Runnable {
        @Override
        public void run() {
            int idx = new Random(System.currentTimeMillis()).nextInt(9);
            CounterManager cm = Framework.getService(CounterManager.class);
            if (idx % 2 == 0) {
                cm.increaseCounter("org.nuxeo.counter" + idx);
            } else {
                cm.decreaseCounter("org.nuxeo.counter" + idx);
            }
        }
    }

    protected void doRandomCounters() throws InterruptedException {
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(
                10000));
        for (int i = 0; i < 10000; i++) {
            tpe.execute(new CounterCaller());
        }
        tpe.shutdown();
        tpe.awaitTermination(120, TimeUnit.SECONDS);
    }

    protected String getCountersSnapshot() {
        StringBuilder sb = new StringBuilder();
        CounterManager cm = Framework.getService(CounterManager.class);
        for (int i = 0; i < 9; i++) {
            String snapshot = cm.getCounterHistory("org.nuxeo.counter" + i).toString();
            sb.append(snapshot);
        }
        return sb.toString();
    }

    @Test
    public void verifyConcurrency() throws InterruptedException {

        CounterManager cm = Framework.getService(CounterManager.class);

        doRandomCounters();

        for (int i = 0; i < 9; i++) {
            // System.out.print(cm.getCounterHistory("org.nuxeo.counter" + i));
        }

        String snapshot = getCountersSnapshot();

        cm.disableCounters();

        doRandomCounters();

        String snapshot2 = getCountersSnapshot();

        assertEquals(snapshot, snapshot2);

        cm.enableCounters();

        doRandomCounters();

        String snapshot3 = getCountersSnapshot();

        assertFalse(snapshot3.equals(snapshot));

    }

    @Test
    public void verifyHistory() throws InterruptedException {

        CounterManager cm = Framework.getService(CounterManager.class);

        String myCounter = "org.nuxeo.testMe";

        CounterHelper.increaseCounter(myCounter); // 1
        CounterHelper.increaseCounter(myCounter); // 2
        CounterHelper.increaseCounter(myCounter); // 3
        CounterHelper.decreaseCounter(myCounter); // 2
        CounterHelper.increaseCounter(myCounter); // 3
        CounterHelper.decreaseCounter(myCounter); // 2
        CounterHelper.decreaseCounter(myCounter); // 1
        CounterHelper.increaseCounter(myCounter); // 2
        CounterHelper.increaseCounter(myCounter); // 3

        CounterHistoryStack history = cm.getCounterHistory(myCounter);
        assertNotNull(history);

        // System.out.println(history.toString());
        assertEquals(3, history.get(0)[1]);
        assertEquals(2, history.get(5)[1]);
        assertEquals(1, history.get(8)[1]);

        CounterHelper.setCounterValue(myCounter, 0);
        for (int i = 0; i < 60; i++) {
            CounterHelper.increaseCounter(myCounter);
        }

        assertEquals(50, history.getAsList().size());
        assertEquals(60, history.get(0)[1]);
        assertEquals(11, history.get(49)[1]);

        // System.out.println(history.toString());

    }
}
