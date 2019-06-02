package org.nuxeo.template.serializer.service;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.template.serializer.executors.Serializer;

/**
 * @Since 11.1
 */
@XObject("serializer")
public class SerializerContribution implements Descriptor {

    @XNode("@class")
    public Class<Serializer> implementationClass;

    @XNode("@name")
    public String name;

    @Override
    public String getId() {
        return StringUtils.defaultIfBlank(name, "default");
    }

    public Serializer getImplementation() {
        Serializer obj;
        try {
            obj = implementationClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Serializer Contribution Exception on Serializer construction: " + name, e);
        }
        return obj;
    }
}
