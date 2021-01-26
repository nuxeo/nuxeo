/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.importer.xml.parser;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Descriptor that can be used to define how Nuxeo DocumentModel properties are filled from the input XML.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@XObject("attributeConfig")
@XRegistry(compatWarnOnMerge = true)
@XRegistryId(value = { "@tagName", "@docProperty" })
public class AttributeConfigDescriptor {

    @XNode("@tagName")
    protected String tagName;

    @XNode("@docProperty")
    protected String targetDocProperty;

    // xpath to select when this config may be valid
    @XNode("@filter")
    protected String filter;

    // mapping between Nuxeo property names and corresponding xpath to extract values
    @XNodeMap(value = "mapping", key = "@documentProperty", type = HashMap.class, componentType = String.class)
    protected Map<String, String> mapping;

    @XNode("@xmlPath")
    protected String xmlPath;

    @XNode("@overwrite")
    protected boolean overwrite = false;

    public String getTagName() {
        return tagName;
    }

    public String getTargetDocProperty() {
        return targetDocProperty;
    }

    public String getFilter() {
        return filter;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public String getSingleXpath() {
        if (xmlPath != null) {
            return xmlPath;
        }
        if (mapping != null && !mapping.keySet().isEmpty()) {
            return mapping.values().iterator().next();
        }
        return null;
    }

    public boolean getOverwrite() {
        return overwrite;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String msg = "AttributeConfig\n\tTag Name: %s\n\tTarget Doc Property: %s\n\tFilter %s\n\tXML Path: %s\n\tOverwrite if list: %s\n\tMapping:\n";
        result.append(String.format(msg, tagName, targetDocProperty, filter, xmlPath, overwrite));
        if (mapping != null && !mapping.keySet().isEmpty()) {
            for (String key : mapping.keySet()) {
                result.append("\t\t" + key + ": " + mapping.get(key) + "\n");
            }
        } else {
            result.append("\t\tNO MAPPING\n");
        }
        return result.toString();
    }
}
