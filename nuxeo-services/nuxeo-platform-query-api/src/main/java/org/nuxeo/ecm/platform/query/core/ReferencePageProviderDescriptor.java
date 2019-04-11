/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.query.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("coreQueryPageProvider")
public class ReferencePageProviderDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    String name;

    @XNode("@enabled")
    boolean enabled = true;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties = new HashMap<>();

    @XNodeList(value = "parameter", type = String[].class, componentType = String.class)
    String[] queryParameters;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String[] getQueryParameters() {
        return queryParameters;
    }

    /**
     * @since 5.6
     */
    @Override
    public ReferencePageProviderDescriptor clone() {
        ReferencePageProviderDescriptor clone = new ReferencePageProviderDescriptor();
        clone.name = getName();
        clone.enabled = isEnabled();
        Map<String, String> props = getProperties();
        if (props != null) {
            clone.properties = new HashMap<>();
            clone.properties.putAll(props);
        }
        String[] params = getQueryParameters();
        if (params != null) {
            clone.queryParameters = params.clone();
        }
        return clone;
    }

}
