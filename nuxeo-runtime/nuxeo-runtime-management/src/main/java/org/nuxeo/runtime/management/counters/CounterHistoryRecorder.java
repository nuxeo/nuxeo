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
package org.nuxeo.runtime.management.counters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.javasimon.CallbackSkeleton;
import org.javasimon.Counter;

/**
 * Listen to Simon events to store past values of the counters
 *
 * History is kept in memory using {@link CounterHistoryStack}
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class CounterHistoryRecorder extends CallbackSkeleton {

    protected Map<String, CounterHistoryStack> counterHistory = new ConcurrentHashMap<String, CounterHistoryStack>();

    protected int historyLength=100;

    public CounterHistoryRecorder(int size) {
        historyLength = size;
    }

    protected CounterHistoryStack getCounterHistoryStack(Counter counter) {
        CounterHistoryStack stack = counterHistory.get(counter.getName());
        if (stack==null) {
            stack = new CounterHistoryStack(historyLength);
            counterHistory.put(counter.getName(), stack);
        }
        return stack;
    }

    protected void storeCounter(Counter counter) {
        getCounterHistoryStack(counter).push(new long[] {System.currentTimeMillis(),counter.getCounter()});
    }

    @Override
    public void counterDecrease(Counter counter, long dec)
    {
        storeCounter(counter);
    }

    @Override
    public void counterSet(Counter counter, long val)
    {
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
