/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Delbosc Benoit
 */
package org.nuxeo.runtime.metrics;

import java.time.Instant;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class MetricsServiceImpl extends DefaultComponent implements MetricsService {

    protected static final Log log = LogFactory.getLog(MetricsServiceImpl.class);

    protected MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    private final Counter instanceUp = registry.counter(MetricRegistry.name("nuxeo", "instance-up"));

    protected static final String CONFIGURATION_EP = "configuration";

    public static MetricsDescriptor config;

    public MetricsServiceImpl() {
        super();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIGURATION_EP.equals(extensionPoint) && contribution instanceof MetricsDescriptor) {
            log.debug("Registering metrics contribution");
            config = (MetricsDescriptor) contribution;
        } else {
            log.warn("Unknown EP " + extensionPoint);
        }
    }

    @Override
    public void activate(ComponentContext context) {
        SharedMetricRegistries.getOrCreate(MetricsService.class.getName());;
    }

    @Override
    public void deactivate(ComponentContext context) {
        SharedMetricRegistries.remove(MetricsService.class.getName());;
        log.debug("Deactivate component.");
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        if (config == null) {
            // Use a default config
            config = new MetricsDescriptor();
        }
        log.info("Setting up metrics configuration");
        config.enable(registry);
        instanceUp.inc();
    }

    @Override
    public void applicationStandby(ComponentContext context, Instant instant) {
        try {
            config.disable(registry);
        } finally {
            instanceUp.dec();
        }
    }
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(MetricRegistry.class)) {
            return adapter.cast(registry);
        }
        return super.getAdapter(adapter);
    }

}
