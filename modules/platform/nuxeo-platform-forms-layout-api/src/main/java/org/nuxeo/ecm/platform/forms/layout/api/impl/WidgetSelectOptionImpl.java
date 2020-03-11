/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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

    public WidgetSelectOptionImpl(Serializable value, String var, String itemLabel, String itemValue) {
        this(value, var, itemLabel, itemValue, null, null);
    }

    public WidgetSelectOptionImpl(Serializable value, String var, String itemLabel, String itemValue,
            Serializable itemDisabled, Serializable itemRendered) {
        super();
        this.value = value;
        this.var = var;
        this.itemLabel = itemLabel;
        this.itemValue = itemValue;
        this.itemDisabled = itemDisabled;
        this.itemRendered = itemRendered;
    }

    @Override
    public Serializable getValue() {
        return value;
    }

    @Override
    public String getVar() {
        return var;
    }

    @Override
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

    @Override
    public String getItemValue() {
        return itemValue;
    }

    @Override
    public Serializable getItemDisabled() {
        return itemDisabled;
    }

    @Override
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

        Integer intValue = Integer.valueOf(builder.toString().hashCode());
        return intValue.toString();
    }

    @Override
    public WidgetSelectOption clone() {
        WidgetSelectOptionImpl clone = new WidgetSelectOptionImpl(value, var, itemLabel, itemValue, itemDisabled,
                itemRendered);
        if (labels != null) {
            clone.setItemLabels(new HashMap<>(labels));
        }
        return clone;
    }

    /**
     * @since 7.2
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WidgetSelectOptionImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        WidgetSelectOptionImpl w = (WidgetSelectOptionImpl) obj;
        return new EqualsBuilder().append(value, w.value).append(var, w.var).append(itemLabel, w.itemLabel).append(
                labels, w.labels).append(itemValue, w.itemValue).append(itemDisabled, w.itemDisabled).append(
                itemRendered, w.itemRendered).isEquals();
    }

}
