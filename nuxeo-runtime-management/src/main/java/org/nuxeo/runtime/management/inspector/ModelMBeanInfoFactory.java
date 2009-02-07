package org.nuxeo.runtime.management.inspector;

import java.util.HashMap;
import java.util.Map;

import javax.management.modelmbean.ModelMBeanInfo;

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
        ModelMBeanInfo info = new ModelMBeanIntrospector(resourceClass).introspect();
        infos.put(resourceClass, info);
        return info;
    }
}
