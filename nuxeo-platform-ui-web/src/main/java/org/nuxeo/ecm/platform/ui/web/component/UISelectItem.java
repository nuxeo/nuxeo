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
 * EasySelectItem from
 * http://jsf-comp.sourceforge.net/components/easysi/index.html, adapted to
 * work with single select item.
 *
 * @author Cagatay-Mert
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UISelectItem extends javax.faces.component.UISelectItem {

    public static final String COMPONENT_TYPE = UISelectItem.class.getName();

    protected String var;

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

    @Override
    public Object getValue() {
        Object value = super.getValue();
        return createSelectItem(value);
    }

    /**
     * Returns the value exposed in request map for the var name.
     * <p>
     * This is useful for restoring this value in the request map.
     *
     * @since 5.4.2
     */
    protected final Object saveRequestMapVarValue() {
        String varName = getVar();
        return VariableManager.saveRequestMapVarValue(varName);
    }

    /**
     * Restores the given value in the request map for the var name.
     *
     * @since 5.4.2
     */
    protected final void restoreRequestMapVarValue(Object value) {
        String varName = getVar();
        VariableManager.restoreRequestMapVarValue(varName, value);
    }

    protected SelectItem createSelectItem(Object value) {
        SelectItem item = null;

        if (value instanceof SelectItem) {
            item = (SelectItem) value;
        } else {
            Object varValue = saveRequestMapVarValue();
            try {
                putIteratorToRequestParam(value);
                item = createSelectItem();
                removeIteratorFromRequestParam();
            } finally {
                restoreRequestMapVarValue(varValue);
            }
        }
        return item;
    }

    protected SelectItem createSelectItem() {
        Object value = getItemValue();
        String label = getItemLabel();
        return new SelectItem(value, label);
    }

    protected void putIteratorToRequestParam(Object object) {
        String var = getVar();
        VariableManager.putVariableToRequestParam(var, object);
    }

    protected void removeIteratorFromRequestParam() {
        String var = getVar();
        VariableManager.removeVariableFromRequestParam(var);
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[2];
        values[0] = super.saveState(context);
        values[1] = var;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        var = (String) values[1];
    }

}
