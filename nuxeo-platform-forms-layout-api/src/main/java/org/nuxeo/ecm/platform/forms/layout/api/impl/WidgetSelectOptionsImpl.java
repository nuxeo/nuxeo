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

import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;

/**
 * @author Anahide Tchertchian
 * @since 5.4.1
 */
public class WidgetSelectOptionsImpl implements WidgetSelectOptions {

    private static final long serialVersionUID = 1L;

    protected Object value;

    protected String var;

    protected String itemLabel;

    protected String itemValue;

    protected Object itemDisabled;

    protected Object itemRendered;

    protected String ordering;

    protected Boolean caseSensitive;

    public WidgetSelectOptionsImpl(Object value, String var, String itemLabel,
            String itemValue) {
        this(value, var, itemLabel, itemValue, null, null);
    }

    public WidgetSelectOptionsImpl(Object value, String var, String itemLabel,
            String itemValue, Boolean itemDisabled, Boolean itemRendered) {
        this(value, var, itemLabel, itemValue, itemDisabled, itemRendered,
                null, null);
    }

    public WidgetSelectOptionsImpl(Object value, String var, String itemLabel,
            String itemValue, Object itemDisabled, Object itemRendered,
            String ordering, Boolean caseSensitive) {
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

    public Object getValue() {
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

    public Object getItemDisabled() {
        return itemDisabled;
    }

    public Object getItemRendered() {
        return itemRendered;
    }

    public String getOrdering() {
        return ordering;
    }

    public Boolean getCaseSensitive() {
        return caseSensitive;
    }

}
