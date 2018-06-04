/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@SuppressWarnings("CanBeFinal")
@XObject("logConfig")
public class LogConfigDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@type")
    public String type;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<>();

    @XNodeList(value = "log", type = ArrayList.class, componentType = StreamDescriptor.class)
    public List<StreamDescriptor> logs = new ArrayList<>(0);

    public String getName() {
        return name;
    }

    public boolean isKafkaLog() {
        return "kafka".equalsIgnoreCase(type);
    }

    public String getOption(String key, String defaultValue) {
        return options.getOrDefault(key, defaultValue);
    }

    public Map<String, Integer> getLogsToCreate() {
        Map<String, Integer> ret = new HashMap<>();
        logs.forEach(d -> ret.put(d.name, d.size));
        return ret;
    }

    @XObject(value = "log")
    public static class StreamDescriptor {
        public static final Integer DEFAULT_PARTITIONS = 4;

        @XNode("@name")
        public String name;

        @XNode("@size")
        public Integer size = DEFAULT_PARTITIONS;

        public StreamDescriptor() {
            // empty constructor
        }
    }

}
