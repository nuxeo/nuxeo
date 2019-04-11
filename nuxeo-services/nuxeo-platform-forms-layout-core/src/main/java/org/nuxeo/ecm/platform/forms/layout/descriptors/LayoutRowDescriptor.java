/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LayoutRowDescriptor.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.descriptors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutRowDefinitionImpl;

/**
 * Layout row descriptor.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("row")
public class LayoutRowDescriptor {

    @XNode("@name")
    String name;

    @XNode("@selectedByDefault")
    boolean selectedByDefault = true;

    @XNode("@alwaysSelected")
    boolean alwaysSelected = false;

    @XNodeList(value = "widget", type = WidgetReferenceDescriptor[].class, componentType = WidgetReferenceDescriptor.class)
    WidgetReferenceDescriptor[] widgets = new WidgetReferenceDescriptor[0];

    @XNodeMap(value = "properties", key = "@mode", type = HashMap.class, componentType = PropertiesDescriptor.class)
    Map<String, PropertiesDescriptor> properties = new HashMap<>();

    public String getName() {
        return name;
    }

    public boolean isSelectedByDefault() {
        return selectedByDefault;
    }

    public boolean isAlwaysSelected() {
        return alwaysSelected;
    }

    public int getSize() {
        return widgets.length;
    }

    public String[] getWidgets() {
        String[] names = new String[widgets.length];
        for (int i = 0; i < widgets.length; i++) {
            names[i] = widgets[i].getName();
        }
        return names;
    }

    public Map<String, Serializable> getProperties(String layoutMode) {
        return WidgetDescriptor.getProperties(properties, layoutMode);
    }

    public Map<String, Map<String, Serializable>> getProperties() {
        return WidgetDescriptor.getProperties(properties);
    }

    public LayoutRowDefinition getLayoutRowDefinition() {
        WidgetReference[] cwidgets = null;
        if (widgets != null) {
            cwidgets = new WidgetReference[widgets.length];
            for (int i = 0; i < widgets.length; i++) {
                cwidgets[i] = widgets[i].getWidgetReference();
            }
        }
        LayoutRowDefinition clone = new LayoutRowDefinitionImpl(name, getProperties(), cwidgets, alwaysSelected,
                selectedByDefault);
        return clone;
    }
}
