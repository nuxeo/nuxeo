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
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;

/**
 * @author Anahide Tchertchian
 * @since 5.4.2
 */
public class WidgetSelectOptionImpl implements WidgetSelectOption {

    private static final long serialVersionUID = 1L;

    protected Serializable value;

    protected String var;

    protected String itemLabel;

    protected Map<String, String> labels;

    protected String itemValue;

    protected Serializable itemDisabled;

    protected Serializable itemRendered;

    // needed by GWT serialization
    protected WidgetSelectOptionImpl() {
        super();
    }

    public WidgetSelectOptionImpl(String itemLabel, String itemValue) {
        this(null, null, itemLabel, itemValue);
    }

    public WidgetSelectOptionImpl(Serializable value, String var,
            String itemLabel, String itemValue) {
        this(value, var, itemLabel, itemValue, null, null);
    }

    public WidgetSelectOptionImpl(Serializable value, String var,
            String itemLabel, String itemValue, Serializable itemDisabled,
            Serializable itemRendered) {
        super();
        this.value = value;
        this.var = var;
        this.itemLabel = itemLabel;
        this.itemValue = itemValue;
        this.itemDisabled = itemDisabled;
        this.itemRendered = itemRendered;
    }

    public Serializable getValue() {
        return value;
    }

    public String getVar() {
        return var;
    }

    public String getItemLabel() {
        return itemLabel;
    }

    @Override
    public String getItemLabel(String locale) {
        return labels.get(locale);
    }

    @Override
    public Map<String, String> getItemLabels() {
        return labels;
    }

    public void setItemLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public String getItemValue() {
        return itemValue;
    }

    public Serializable getItemDisabled() {
        return itemDisabled;
    }

    public Serializable getItemRendered() {
        return itemRendered;
    }

    @Override
    public String getTagConfigId() {
        StringBuilder builder = new StringBuilder();
        builder.append(value).append(";");
        builder.append(var).append(";");
        builder.append(itemLabel).append(";");
        if (labels != null) {
            builder.append(labels.toString());
        }
        builder.append(";");
        builder.append(itemValue).append(";");
        if (itemDisabled != null) {
            builder.append(itemDisabled.toString());
        }
        builder.append(";");
        if (itemRendered != null) {
            builder.append(itemRendered.toString());
        }
        builder.append(";");

        Integer intValue = new Integer(builder.toString().hashCode());
        return intValue.toString();
    }

    @Override
    public WidgetSelectOption clone() {
        WidgetSelectOptionImpl clone = new WidgetSelectOptionImpl(value, var,
                itemLabel, itemValue, itemDisabled, itemRendered);
        if (labels != null) {
            clone.setItemLabels(new HashMap<String, String>(labels));
        }
        return clone;
    }
}
