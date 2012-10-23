/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.runtime.management.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.javasimon.Counter;
import org.javasimon.CounterSample;
import org.javasimon.Split;
import org.javasimon.StopwatchSample;
import org.javasimon.callback.CallbackSkeleton;

/**
 * Listen to Simon events to store past values of the counters
 *
 * History is kept in memory using {@link MetricHistoryStack}
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class MetricHistoryRecorder extends CallbackSkeleton {

    protected Map<String, MetricHistoryStack> counterHistory = new ConcurrentHashMap<String, MetricHistoryStack>();

    protected int historyLength=100;

    public MetricHistoryRecorder(int size) {
        historyLength = size;
    }

    protected MetricHistoryStack getOrCreateHistoryStack(String name) {
        MetricHistoryStack stack = counterHistory.get(name);
        if (stack==null) {
            stack = new MetricHistoryStack(historyLength);
            counterHistory.put(name, stack);
        }
        return stack;
    }

    protected void storeCounter(Counter counter, CounterSample sample) {
        getOrCreateHistoryStack(counter.getName()).push(new long[] {sample.getLastUsage(),sample.getCounter()});
    }

    @Override
    public void onCounterDecrease(Counter counter, long dec, CounterSample sample)
    {
        storeCounter(counter, sample);
    }

    @Override
    public void onCounterSet(Counter counter, long val, CounterSample sample)
    {
        storeCounter(counter, sample);
    }

    @Override
    public void onCounterIncrease(Counter counter, long inc, CounterSample sample) {
        storeCounter(counter, sample);
    }


    @Override
    public void onStopwatchStop(Split split, StopwatchSample sample) {
        MetricHistoryStack stack = getOrCreateHistoryStack(split.getStopwatch().getName());
        stack.push(new long[] { sample.getLastUsage(), sample.getLast()});
    }

    public MetricHistoryStack getStack(String counterName) {
        return counterHistory.get(counterName);
    }

    public void clearStacks() {
        counterHistory.clear();
    }
}
