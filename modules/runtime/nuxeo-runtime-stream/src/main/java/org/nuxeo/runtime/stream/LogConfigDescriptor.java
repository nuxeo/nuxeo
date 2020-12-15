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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

@XObject("logConfig")
@XRegistry(enable = false)
public class LogConfigDescriptor {
    // @since 11.1
    public static final String SEP = ":";

    // @since 11.1
    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    protected boolean isEnabled;

    // @since 11.1
    @XNode(value = "@default", defaultAssignment = "false")
    protected boolean isDefault;

    @XObject(value = "log")
    public static class LogDescriptor {

        public static final Integer DEFAULT_PARTITIONS = 4;

        @XNode("@name")
        public String name;

        @XNode("@size")
        public Integer size = DEFAULT_PARTITIONS;

        public String getId() {
            return name;
        }
    }

    // @since 11.1
    @XObject(value = "match")
    public static class LogMatchDescriptor {

        @XNode("@name")
        public String name;

        @XNode("@group")
        public String group;

        public String getId() {
            return (group != null && !group.isBlank()) ? name + SEP + group : name;
        }
    }

    @XNode("@name")
    @XRegistryId
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

    public String getId() {
        return name;
    }

    // @since 11.1
    public boolean isEnabled() {
        return isEnabled;
    }

    // @since 11.1
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    // @since 11.1
    public boolean isDefault() {
        return isDefault;
    }

    // @since 11.1
    public boolean onlyLogDeclaration() {
        return name == null && type == null;
    }

    // @since 11.1
    public List<String> getPatterns() {
        return matches.stream().map(match -> match.getId()).collect(Collectors.toList());
    }

}
