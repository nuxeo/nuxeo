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

import org.javasimon.SimonManager;
import org.javasimon.SimonState;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Runtime component that provides the {@link CounterManager} service.
 *
 * Uses Simon Counters for implementation
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class CounterManagerImpl extends DefaultComponent implements
        CounterManager {

    public static final String COUNTER_PREFIX = "org.nuxeo";

    protected CounterHistoryRecorder history = new CounterHistoryRecorder(50);

    public void enableCounters() {
        SimonManager.getCounter(COUNTER_PREFIX).setState(SimonState.ENABLED,
                true);
    }

    public void disableCounters() {
        SimonManager.getCounter(COUNTER_PREFIX).setState(SimonState.DISABLED,
                true);
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
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

    public CounterHistoryStack getCounterHistory(String counterName) {

        CounterHistoryStack stack = history.getCounterHistory(counterName);
        if (stack == null) {
            return new CounterHistoryStack(50);
        } else {
            return stack;
        }
    }

}
