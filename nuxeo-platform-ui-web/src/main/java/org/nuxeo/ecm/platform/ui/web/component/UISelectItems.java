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

    enum PropertyKeys {
        var, itemLabel, itemValue, itemRendered, itemDisabled, itemEscaped, ordering, caseSensitive
    }

    public String getVar() {
        return (String) getStateHelper().eval(PropertyKeys.var);
    }

    public void setVar(String var) {
        getStateHelper().put(PropertyKeys.var, var);
    }

    public Object getItemLabel() {
        return getStateHelper().eval(PropertyKeys.itemLabel);
    }

    public void setItemLabel(Object itemLabel) {
        getStateHelper().put(PropertyKeys.itemLabel, itemLabel);
    }

    public Object getItemValue() {
        return getStateHelper().eval(PropertyKeys.itemValue);
    }

    public void setItemValue(Object itemValue) {
        getStateHelper().put(PropertyKeys.itemValue, itemValue);
    }

    public boolean isItemDisabled() {
        return Boolean.TRUE.equals(getStateHelper().eval(
                PropertyKeys.itemDisabled, Boolean.FALSE));
    }

    @SuppressWarnings("boxing")
    public void setItemDisabled(boolean itemDisabled) {
        getStateHelper().put(PropertyKeys.itemDisabled, itemDisabled);
    }

    public boolean isItemRendered() {
        return Boolean.TRUE.equals(getStateHelper().eval(
                PropertyKeys.itemRendered, Boolean.FALSE));
    }

    @SuppressWarnings("boxing")
    public void setItemRendered(boolean itemRendered) {
        getStateHelper().put(PropertyKeys.itemRendered, itemRendered);
    }

    public boolean isItemEscaped() {
        return Boolean.TRUE.equals(getStateHelper().eval(
                PropertyKeys.itemEscaped, Boolean.FALSE));
    }

    @SuppressWarnings("boxing")
    public void setItemEscaped(boolean itemEscaped) {
        getStateHelper().put(PropertyKeys.itemEscaped, itemEscaped);
    }

    public String getOrdering() {
        return (String) getStateHelper().eval(PropertyKeys.ordering);
    }

    public void setOrdering(String ordering) {
        getStateHelper().put(PropertyKeys.ordering, ordering);
    }

    public boolean isCaseSensitive() {
        return Boolean.TRUE.equals(getStateHelper().eval(
                PropertyKeys.caseSensitive));
    }

    @SuppressWarnings("boxing")
    public void setCaseSensitive(boolean caseSensitive) {
        getStateHelper().put(PropertyKeys.caseSensitive, caseSensitive);
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
            protected boolean isCaseSensitive() {
                return UISelectItems.this.isCaseSensitive();
            }

            @Override
            protected SelectItem createSelectItem() {
                return UISelectItems.this.createSelectItem();
            }

        }.createSelectItems(value);
    }

    protected SelectItem createSelectItem() {
        if (!isItemRendered()) {
            return null;
        }
        Object value = getItemValue();
        Object labelObject = getItemLabel();
        String label = labelObject != null ? labelObject.toString() : null;
        return new SelectItem(value, label, null, isItemDisabled(),
                isItemEscaped());
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
