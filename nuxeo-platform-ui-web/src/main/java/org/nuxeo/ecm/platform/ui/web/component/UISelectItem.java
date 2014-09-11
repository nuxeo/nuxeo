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
        return new SelectItemFactory() {

            @Override
            protected String getVar() {
                return UISelectItem.this.getVar();
            }

            @Override
            protected SelectItem createSelectItem() {
                return UISelectItem.this.createSelectItem();
            }

        }.createSelectItem(value);
    }

    protected SelectItem createSelectItem() {
        Object value = getItemValue();
        Object labelObject = getItemLabel();
        String label = labelObject != null ? labelObject.toString() : null;
        return new SelectItem(value, label, null, false);
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
