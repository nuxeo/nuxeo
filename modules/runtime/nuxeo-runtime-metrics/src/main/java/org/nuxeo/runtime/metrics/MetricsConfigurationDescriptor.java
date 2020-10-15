/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Delbosc Benoit
 */
package org.nuxeo.runtime.metrics;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

import io.dropwizard.metrics5.Metric;
import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;

@XObject("configuration")
public class MetricsConfigurationDescriptor implements Descriptor, MetricFilter {

    protected static final String ALL_METRICS = "ALL";

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
    }

    @XNode("@enabled")
    protected Boolean enabled;

    @XObject(value = "instrument")
    public static class InstrumentDescriptor implements Descriptor {

        @XNode("@name")
        protected String name;

        @XNode("@enabled")
        protected Boolean enabled;

        @Override
        public String getId() {
            return name;
        }

        @Override
        public boolean isEnabled() {
            return toBooleanDefaultIfNull(enabled, true);
        }

        @Override
        public InstrumentDescriptor merge(Descriptor o) {
            var other = (InstrumentDescriptor) o;
            var merged = new InstrumentDescriptor();
            merged.name = other.name;
            merged.enabled = defaultIfNull(other.enabled, enabled);
            return merged;
        }
    }

    @XNodeList(value = "instrument", type = ArrayList.class, componentType = InstrumentDescriptor.class)
    protected List<InstrumentDescriptor> instruments = new ArrayList<>();

    @XObject(value = "filter")
    public static class FilterDescriptor {

        @XNodeList(value = "allow/prefix", type = ArrayList.class, componentType = String.class)
        protected List<String> allowedPrefix = new ArrayList<>();

        @XNodeList(value = "deny/prefix", type = ArrayList.class, componentType = String.class)
        protected List<String> deniedPrefix = new ArrayList<>();

        @XNodeList(value = "deny/expansion", type = ArrayList.class, componentType = String.class)
        protected List<String> deniedExpansions = new ArrayList<>();

        public List<String> getAllowedPrefix() {
            return Collections.unmodifiableList(allowedPrefix);
        }

        public List<String> getDeniedPrefix() {
            return Collections.unmodifiableList(deniedPrefix);
        }

        public Set<MetricAttribute> getDeniedExpansions() {
            if (deniedExpansions.isEmpty()) {
                return Collections.emptySet();
            }
            return deniedExpansions.stream()
                                   .map(expansion -> MetricAttribute.valueOf(expansion.toUpperCase().strip()))
                                   .collect(Collectors.toSet());
        }
    }

    @XNode(value = "filter")
    protected FilterDescriptor filter = new FilterDescriptor();

    public static String expandName(MetricName metric) {
        if (metric.getTags().isEmpty()) {
            return metric.getKey();
        }
        String name = metric.getKey();
        for (Map.Entry<String, String> entry : metric.getTags().entrySet()) {
            String key = "." + entry.getKey() + ".";
            String keyAndValue = key + entry.getValue() + ".";
            name = name.replace(key, keyAndValue);
        }
        return name;
    }

    @Override
    public boolean matches(MetricName name, Metric metric) {
        String expandedName = expandName(name);
        return filter.allowedPrefix.stream().anyMatch(f -> ALL_METRICS.equals(f) || expandedName.startsWith(f))
                || filter.deniedPrefix.stream().noneMatch(f -> ALL_METRICS.equals(f) || expandedName.startsWith(f));
    }

    public Set<MetricAttribute> getDeniedExpansions() {
        return filter.getDeniedExpansions();
    }

    public boolean isEnabled() {
        return toBooleanDefaultIfNull(enabled, true);
    }

    public List<InstrumentDescriptor> getInstruments() {
        return instruments;
    }

    @Override
    public MetricsConfigurationDescriptor merge(Descriptor o) {
        var other = (MetricsConfigurationDescriptor) o;
        var merged = new MetricsConfigurationDescriptor();
        merged.enabled = defaultIfNull(other.enabled, enabled);
        merged.instruments = Descriptor.merge(other.instruments, instruments);
        merged.filter = defaultIfNull(other.filter, filter);
        return merged;
    }
}
