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
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.metrics.MetricAttributes;
import org.nuxeo.runtime.management.metrics.MetricAttributesProvider;
import org.nuxeo.runtime.management.metrics.MetricHistoryProvider;
import org.nuxeo.runtime.management.metrics.MetricHistoryStack;
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

    public static final String SIMON_ROOT = "org.nuxeo.counters";

    protected static final String simonName(String name) {
        return SIMON_ROOT + "." + name;
    }

    @Override
    public void enableCounters() {
        SimonManager.getCounter(SIMON_ROOT).setState(SimonState.ENABLED,
                true);
    }

    @Override
    public void disableCounters() {
        SimonManager.getCounter(SIMON_ROOT).setState(SimonState.DISABLED,
                true);
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        // create the root counter
        SimonManager.getCounter(SIMON_ROOT);
    }

    @Override
    public void decreaseCounter(String counterName) {
        SimonManager.getCounter(simonName(counterName)).decrease();
    }

    @Override
    public void increaseCounter(String counterName) {
        SimonManager.getCounter(simonName(counterName)).increase();
    }

    @Override
    public void decreaseCounter(String counterName, long value) {
        SimonManager.getCounter(simonName(counterName)).decrease(value);
    }

    @Override
    public void increaseCounter(String counterName, long value) {
        SimonManager.getCounter(simonName(counterName)).increase(value);
    }

    @Override
    public void setCounterValue(String counterName, long value) {
        SimonManager.getCounter(simonName(counterName)).set(value);
    }

    @Override
    public MetricHistoryStack getCounterHistory(String counterName) {
        return Framework.getLocalService(MetricHistoryProvider.class).getStack(simonName(counterName));
    }

    @Override
    public MetricAttributes getAttributes(String name) {
        return Framework.getLocalService(MetricAttributesProvider.class).getAttributes(simonName(name));
    }

 }
