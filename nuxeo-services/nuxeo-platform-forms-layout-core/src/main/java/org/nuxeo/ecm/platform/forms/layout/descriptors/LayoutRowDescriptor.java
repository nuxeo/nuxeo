/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
    Map<String, PropertiesDescriptor> properties = new HashMap<String, PropertiesDescriptor>();

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
        LayoutRowDefinition clone = new LayoutRowDefinitionImpl(name,
                getProperties(), cwidgets, alwaysSelected, selectedByDefault);
        return clone;
    }
}
