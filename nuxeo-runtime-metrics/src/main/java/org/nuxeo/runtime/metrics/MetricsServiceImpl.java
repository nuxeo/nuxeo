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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class MetricsServiceImpl extends DefaultComponent implements
        MetricsService {

    protected static final Log log = LogFactory.getLog(MetricsServiceImpl.class);

    protected MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    private final Counter instanceUp = registry.counter(MetricRegistry.name(
            MetricsServiceImpl.class, "instance-up"));

    protected static final String CONFIGURATION_EP = "configuration";

    public static MetricsDescriptor config;

    public MetricsServiceImpl() {
        super();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONFIGURATION_EP.equals(extensionPoint)
                && contribution instanceof MetricsDescriptor) {
            log.debug("Registering metrics contribution");
            config = (MetricsDescriptor) contribution;
        } else {
            log.warn("Unknown EP " + extensionPoint);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        try {
            config.disable(registry);
        } finally {
            instanceUp.dec();
        }
        log.debug("Deactivate component.");
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        if (config == null) {
            // Use a default config
            config = new MetricsDescriptor();
        }
        log.info("Setting up metrics configuration");
        config.enable(registry);
        instanceUp.inc();
    }

}
