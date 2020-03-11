/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.runtime.management.metrics;

import org.javasimon.CallbackSkeleton;
import org.javasimon.Counter;
import org.javasimon.Simon;
import org.javasimon.Stopwatch;
import org.javasimon.jmx.SimonSuperMXBean;
import org.nuxeo.runtime.management.counters.CounterMXBeanImpl;
import org.nuxeo.runtime.management.stopwatchs.StopwatchMXBeanImpl;

/**
 * Callback that registers MXBeans for metrics after their creation.
 */
public class MetricRegisteringCallback extends CallbackSkeleton {

    MetricRegister register;

    /**
     * @param register
     */
    public MetricRegisteringCallback(MetricRegister register) {
        this.register = register;
    }

    @Override
    public void simonCreated(Simon simon) {
        if (simon.getName() == null) {
            return;
        }
        register(simon);
    }

    @Override
    public void simonDestroyed(Simon simon) {
        register.unregisterMXBean(simon.getName());
    }

    @Override
    public void clear() {
        register.unregisterAll();
    }

    protected final void register(Simon simon) {
        SimonSuperMXBean mbean;
        if (simon instanceof Counter) {
            mbean = new CounterMXBeanImpl((Counter) simon);
        } else if (simon instanceof Stopwatch) {
            mbean = new StopwatchMXBeanImpl((Stopwatch) simon);
        } else {
            return;
        }
        register.registerMXBean(mbean, simon.getName(), mbean.getClass(), mbean.getType());
    }
}
