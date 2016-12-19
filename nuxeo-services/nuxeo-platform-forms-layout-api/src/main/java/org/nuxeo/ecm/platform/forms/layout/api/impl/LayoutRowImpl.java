/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

    protected String definitionId;

    // needed by GWT serialization
    protected LayoutRowImpl() {
        super();
    }

    /**
     * @since 5.5
     */
    public LayoutRowImpl(String name, boolean selectedByDefault, boolean alwaysSelected, List<Widget> widgets,
            Map<String, Serializable> properties, String definitionId) {
        this.name = name;
        this.selectedByDefault = selectedByDefault;
        this.alwaysSelected = alwaysSelected;
        this.widgets = widgets.toArray(new Widget[0]);
        this.properties = properties;
        this.definitionId = definitionId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getTagConfigId() {
        // XXX check if widget instances should be taken into account.
        return definitionId;
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
