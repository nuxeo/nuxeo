/*
 * (C) Copyright 2002-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

/**
 * Descriptor that can be used to define how Nuxeo DocumentModel properties are filled from the input XML
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@XObject("attributeConfig")
public class AttributeConfigDescriptor {

    @XNode("@tagName")
    protected String tagName;

    @XNode("@docProperty")
    protected String targetDocProperty;

    // xpath to select when this config may be valid
    @XNode("@filter")
    protected String filter;

    // mapping between Nuxeo property names and corresponding xpath to extract
    // values
    @XNodeMap(value = "mapping", key = "@documentProperty", type = HashMap.class, componentType = String.class)
    protected Map<String, String> mapping;

    @XNode("@xmlPath")
    protected String xmlPath;

    @XNode("@overwrite")
    protected boolean overwrite = false;
    
    public AttributeConfigDescriptor() {
    }

    public AttributeConfigDescriptor(String tagName, String targetDocProperty, Map<String, String> mapping,
            String filter) {
        this.tagName = tagName;
        this.targetDocProperty = targetDocProperty;
        if (mapping == null) {
            mapping = new HashMap<String, String>();
        } else {
            this.mapping = mapping;
        }
        this.filter = filter;
    }

    public AttributeConfigDescriptor(String tagName, String targetDocProperty, String xmlPath, String filter) {
        this.tagName = tagName;
        this.targetDocProperty = targetDocProperty;
        this.xmlPath = xmlPath;
        this.filter = filter;
    }

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

    public boolean getOverwrite(){
    	return overwrite;
    }

    @Override
    public String toString() {
        String msg = "AttributeConfig\n\tTag Name: %s\n\tTarget Doc Property: %s\n\tFilter %s\n\tXML Path: %s\n\tOverwrite if list: %s\n\tMapping:\n";
        String result = String.format(msg, tagName, targetDocProperty, filter, xmlPath, overwrite);
        if (mapping != null && !mapping.keySet().isEmpty()) {
            for (String key : mapping.keySet()) {
                result += "\t\t" + key + ": " + mapping.get(key) + "\n";
            }
        } else {
            result += "\t\tNO MAPPING\n";
        }

        return result;
    }
}
