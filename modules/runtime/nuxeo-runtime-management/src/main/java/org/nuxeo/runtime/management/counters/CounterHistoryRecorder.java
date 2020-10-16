/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.runtime.management.counters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.javasimon.CallbackSkeleton;
import org.javasimon.Counter;

/**
 * Listen to Simon events to store past values of the counters History is kept in memory using
 * {@link CounterHistoryStack}
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @deprecated since 11.4: use dropwizard metrics instead
 */
@Deprecated(since = "11.4")
public class CounterHistoryRecorder extends CallbackSkeleton {

    protected Map<String, CounterHistoryStack> counterHistory = new ConcurrentHashMap<>();

    protected int historyLength = 100;

    public CounterHistoryRecorder(int size) {
        historyLength = size;
    }

    protected CounterHistoryStack getCounterHistoryStack(Counter counter) {
        CounterHistoryStack stack = counterHistory.get(counter.getName());
        if (stack == null) {
            stack = new CounterHistoryStack(historyLength);
            counterHistory.put(counter.getName(), stack);
        }
        return stack;
    }

    protected void storeCounter(Counter counter) {
        getCounterHistoryStack(counter).push(new long[] { System.currentTimeMillis(), counter.getCounter() });
    }

    @Override
    public void counterDecrease(Counter counter, long dec) {
        storeCounter(counter);
    }

    @Override
    public void counterSet(Counter counter, long val) {
        storeCounter(counter);
    }

    @Override
    public void counterIncrease(Counter counter, long inc) {
        storeCounter(counter);
    }

    public CounterHistoryStack getCounterHistory(String counterName) {
        return counterHistory.get(counterName);
    }
}
