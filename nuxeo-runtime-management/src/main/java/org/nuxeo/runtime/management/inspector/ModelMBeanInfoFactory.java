package org.nuxeo.runtime.management.inspector;

import java.util.HashMap;
import java.util.Map;

import javax.management.modelmbean.ModelMBeanInfo;

import org.jsesoft.ri.ReflectionInspector;

public class ModelMBeanInfoFactory {

    public ModelMBeanInfoFactory() {
        super(); // enabled breaking
    }

    private final Map<Class<?>, ModelMBeanInfo> infos = new HashMap<Class<?>, ModelMBeanInfo>();

    public ModelMBeanInfo getModelMBeanInfo(Class<?> resourceClass)
            throws Exception {
        if (infos.containsKey(resourceClass)) {
            return infos.get(resourceClass);
        }

        ModelMBeanInspectorStrategy inspectorStrategy = new ModelMBeanInspectorStrategy(
                resourceClass);

        ReflectionInspector inspector = new ReflectionInspector();

        inspector.setInspectee(resourceClass);
        inspector.setStrategy(inspectorStrategy);
        boolean reply = inspector.inspect();

        assert reply : "inspection failed";
        ModelMBeanInfo info = inspectorStrategy.getMBeanInfo();
        infos.put(resourceClass, info);
        return info;
    }
}
