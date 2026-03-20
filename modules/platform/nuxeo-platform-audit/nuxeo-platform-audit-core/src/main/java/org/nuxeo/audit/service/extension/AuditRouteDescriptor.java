/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.audit.service.extension;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 2025.16
 */
@XObject("route")
public class AuditRouteDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("backend@name")
    protected String backendName;

    @XNodeList(value = "predicate", type = ArrayList.class, componentType = PredicateDescriptor.class)
    protected List<PredicateDescriptor> predicates = new ArrayList<>();

    @XNodeList(value = "event", type = ArrayList.class, componentType = EventDescriptor.class)
    protected List<EventDescriptor> events = new ArrayList<>();

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getBackendName() {
        return backendName;
    }

    public Stream<PredicateDescriptor> streamPredicates() {
        return predicates.stream();
    }

    public List<EventDescriptor> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /** @since 2025.16 */
    public Stream<EventDescriptor> streamEvents() {
        return events.stream();
    }

    @Override
    public AuditRouteDescriptor merge(Descriptor o) {
        var other = (AuditRouteDescriptor) o;
        var merged = new AuditRouteDescriptor();
        merged.name = getIfNull(other.name, name);
        merged.backendName = defaultIfBlank(other.backendName, backendName);
        merged.predicates = merge(other.predicates, predicates);
        merged.events = merge(events, other.events);
        return merged;
    }

    @SuppressWarnings("unchecked")
    protected <D extends Descriptor> List<D> merge(List<D> first, List<D> second) {
        var map = new HashMap<String, D>();
        first.forEach(descriptor -> map.put(descriptor.getId(), descriptor));
        second.forEach(descriptor -> map.merge(descriptor.getId(), descriptor,
                (previous, current) -> (D) previous.merge(current)));
        return new ArrayList<>(map.values());
    }

    @XObject("predicate")
    public static class PredicateDescriptor implements Descriptor {

        @XNode("@name")
        protected String name;

        // @XNode on setter
        protected Class<? extends Predicate<LogEntry>> predicateClass;

        @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
        protected Map<String, String> properties;

        @Override
        public String getId() {
            return name;
        }

        public String getName() {
            return name;
        }

        public Class<? extends Predicate<LogEntry>> getPredicateClass() {
            return predicateClass;
        }

        public Map<String, String> getProperties() {
            return Collections.unmodifiableMap(properties);
        }

        public Predicate<LogEntry> instantiatePredicate() {
            try {
                return predicateClass.getDeclaredConstructor(Map.class).newInstance(properties);
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException("Failed to instantiate predicate: " + name, e);
            }
        }

        @XNode("@class")
        protected void setPredicateClass(Class<? extends Predicate<LogEntry>> predicateClass) {
            if (predicateClass != null) {
                if (isBlank(name)) {
                    this.name = predicateClass.getSimpleName();
                }
                this.predicateClass = predicateClass;
            }
        }

        @Override
        public PredicateDescriptor merge(Descriptor o) {
            var other = (PredicateDescriptor) o;
            var merged = new PredicateDescriptor();
            merged.name = getIfNull(other.name, name);
            merged.predicateClass = getIfNull(other.predicateClass, predicateClass);
            merged.properties = new HashMap<>(properties);
            merged.properties.putAll(other.properties);
            return merged;
        }
    }

    @XObject("event")
    public static class EventDescriptor implements Descriptor {

        @XNode("@name")
        protected String name;

        @XNode("@enabled")
        protected Boolean enabled;

        @Override
        public String getId() {
            return name;
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return toBooleanDefaultIfNull(enabled, true);
        }

        @Override
        public EventDescriptor merge(Descriptor o) {
            var other = (EventDescriptor) o;
            var merged = new EventDescriptor();
            merged.name = getIfNull(other.name, name);
            merged.enabled = getIfNull(other.enabled, enabled);
            return merged;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EventDescriptor other)) {
                return false;
            }
            return Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }
}
