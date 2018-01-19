package org.nuxeo.ecm.automation.server.jaxrs.batch.handler;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchHandler;

import java.util.HashMap;
import java.util.Map;

@XObject("batchHandler")
public class BatchHandlerDescriptor {

    @XNode("name")
    private String name;

    @XNode("class")
    private Class<? extends BatchHandler> klass;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    private Map<String, String> properties;

    public void setName(String name) {
        this.name = name;
    }

    public void setKlass(Class<? extends BatchHandler> klass) {
        this.klass = klass;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public Class<? extends BatchHandler> getKlass() {
        return klass;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
