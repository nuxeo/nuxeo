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

import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;

/**
 * @author Anahide Tchertchian
 * @since 5.4.2
 */
public class WidgetSelectOptionsImpl implements WidgetSelectOptions {

    private static final long serialVersionUID = 1L;

    protected Serializable value;

    protected String var;

    protected String itemLabel;

    protected String itemValue;

    protected Serializable itemDisabled;

    protected Serializable itemRendered;

    protected String ordering;

    protected Boolean caseSensitive;

    // needed by GWT serialization
    protected WidgetSelectOptionsImpl() {
        super();
    }

    public WidgetSelectOptionsImpl(Serializable value, String var,
            String itemLabel, String itemValue) {
        this(value, var, itemLabel, itemValue, null, null);
    }

    public WidgetSelectOptionsImpl(Serializable value, String var,
            String itemLabel, String itemValue, Serializable itemDisabled,
            Serializable itemRendered) {
        this(value, var, itemLabel, itemValue, itemDisabled, itemRendered,
                null, null);
    }

    public WidgetSelectOptionsImpl(Serializable value, String var,
            String itemLabel, String itemValue, Serializable itemDisabled,
            Serializable itemRendered, String ordering, Boolean caseSensitive) {
        super();
        this.value = value;
        this.var = var;
        this.itemLabel = itemLabel;
        this.itemValue = itemValue;
        this.itemDisabled = itemDisabled;
        this.itemRendered = itemRendered;
        this.ordering = ordering;
        this.caseSensitive = caseSensitive;
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

    public String getItemValue() {
        return itemValue;
    }

    public Serializable getItemDisabled() {
        return itemDisabled;
    }

    public Serializable getItemRendered() {
        return itemRendered;
    }

    public String getOrdering() {
        return ordering;
    }

    public Boolean getCaseSensitive() {
        return caseSensitive;
    }

    @Override
    public String getTagConfigId() {
        StringBuilder builder = new StringBuilder();
        builder.append(value).append(";");
        builder.append(var).append(";");
        builder.append(itemLabel).append(";");
        builder.append(itemValue).append(";");
        if (itemDisabled != null) {
            builder.append(itemDisabled.toString());
        }
        if (itemRendered != null) {
            builder.append(itemRendered.toString());
        }
        builder.append(ordering).append(";");
        builder.append(caseSensitive).append(";");

        Integer intValue = new Integer(builder.toString().hashCode());
        return intValue.toString();
    }

    @Override
    public WidgetSelectOption clone() {
        return new WidgetSelectOptionsImpl(value, var, itemLabel, itemValue,
                itemDisabled, itemRendered, ordering, caseSensitive);
    }

}
