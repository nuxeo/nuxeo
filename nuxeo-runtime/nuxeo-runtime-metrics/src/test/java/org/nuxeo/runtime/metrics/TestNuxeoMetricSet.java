/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;

/**
 * @since 8.10-HF08, 9.2
 */
public class TestNuxeoMetricSet {

    @Test
    public void testInstantiation() {
        NuxeoMetricSet metrics = new NuxeoMetricSet();
        assertEquals("", metrics.getPrefixName());
        assertNotNull(metrics.getMetrics());
        assertTrue(metrics.getMetrics().isEmpty());
    }

    @Test
    public void testInstantiationWithPrefix() {
        NuxeoMetricSet metrics = new NuxeoMetricSet("nuxeo", "prefix");
        assertEquals("nuxeo.prefix", metrics.getPrefixName());
        assertNotNull(metrics.getMetrics());
        assertTrue(metrics.getMetrics().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPutGauge() {
        NuxeoMetricSet metrics = new NuxeoMetricSet("nuxeo", "prefix");
        Integer value = Integer.valueOf(10);
        metrics.putGauge(() -> value, "special", "value");
        assertNotNull(metrics.getMetrics());
        assertFalse(metrics.getMetrics().isEmpty());
        Metric gauge = metrics.getMetrics().get("nuxeo.prefix.special.value");
        // Check that name has been correctly built and we have the right gauge
        assertTrue(gauge instanceof Gauge);
        // Check also the name from getMetricNames
        assertTrue(metrics.getMetricNames().contains("nuxeo.prefix.special.value"));
        // Check the gauge value
        assertEquals(value, ((Gauge<Integer>) gauge).getValue());
    }

}
