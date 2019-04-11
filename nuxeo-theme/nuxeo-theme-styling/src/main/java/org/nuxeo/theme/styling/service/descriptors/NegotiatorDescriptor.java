/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.theme.styling.service.descriptors;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.styling.negotiation.Negotiator;

/**
 * Descriptor for contributed negotiators.
 *
 * @since 7.4
 */
@XObject("negotiator")
public class NegotiatorDescriptor implements Comparable<NegotiatorDescriptor> {

    @XNode("@class")
    protected Class<Negotiator> klass;

    @XNode("@order")
    protected int order = 0;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties = new HashMap<>();

    public Class<Negotiator> getNegotiatorClass() {
        return klass;
    }

    public void setNegotiatorKlass(Class<Negotiator> klass) {
        this.klass = klass;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public int compareTo(NegotiatorDescriptor o) {
        int cmp = order - o.order;
        if (cmp == 0) {
            // make sure we have a deterministic sort
            cmp = klass.getName().compareTo(o.klass.getName());
        }
        return cmp;
    }

    @Override
    public NegotiatorDescriptor clone() {
        NegotiatorDescriptor clone = new NegotiatorDescriptor();
        clone.setNegotiatorKlass(getNegotiatorClass());
        clone.setOrder(getOrder());
        Map<String, String> props = getProperties();
        if (props != null) {
            clone.setProperties(new HashMap<>(props));
        }
        return clone;
    }

}
