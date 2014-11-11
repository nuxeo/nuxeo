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
 * $Id: LayoutRowImpl.java 28478 2008-01-04 12:53:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;

/**
 * Implementation for layout rows.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutRowImpl implements LayoutRow {

    private static final long serialVersionUID = 1528198770297610864L;

    protected String name;

    protected boolean selectedByDefault = true;

    protected boolean alwaysSelected = false;

    protected Widget[] widgets;

    protected Map<String, Serializable> properties;

    // needed by GWT serialization
    protected LayoutRowImpl() {
        super();
    }

    public LayoutRowImpl(List<Widget> widgets,
            Map<String, Serializable> properties) {
        this.widgets = widgets.toArray(new Widget[] {});
        this.properties = properties;
        name = null;
        selectedByDefault = true;
        alwaysSelected = false;
    }

    public LayoutRowImpl(String name, boolean selectedByDefault,
            boolean alwaysSelected, List<Widget> widgets,
            Map<String, Serializable> properties) {
        this.name = name;
        this.selectedByDefault = selectedByDefault;
        this.alwaysSelected = alwaysSelected;
        this.widgets = widgets.toArray(new Widget[] {});
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public boolean isAlwaysSelected() {
        return alwaysSelected;
    }

    public boolean isSelectedByDefault() {
        return selectedByDefault;
    }

    public Widget[] getWidgets() {
        return widgets;
    }

    public int getSize() {
        if (widgets != null) {
            return widgets.length;
        }
        return 0;
    }

    public Map<String, Serializable> getProperties() {
        if (properties == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(properties);
    }

    public Serializable getProperty(String name) {
        if (properties != null) {
            return properties.get(name);
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append("LayoutRowImpl");
        buf.append(" {");
        buf.append(" name=");
        buf.append(name);
        buf.append(", properties=");
        buf.append(properties);
        buf.append('}');

        return buf.toString();
    }

}
