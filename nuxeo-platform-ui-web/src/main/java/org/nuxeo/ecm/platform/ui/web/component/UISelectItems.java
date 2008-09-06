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

/**
 * EasySelectItems from
 * http://jsf-comp.sourceforge.net/components/easysi/index.html, adapted to work
 * with jboss seam ListDataModel instances.
 *
 * @author Cagatay-Mert
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UISelectItems extends javax.faces.component.UISelectItems {

    public static final String COMPONENT_TYPE = UISelectItems.class.getName();

    protected String var;

    protected Object itemLabel;

    protected Object itemValue;

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
                    items.add(selectItem);
                }
            }
        } else if (value instanceof Map) {
            Map map = (Map) value;
            for (Object obj : map.entrySet()) {
                Entry currentItem = (Entry) obj;
                putIteratorToRequestParam(currentItem.getValue());
                SelectItem selectItem = createSelectItem();
                removeIteratorFromRequestParam();
                items.add(selectItem);
            }
        }
        return (SelectItem[]) items.toArray(new SelectItem[0]);
    }

    protected SelectItem createSelectItem() {
        Object value = getItemValue();
        Object labelObject = getItemLabel();
        String label = labelObject != null ? labelObject.toString() : null;
        return new SelectItem(value, label);
    }

    protected void putIteratorToRequestParam(Object object) {
        FacesContext.getCurrentInstance().getExternalContext().getRequestMap().put(
                getVar(), object);
    }

    protected void removeIteratorFromRequestParam() {
        FacesContext.getCurrentInstance().getExternalContext().getRequestMap().remove(
                getVar());
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[4];
        values[0] = super.saveState(context);
        values[1] = var;
        values[2] = itemLabel;
        values[3] = itemValue;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        var = (String) values[1];
        itemLabel = values[2];
        itemValue = values[3];
    }

}
