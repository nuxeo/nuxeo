/*
 * (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *      Delbosc Benoit
 */
package org.nuxeo.runtime.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public interface MetricsService {

    /**
     * @since 5.7.2
     */
    MetricRegistry getRegistry();

    /**
     * @since 5.7.2
     */
    Gauge<?> newGauge(String mbean, String attribute, Class<?> clazz,
            String... name);

    /**
     * @return
     * @since 5.7.2
     */
    Gauge<?> newGauge(Gauge<?> gauge, Class<?> clazz,
            String... names);
    /**
     * @since 5.7.2
     */
    Counter newCounter(Class<?> clazz, String... names);

    /**
     * @since 5.7.2
     */
    Timer newTimer(Class<?> clazz, String... names);




}
