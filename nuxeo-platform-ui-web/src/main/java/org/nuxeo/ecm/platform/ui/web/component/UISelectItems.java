/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.platform.ui.web.component;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

/**
 * EasySelectItems from
 * http://jsf-comp.sourceforge.net/components/easysi/index.html, adapted to
 * work with jboss seam ListDataModel instances.
 * <p>
 * Adapted to handle ordering and disabling of select items.
 *
 * @author Cagatay-Mert
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UISelectItems extends javax.faces.component.UISelectItems
        implements ResettableComponent {

    public static final String COMPONENT_TYPE = UISelectItems.class.getName();

    protected String var;

    protected Object itemLabel;

    protected Object itemValue;

    protected Boolean itemRendered;

    protected Boolean itemDisabled;

    protected String ordering;

    protected Boolean caseSensitive;

    public String getVar() {
        if (var != null) {
            return var;
        }
        ValueExpression ve = getValueExpression("var");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setVar(String var) {
        this.var = var;
    }

    public Object getItemLabel() {
        if (itemLabel != null) {
            return itemLabel;
        }
        ValueExpression ve = getValueExpression("itemLabel");
        if (ve != null) {
            try {
                return ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setItemLabel(Object itemLabel) {
        this.itemLabel = itemLabel;
    }

    public Object getItemValue() {
        if (itemValue != null) {
            return itemValue;
        }
        ValueExpression ve = getValueExpression("itemValue");
        if (ve != null) {
            try {
                return ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setItemValue(Object itemValue) {
        this.itemValue = itemValue;
    }

    protected Boolean getBooleanValue(String name, boolean defaultValue) {
        ValueExpression ve = getValueExpression(name);
        if (ve != null) {
            try {
                return Boolean.valueOf(!Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext())));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return Boolean.valueOf(defaultValue);
        }
    }

    public Boolean getItemDisabled() {
        if (itemDisabled != null) {
            return itemDisabled;
        }
        return getBooleanValue("itemDisabled", false);
    }

    public void setItemDisabled(Boolean itemDisabled) {
        this.itemDisabled = itemDisabled;
    }

    public Boolean getItemRendered() {
        if (itemRendered != null) {
            return itemRendered;
        }
        return getBooleanValue("itemRendered", true);
    }

    public void setItemRendered(Boolean itemRendered) {
        this.itemRendered = itemRendered;
    }

    public String getOrdering() {
        if (ordering != null) {
            return ordering;
        }
        ValueExpression ve = getValueExpression("ordering");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return null;
        }
    }

    public Boolean getCaseSensitive() {
        if (caseSensitive != null) {
            return caseSensitive;
        }
        return getBooleanValue("caseSensitive", false);
    }

    public void setCaseSensitive(Boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public void setOrdering(String ordering) {
        this.ordering = ordering;
    }

    @Override
    public Object getValue() {
        Object value = super.getValue();
        return new SelectItemsFactory() {

            @Override
            protected String getVar() {
                return UISelectItems.this.getVar();
            }

            @Override
            protected String getOrdering() {
                return UISelectItems.this.getOrdering();
            }

            @Override
            protected Boolean getCaseSensitive() {
                return UISelectItems.this.getCaseSensitive();
            }

            @Override
            protected SelectItem createSelectItem() {
                return UISelectItems.this.createSelectItem();
            }

        }.createSelectItems(value);
    }

    protected SelectItem createSelectItem() {
        Boolean rendered = getItemRendered();
        if (!Boolean.TRUE.equals(rendered)) {
            return null;
        }
        Object value = getItemValue();
        Object labelObject = getItemLabel();
        String label = labelObject != null ? labelObject.toString() : null;
        Boolean disabled = getItemDisabled();
        return new SelectItem(value, label, null,
                !Boolean.FALSE.equals(disabled));
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[8];
        values[0] = super.saveState(context);
        values[1] = var;
        values[2] = itemLabel;
        values[3] = itemValue;
        values[4] = itemDisabled;
        values[5] = itemRendered;
        values[6] = ordering;
        values[7] = caseSensitive;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        var = (String) values[1];
        itemLabel = values[2];
        itemValue = values[3];
        itemDisabled = (Boolean) values[4];
        itemRendered = (Boolean) values[5];
        ordering = (String) values[6];
        caseSensitive = (Boolean) values[7];
    }

    /**
     * Reset the local value set, useful to reset cache on ajax action when
     * using a shuttle widget for instance.
     *
     * @since 5.7
     */
    @Override
    public void resetCachedModel() {
        setValue(null);
    }

}
