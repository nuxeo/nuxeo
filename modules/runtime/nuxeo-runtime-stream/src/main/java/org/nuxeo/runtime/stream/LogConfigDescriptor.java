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
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.model.Descriptor;

@XObject("logConfig")
public class LogConfigDescriptor implements Descriptor {

    public static final String SEP = ":";

    // @since 11.1
    @XNode("@enabled")
    protected boolean isEnabled = true;

    // @since 11.1
    @XNode("@default")
    protected boolean isDefault = false;

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
    }

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
    }

    @XNode("@name")
    public String name;

    @XNode("@type")
    public String type;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<>();

    @XNodeList(value = "log", type = ArrayList.class, componentType = LogDescriptor.class)
    public List<LogDescriptor> logs = new ArrayList<>();

    @XNodeList(value = "match", type = ArrayList.class, componentType = LogMatchDescriptor.class)
    public List<LogMatchDescriptor> matches = new ArrayList<>();

    @Override
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

    public List<String> getPatterns() {
        return matches.stream().map(match -> match.getId()).collect(Collectors.toList());
    }

}
