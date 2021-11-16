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
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.segment.io.SegmentIO.ACTIONS;

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

    private Class<?> klass;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SegmentIOMapper) {
            return name.equals(((SegmentIOMapper) obj).name);
        }
        return super.equals(obj);
    }

    public boolean isIdentify() {
        return ACTIONS.identify.name().equalsIgnoreCase(target);
    }

    public boolean isPage() {
        return ACTIONS.page.name().equalsIgnoreCase(target);
    }

    public boolean isScreen() {
        return ACTIONS.screen.name().equalsIgnoreCase(target);
    }

    public Class<?> getGroovyClazz() {
        if (klass == null) {
            synchronized (this) {
                if (klass == null) {
                    // Define Groovy class based on mapper contributions
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
                    try (GroovyClassLoader loader = new GroovyClassLoader(this.getClass().getClassLoader())) {
                        klass = loader.parseClass(sb.toString());
                    } catch (IOException e) {
                        throw new NuxeoException(String.format("Error during Groovy script execution for the '%s' segmentIO mapper", name), e);
                    }
                }
            }
        }
        return klass;
    }

    public Map<String, Serializable> getMappedData(Map<String, Object> context) {
        Map<String, Serializable> mapping = new HashMap<String, Serializable>();
        context.put("mapping", mapping);

        // Execute Groovy script to generate the mapped data
        Binding binding = new Binding(context);
        Script script = InvokerHelper.createScript(getGroovyClazz(), binding);
        script.run();

        return mapping;
    }

    public String getName() {
        return name;
    }

    public String getTarget() {
        return target;
    }

}
