/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.segment.io;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

@XObject("mapper")
public class SegmentIOMapper {

    private static Log log = LogFactory.getLog(SegmentIOMapper.class);

    @XNode("@name")
    String name;

    @XNode("@targetAPI")
    String target = "track";

    @XNodeList(value = "events/event", type = ArrayList.class, componentType = String.class)
    List<String> events;

    @XNode("groovy")
    String groovyMapping;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> parameters = new HashMap<String, String>();

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SegmentIOMapper) {
            return name.equals(((SegmentIOMapper) obj).name);
        }
        return super.equals(obj);
    }

    public boolean isIdentify() {
        return "identify".equalsIgnoreCase(target);
    }

    public Map<String, Serializable> getMappedData(Map<String, Object> context) {

        Map<String, Serializable> mapping = new HashMap<String, Serializable>();
        context.put("mapping", mapping);

        StringBuffer sb = new StringBuffer();
        for (String key : parameters.keySet()) {
            sb.append("mapping.put(\"");
            sb.append(key);
            sb.append("\", ");
            sb.append(parameters.get(key));
            sb.append(");\n");
        }

        if (groovyMapping != null && !groovyMapping.isEmpty()) {
            sb.append(groovyMapping);
        }

        Binding binding = new Binding(context);
        try (GroovyClassLoader loader = new GroovyClassLoader(this.getClass().getClassLoader())) {
            Class<?> klass = loader.parseClass(sb.toString());
            Script script = InvokerHelper.createScript(klass, binding);
            script.run();
        } catch (IOException e) {
            log.error(String.format("Error during Groovy script execution for the '%s' segmentIO mapper", name), e);
        }
        return mapping;
    }

    public String getName() {
        return name;
    }

    public String getTarget() {
        return target;
    }

}
