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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.directory.SelectItemComparator;

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
public class UISelectItems extends javax.faces.component.UISelectItems {

    private static final Log log = LogFactory.getLog(UISelectItems.class);

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
                return (!Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext())));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return defaultValue;
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
        return createSelectItems(value);
    }

    @SuppressWarnings("unchecked")
    protected SelectItem[] createSelectItems(Object value) {
        List items = new ArrayList();

        if (value instanceof ListDataModel) {
            ListDataModel ldm = (ListDataModel) value;
            value = ldm.getWrappedData();
        }

        if (value instanceof SelectItem[]) {
            return (SelectItem[]) value;
        } else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            for (Object currentItem : array) {
                if (currentItem instanceof SelectItemGroup) {
                    SelectItemGroup itemGroup = (SelectItemGroup) currentItem;
                    SelectItem[] itemsFromGroup = itemGroup.getSelectItems();
                    items.addAll(Arrays.asList(itemsFromGroup));
                } else {
                    putIteratorToRequestParam(currentItem);
                    SelectItem selectItem = createSelectItem();
                    removeIteratorFromRequestParam();
                    if (selectItem != null) {
                        items.add(selectItem);
                    }
                }
            }
        } else if (value instanceof Collection) {
            Collection collection = (Collection) value;
            for (Object currentItem : collection) {
                if (currentItem instanceof SelectItemGroup) {
                    SelectItemGroup itemGroup = (SelectItemGroup) currentItem;
                    SelectItem[] itemsFromGroup = itemGroup.getSelectItems();
                    items.addAll(Arrays.asList(itemsFromGroup));
                } else {
                    putIteratorToRequestParam(currentItem);
                    SelectItem selectItem = createSelectItem();
                    removeIteratorFromRequestParam();
                    if (selectItem != null) {
                        items.add(selectItem);
                    }
                }
            }
        } else if (value instanceof Map) {
            Map map = (Map) value;
            for (Object obj : map.entrySet()) {
                Entry currentItem = (Entry) obj;
                putIteratorToRequestParam(currentItem.getValue());
                SelectItem selectItem = createSelectItem();
                removeIteratorFromRequestParam();
                if (selectItem != null) {
                    items.add(selectItem);
                }
            }
        } else if (value != null) {
            log.warn("Could not map values to select items, value is not supported: "
                    + value);
        }

        String ordering = getOrdering();
        Boolean caseSensitive = getCaseSensitive();
        if (ordering != null && !"".equals(ordering)) {
            Collections.sort(items, new SelectItemComparator(ordering,
                    caseSensitive));
        }
        return (SelectItem[]) items.toArray(new SelectItem[0]);
    }

    protected SelectItem createSelectItem() {
        Boolean rendered = getItemRendered();
        if (!rendered) {
            return null;
        }
        Object value = getItemValue();
        Object labelObject = getItemLabel();
        Boolean disabled = getItemDisabled();
        String label = labelObject != null ? labelObject.toString() : null;
        return new SelectItem(value, label, null,
                !Boolean.FALSE.equals(disabled));
    }

    protected void putIteratorToRequestParam(Object object) {
        String var = getVar();
        if (var != null) {
            FacesContext.getCurrentInstance().getExternalContext().getRequestMap().put(
                    var, object);
        }
    }

    protected void removeIteratorFromRequestParam() {
        String var = getVar();
        if (var != null) {
            FacesContext.getCurrentInstance().getExternalContext().getRequestMap().remove(
                    var);
        }
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
}
