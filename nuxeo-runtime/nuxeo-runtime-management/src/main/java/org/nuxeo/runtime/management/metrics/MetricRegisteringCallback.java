/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
        } else  if (simon instanceof Stopwatch) {
            mbean = new StopwatchMXBeanImpl((Stopwatch) simon);
        } else {
            return;
        }
        register.registerMXBean(mbean, simon.getName(), mbean.getClass(), mbean.getType());
    }
}
