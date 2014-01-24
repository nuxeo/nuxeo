package org.nuxeo.segment.io;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.Script;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.mvel2.MVEL;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.core.scripting.Scripting.GroovyScript;
import org.nuxeo.ecm.automation.core.scripting.Scripting.MvelScript;

@XObject("mapper")
public class SegmentIOMapper {

    @XNode("@name")
    String name;

    @XNode("@targetAPI")
    String target = "track";

    @XNodeList(value = "events/event", type = ArrayList.class, componentType = String.class)
    List<String> events;

    @XNode("mvel")
    String mvelMapping;

    @XNode("groovy")
    String groovyMapping;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SegmentIOMapper) {
            return name.equals(((SegmentIOMapper)obj).name);
        }
        return super.equals(obj);
    }

    public Map<String, Object> getMappedData(Map<String, Object> context) {

        Map<String, Object> mapping = new HashMap<String, Object>();

        context.put("mapping", mapping);

        if (mvelMapping!=null && !mvelMapping.isEmpty()) {
            Serializable compiled = MVEL.compileExpression(mvelMapping);
            MVEL.executeExpression(compiled, context);
        } else if (groovyMapping!=null && !groovyMapping.isEmpty()) {

            Binding binding = new Binding(context);
            GroovyClassLoader loader = new GroovyClassLoader(this.getClass().getClassLoader());
            Class<?> klass = loader.parseClass(groovyMapping);
            Script script = InvokerHelper.createScript(klass, binding);
             script.run();
        } else {
            // WTF !
        }
        return mapping;
    }
}
