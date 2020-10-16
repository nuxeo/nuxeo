/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.management.counters;

import org.javasimon.SimonManager;
import org.javasimon.SimonState;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component that provides the {@link CounterManager} service. Uses Simon Counters for implementation
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @deprecated since 11.4: use dropwizard metrics counter instead
 */
@Deprecated(since = "11.4")
public class CounterManagerImpl extends DefaultComponent implements CounterManager {

    public static final String COUNTER_PREFIX = "org.nuxeo";

    protected CounterHistoryRecorder history = new CounterHistoryRecorder(50);

    @Override
    public void enableCounters() {
        SimonManager.getCounter(COUNTER_PREFIX).setState(SimonState.ENABLED, true);
    }

    @Override
    public void disableCounters() {
        SimonManager.getCounter(COUNTER_PREFIX).setState(SimonState.DISABLED, true);
    }

    @Override
    public void start(ComponentContext context) {
        // create the root counter
        SimonManager.getCounter(COUNTER_PREFIX);
        // register call back for history management
        SimonManager.callback().addCallback(history);
    }

    @Override
    public void decreaseCounter(String counterName) {
        if (SimonManager.getCounter(counterName).isEnabled()) {
            SimonManager.getCounter(counterName).decrease();
        }
    }

    @Override
    public void increaseCounter(String counterName) {
        if (SimonManager.getCounter(counterName).isEnabled()) {
            SimonManager.getCounter(counterName).increase();
        }
    }

    @Override
    public void decreaseCounter(String counterName, long value) {
        if (SimonManager.getCounter(counterName).isEnabled()) {
            SimonManager.getCounter(counterName).decrease(value);
        }
    }

    @Override
    public void increaseCounter(String counterName, long value) {
        if (SimonManager.getCounter(counterName).isEnabled()) {
            SimonManager.getCounter(counterName).increase(value);
        }
    }

    @Override
    public void setCounterValue(String counterName, long value) {
        if (SimonManager.getCounter(counterName).isEnabled()) {
            SimonManager.getCounter(counterName).set(value);
        }
    }

    @Override
    public CounterHistoryStack getCounterHistory(String counterName) {

        CounterHistoryStack stack = history.getCounterHistory(counterName);
        if (stack == null) {
            return new CounterHistoryStack(50);
        } else {
            return stack;
        }
    }

}
