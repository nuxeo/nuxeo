/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.coursera.metrics.datadog.DatadogReporter.Expansion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.codahale.metrics.MetricFilter;
import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.datadog.reporter")
@Deploy("org.nuxeo.datadog.reporter.test:datadog-contrib2.xml")
public class EmptyFilterConfDescriptorTest {

    @Inject
    DatadogReporterService reporter;

    @Test
    public void canConfigureMetricFilter() throws Exception {
        DatadogReporterServiceImpl service = (DatadogReporterServiceImpl) reporter;
        DatadogReporterConfDescriptor config = service.getConfig();

        assertThat(config.filter.getUseRegexFilters()).isFalse();
        assertThat(config.filter.getUseSubstringMatching()).isFalse();

        MetricFilter filter = service.getFilter();

        assertThat(filter.matches("jvm.useful", null)).isTrue();
        assertThat(filter.matches("jvm.useless", null)).isTrue();
        assertThat(filter.matches("nuxeo.all", null)).isTrue();

    }

    @Test
    public void canConfigureExpansions() throws Exception {
        DatadogReporterServiceImpl service = (DatadogReporterServiceImpl) reporter;
        DatadogReporterConfDescriptor config = service.getConfig();

        assertThat(config.filter.getExpansions()).isEqualTo(Expansion.ALL);
    }

}
