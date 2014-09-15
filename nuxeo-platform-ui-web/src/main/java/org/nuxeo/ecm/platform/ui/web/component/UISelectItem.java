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

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;

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

    enum PropertyKeys {
        var, itemRendered,
    }

    public String getVar() {
        return (String) getStateHelper().eval(PropertyKeys.var);
    }

    public void setVar(String var) {
        getStateHelper().put(PropertyKeys.var, var);
    }

    @SuppressWarnings("boxing")
    public boolean isItemRendered() {
        return (Boolean) getStateHelper().eval(PropertyKeys.itemRendered, false);
    }

    @SuppressWarnings("boxing")
    public void setItemRendered(boolean itemRendered) {
        getStateHelper().put(PropertyKeys.itemRendered, itemRendered);
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
        if (!isItemRendered()) {
            return null;
        }
        Object value = getItemValue();
        Object labelObject = getItemLabel();
        String label = labelObject != null ? labelObject.toString() : null;
        // make sure label is never blank
        if (StringUtils.isBlank(label)) {
            label = String.valueOf(value);
        }
        return new SelectItem(value, label, null, isItemDisabled(),
                isItemEscaped());
    }

}