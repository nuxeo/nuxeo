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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 11.1
 */
@XObject("reporter")
public class MetricsReporterDescriptor implements Descriptor {

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@name")
    public String name;

    @XNode("@class")
    public Class<? extends MetricsReporter> klass;

    @XNode("@pollInterval")
    protected long pollInterval = 60;

    public long getPollInterval() {
        return pollInterval;
    }

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<>();

    @Override
    public String getId() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return toBooleanDefaultIfNull(enabled, true);
    }

    public Map<String, String> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    public MetricsReporter newInstance() {
        if (!MetricsReporter.class.isAssignableFrom(klass)) {
            throw new IllegalArgumentException(
                    "Cannot create reporter: " + getId() + ", class must implement MetricsReporter");
        }
        try {
            MetricsReporter ret = klass.getDeclaredConstructor().newInstance();
            ret.init(getPollInterval(), getOptions());
            return ret;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot create reporter: " + getId(), e);
        }
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (MetricsReporterDescriptor) o;
        var merged = new MetricsReporterDescriptor();
        merged.name = other.name;
        merged.enabled = defaultIfNull(other.enabled, enabled);
        merged.klass = defaultIfNull(other.klass, klass);
        merged.pollInterval = defaultIfNull(other.pollInterval, pollInterval);
        merged.options = new HashMap<>(options);
        merged.options.putAll(other.options);
        return merged;
    }
}
