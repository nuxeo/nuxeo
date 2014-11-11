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

    final Widget[] widgets;

    final Map<String, Serializable> properties;

    // Not used
    public LayoutRowImpl(Widget[] widgets) {
        this.widgets = widgets;
        properties = null;
    }

    //Not used
    public LayoutRowImpl(List<Widget> widgets) {
        this.widgets = widgets.toArray(new Widget[] {});
        properties = null;
    }

    public LayoutRowImpl(List<Widget> widgets,
            Map<String, Serializable> properties) {
        this.widgets = widgets.toArray(new Widget[] {});
        this.properties = properties;
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

}
