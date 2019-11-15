/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.datadog.reporter;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.DatadogReporter.Expansion;
import org.coursera.metrics.datadog.DefaultMetricNameFormatter;
import org.coursera.metrics.datadog.transport.HttpTransport;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public class DatadogReporterServiceImpl extends DefaultComponent implements DatadogReporterService {

    private static final Logger log = LogManager.getLogger(DatadogReporterServiceImpl.class);

    private static final DefaultStringMatchingStrategy DEFAULT_STRING_MATCHING_STRATEGY = new DefaultStringMatchingStrategy();

    private static final RegexStringMatchingStrategy REGEX_STRING_MATCHING_STRATEGY = new RegexStringMatchingStrategy();

    private static final SubstringMatchingStrategy SUBSTRING_MATCHING_STRATEGY = new SubstringMatchingStrategy();

    protected final MetricRegistry metrics = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    private DatadogReporter reporter;

    private DatadogReporterConfDescriptor configuration;

    @Override
    public void start(ComponentContext context) {
        if (configuration == null) {
            log.error("Missing Datadog configuration: Datadog Reporter disabled");
            return;
        }
        if (StringUtils.isBlank(configuration.getApiKey())) {
            log.warn("Missing Datadog API key: Datadog Reporter disabled. Please make sure that the datadog.apikey"
                    + " property is set in nuxeo.conf.");
            return;
        }
        buildReporter();
        startReporter();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("configuration".equals(extensionPoint)) {
            // last one wins, no merge
            this.configuration = (DatadogReporterConfDescriptor) contribution;
        }
    }

    private void buildReporter() {
        HttpTransport httpTransport = new HttpTransport.Builder().withApiKey(configuration.getApiKey()).build();
        reporter = DatadogReporter.forRegistry(metrics)//
                                  .withHost(configuration.getHost())//
                                  .withTags(configuration.getTags())
                                  .withTransport(httpTransport)//
                                  .withExpansions(getExpansions())//
                                  .filter(getFilter())
                                  .withMetricNameFormatter(new DefaultMetricNameFormatter())//
                                  .build();
    }

    private EnumSet<Expansion> getExpansions() {
        return configuration.filter.getExpansions();
    }

    public MetricFilter getFilter() {
        final StringMatchingStrategy stringMatchingStrategy;
        if (configuration.filter.getUseRegexFilters()) {
            stringMatchingStrategy = REGEX_STRING_MATCHING_STRATEGY;
        } else if (configuration.filter.getUseSubstringMatching()) {
            stringMatchingStrategy = SUBSTRING_MATCHING_STRATEGY;
        } else {
            stringMatchingStrategy = DEFAULT_STRING_MATCHING_STRATEGY;
        }

        return (name, metric) ->
        // Include the metric if its name is not excluded and its name is included
        // Where, by default, with no includes setting, all names are included.
        !stringMatchingStrategy.containsMatch(configuration.filter.getExcludes(), name)
                && (configuration.filter.getIncludes().isEmpty()
                        || stringMatchingStrategy.containsMatch(configuration.filter.getIncludes(), name));
    }

    @Override
    public void startReporter() {
        if (reporter != null) {
            log.info("Starting Datadog reporter");
            reporter.start(configuration.getPollInterval(), TimeUnit.SECONDS);
        }
    }

    @Override
    public void stopReporter() {
        log.info("Stopping Datadog reporter");
        reporter.stop();
    }

    DatadogReporter getReporter() {
        return reporter;
    }

    DatadogReporterConfDescriptor getConfig() {
        return configuration;
    }

}
