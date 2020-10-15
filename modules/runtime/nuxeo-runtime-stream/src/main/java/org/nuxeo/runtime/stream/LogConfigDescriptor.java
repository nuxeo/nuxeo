/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.runtime.stream;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

@XObject("logConfig")
public class LogConfigDescriptor implements Descriptor {
    // @since 11.1
    public static final String SEP = ":";

    // @since 11.1
    @XNode("@enabled")
    protected Boolean enabled;

    // @since 11.1
    @XNode("@default")
    protected Boolean isDefault;

    @XObject(value = "log")
    public static class LogDescriptor implements Descriptor {

        public static final Integer DEFAULT_PARTITIONS = 4;

        @XNode("@name")
        public String name;

        @XNode("@size")
        public Integer size = DEFAULT_PARTITIONS;

        @Override
        public String getId() {
            return name;
        }

        @Override
        public LogDescriptor merge(Descriptor o) {
            var other = (LogDescriptor) o;
            var merged = new LogDescriptor();
            merged.name = other.name;
            merged.size = defaultIfNull(other.size, size);
            return merged;
        }
    }

    // @since 11.1
    @XObject(value = "match")
    public static class LogMatchDescriptor implements Descriptor {

        @XNode("@name")
        public String name;

        @XNode("@group")
        public String group;

        @Override
        public String getId() {
            return (group != null && !group.isBlank()) ? name + SEP + group : name;
        }

        @Override
        public LogMatchDescriptor merge(Descriptor o) {
            var other = (LogMatchDescriptor) o;
            var merged = new LogMatchDescriptor();
            merged.name = other.name;
            merged.group = defaultIfNull(other.group, group);
            return merged;
        }
    }

    @XNode("@name")
    public String name;

    @XNode("@type")
    public String type;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<>();

    @XNodeList(value = "log", type = ArrayList.class, componentType = LogDescriptor.class)
    public List<LogDescriptor> logs = new ArrayList<>();

    // @since 11.1
    @XNodeList(value = "match", type = ArrayList.class, componentType = LogMatchDescriptor.class)
    public List<LogMatchDescriptor> matches = new ArrayList<>();

    @Override
    public String getId() {
        return name;
    }

    // @since 11.1
    @Override
    public boolean isEnabled() {
        return toBooleanDefaultIfNull(enabled, true);
    }

    // @since 11.1
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // @since 11.1
    public boolean isDefault() {
        return toBooleanDefaultIfNull(isDefault, false);
    }

    // @since 11.1
    public boolean onlyLogDeclaration() {
        return name == null && type == null;
    }

    // @since 11.1
    public List<String> getPatterns() {
        return matches.stream().map(LogMatchDescriptor::getId).collect(Collectors.toList());
    }

    @Override
    public LogConfigDescriptor merge(Descriptor o) {
        var other = (LogConfigDescriptor) o;
        var merged = new LogConfigDescriptor();
        merged.name = other.name;
        merged.enabled = defaultIfNull(other.enabled, enabled);
        merged.isDefault = defaultIfNull(other.isDefault, isDefault);
        merged.type = defaultIfBlank(other.type, type);
        merged.options = new HashMap<>(options);
        merged.options.putAll(other.options);
        merged.logs = Descriptor.merge(logs, other.logs);
        merged.matches = Descriptor.merge(matches, other.matches);
        return merged;
    }
}
