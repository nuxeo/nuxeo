package org.nuxeo.segment.io;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("mapper")
public class SegmentIOMapper {

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
            return name.equals(((SegmentIOMapper)obj).name);
        }
        return super.equals(obj);
    }

    public boolean isIdentify() {
        return "identify".equalsIgnoreCase(target);
    }

    public Map<String, String> getMappedData(Map<String, Object> context) {

        Map<String, String> mapping = new HashMap<String, String>();
        context.put("mapping", mapping);

        StringBuffer sb = new StringBuffer();
        for (String key : parameters.keySet()) {
            sb.append("mapping.put(\"");
            sb.append(key);
            sb.append("\", ");
            sb.append(parameters.get(key));
            sb.append(");\n");
        }

        if (groovyMapping!=null && !groovyMapping.isEmpty()) {
            sb.append(groovyMapping);
        }

        Binding binding = new Binding(context);
        GroovyClassLoader loader = new GroovyClassLoader(this.getClass().getClassLoader());
        Class<?> klass = loader.parseClass(sb.toString());
        Script script = InvokerHelper.createScript(klass, binding);
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
