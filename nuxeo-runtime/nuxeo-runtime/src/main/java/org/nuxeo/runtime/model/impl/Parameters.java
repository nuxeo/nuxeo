package org.nuxeo.runtime.model.impl;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;

public class Parameters {

    static class Parameter {
        @XNode("@name")
        String name;

        @XNode("@documentation")
        String documentation;

        String value;
    }

    final Properties properties = new Properties();

    final Set<Parameter> entries = new HashSet<>();

    @XNodeList(value = "parameter", type = HashSet.class, componentType = Parameter.class)
    public void setParameters(Set<Parameter> params) {
        for (Parameter eachParam : params) {
            entries.add(eachParam);
            properties.put(eachParam.name, eachParam.value);
        }
    }

    void clear() {
        properties.clear();
        entries.clear();
    }
}
