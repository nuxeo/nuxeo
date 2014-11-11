/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;

/**
 * Default implementation for a layout row definition.
 * <p>
 * Useful to compute rows independently from the layout service.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class LayoutRowDefinitionImpl implements LayoutRowDefinition {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected Map<String, Map<String, Serializable>> properties;

    protected String[] widgets;

    protected boolean alwaysSelected = false;

    protected boolean selectedByDefault = true;

    // needed by GWT serialization
    protected LayoutRowDefinitionImpl() {
        super();
    }

    public LayoutRowDefinitionImpl(String name, String widget) {
        this.name = name;
        this.properties = null;
        if (widget == null) {
            this.widgets = new String[0];
        } else {
            this.widgets = new String[] { widget };
        }
        this.alwaysSelected = false;
        this.selectedByDefault = true;
    }

    public LayoutRowDefinitionImpl(String name,
            Map<String, Map<String, Serializable>> properties,
            List<String> widgets, boolean alwaysSelected,
            boolean selectedByDefault) {
        super();
        this.name = name;
        this.properties = properties;
        if (widgets == null) {
            this.widgets = new String[0];
        } else {
            this.widgets = widgets.toArray(new String[] {});
        }
        this.alwaysSelected = alwaysSelected;
        this.selectedByDefault = selectedByDefault;
    }

    public LayoutRowDefinitionImpl(String name,
            Map<String, Map<String, Serializable>> properties,
            String[] widgets, boolean alwaysSelected, boolean selectedByDefault) {
        super();
        this.name = name;
        this.properties = properties;
        this.widgets = widgets;
        this.alwaysSelected = alwaysSelected;
        this.selectedByDefault = selectedByDefault;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Serializable> getProperties(String layoutMode) {
        return WidgetDefinitionImpl.getProperties(properties, layoutMode);
    }

    @Override
    public Map<String, Map<String, Serializable>> getProperties() {
        return properties;
    }

    @Override
    public int getSize() {
        return widgets.length;
    }

    @Override
    public String[] getWidgets() {
        return widgets;
    }

    @Override
    public boolean isAlwaysSelected() {
        return alwaysSelected;
    }

    @Override
    public boolean isSelectedByDefault() {
        return selectedByDefault;
    }

}
