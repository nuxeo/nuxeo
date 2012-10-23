/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     "Stephane Lacoin (aka matic) slacoin@nuxeo.com"
 */
package org.nuxeo.runtime.management.stopwatchs;

import org.javasimon.SimonManager;
import org.javasimon.SimonState;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.metrics.MetricAttributes;
import org.nuxeo.runtime.management.metrics.MetricAttributesProvider;
import org.nuxeo.runtime.management.metrics.MetricHistoryProvider;
import org.nuxeo.runtime.management.metrics.MetricHistoryStack;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author "Stephane Lacoin (aka matic) slacoin@nuxeo.com"
 *
 */
public class StopwatchManagerImpl extends DefaultComponent implements StopwatchManager {


    public static final String SIMON_ROOT = "org.nuxeo.stopwatch";

    protected static final String simonName(String name) {
        return SIMON_ROOT + "." + name;
    }

    protected class SimonSplitWrapper implements Split {

        protected final org.javasimon.Split wrapped;

        SimonSplitWrapper(org.javasimon.Split wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void stop() {
            wrapped.stop();
        }


        @Override
        public String getName() {
            return wrapped.getStopwatch().getName().substring(SIMON_ROOT.length());
        }
    }

    @Override
    public void enableStopwatchs() {
        SimonManager.getSimon(SIMON_ROOT).setState(SimonState.ENABLED,
                true);
    }

    @Override
    public void disableStopwatchs() {
        SimonManager.getSimon(SIMON_ROOT).setState(SimonState.DISABLED,
                true);
    }


    @Override
    public Split start(String name) {
        return new SimonSplitWrapper(SimonManager.getStopwatch(simonName(name)).start());
    }


    @Override
    public MetricHistoryStack getStack(String name) {
        return Framework.getLocalService(MetricHistoryProvider.class).getStack(simonName(name));
    }


    @Override
    public MetricAttributes getAttributes(String name) {
        return Framework.getLocalService(MetricAttributesProvider.class).getAttributes(simonName(name));
    }

 }
