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
 * $Id: LayoutImpl.java 28478 2008-01-04 12:53:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;

/**
 * Implementation for layouts.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutImpl implements Layout {

    private static final long serialVersionUID = -8975637002024432963L;

    protected String id;

    protected String name;

    protected String mode;

    protected String template;

    protected LayoutRow[] rows;

    protected Map<String, Widget> widgetMap;

    protected int columns = 0;

    protected Map<String, Serializable> properties;

    // needed by GWT serialization
    protected LayoutImpl() {
        super();
    }

    private LayoutImpl(String name, String mode, String template, int columns) {
        this.name = name;
        this.mode = mode;
        this.template = template;
        this.columns = columns;
        this.widgetMap = new HashMap<String, Widget>();
    }

    public LayoutImpl(String name, String mode, String template,
            List<LayoutRow> rows, int columns) {
        this(name, mode, template, columns);
        this.rows = rows.toArray(new LayoutRow[] {});
        computeWidgetMap();
    }

    public LayoutImpl(String name, String mode, String template,
            List<LayoutRow> rows, int columns,
            Map<String, Serializable> properties) {
        this(name, mode, template, rows, columns);
        this.properties = properties;
    }

    protected void computeWidgetMap() {
        if (rows == null || rows.length == 0) {
            return;
        }
        for (LayoutRow row : rows) {
            Widget[] widgets = row.getWidgets();
            if (widgets == null || widgets.length == 0) {
                continue;
            }
            for (Widget widget : widgets) {
                if (widget != null) {
                    widgetMap.put(widget.getName(), widget);
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getMode() {
        return mode;
    }

    public String getTemplate() {
        return template;
    }

    public LayoutRow[] getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public Widget getWidget(String name) {
        return widgetMap.get(name);
    }

    public Map<String, Widget> getWidgetMap() {
        return Collections.unmodifiableMap(widgetMap);
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

    public void setProperty(String name, Serializable value) {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }
        properties.put(name, value);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append("LayoutImpl");
        buf.append(" {");
        buf.append(" name=");
        buf.append(name);
        buf.append(", id=");
        buf.append(id);
        buf.append(", mode=");
        buf.append(mode);
        buf.append(", template=");
        buf.append(template);
        buf.append(", properties=");
        buf.append(properties);
        buf.append('}');

        return buf.toString();
    }

}
