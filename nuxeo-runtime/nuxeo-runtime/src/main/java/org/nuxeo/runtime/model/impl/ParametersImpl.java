package org.nuxeo.runtime.model.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Parameters;

public abstract class ParametersImpl implements Parameters {

    final PropertiesWithDefaultsAccess properties = new PropertiesWithDefaultsAccess();

    final Map<String, Parameters.Descriptor> descriptors = new HashMap<>();

    @XNodeList(value = "define", type = LinkedList.class, componentType = ParametersImpl.DescriptorImpl.class)
    public void injectDefines(List<ParametersImpl.DescriptorImpl> defines) {
        for (ParametersImpl.DescriptorImpl each : defines) {
            descriptors.put(each.name, each);
            properties.setProperty(each.name, each.value);
        }
    }

    @XNodeList(value = "override", type = LinkedList.class, componentType = ParametersImpl.OverrideImpl.class)
    public void injectOverrides(List<ParametersImpl.OverrideImpl> overrides) {
        for (ParametersImpl.OverrideImpl eachParam : overrides) {
            properties.setProperty(eachParam.name, eachParam.value);
        }
    }

    @XObject
    public static class Base extends ParametersImpl {

    }

    @XObject
    static class Extended extends ParametersImpl {
        Parameters base;

        void override(Parameters parameters) {
            base = parameters;
            properties.setDefaults(parameters.getProperties());
        }
    }

    @XObject
    public static class OverrideImpl {
        @XNode("@name")
        String name;

        String value;

        @XContent()
        void injectValue(String text) {
            value = text.trim();
        }
    }

    @XObject
    public static class DescriptorImpl implements Parameters.Descriptor {
        @XNode("@name")
        String name;

        @XNode("@documentation")
        String documentation;

        String value;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDocumentation() {
            return documentation;
        }

        @Override
        public String getValue() {
            return value;
        }

        @XContent()
        public void setContent(String text) {
            value = Framework.getProperty(name, text.trim());
        }

        @Override
        public String toString() {
            return "name=" + name + ",documentation=" + documentation + ",value=" + value;
        }
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public Set<Parameters.Descriptor> getDescriptors() {
        return new HashSet<Parameters.Descriptor>(descriptors.values());
    }

    @Override
    public String toString() {
        return descriptors.toString();
    }

    static class PropertiesWithDefaultsAccess extends Properties {

        private static final long serialVersionUID = 1L;

        void setDefaults(Properties properties) {
            defaults = properties;
        }

    }
}