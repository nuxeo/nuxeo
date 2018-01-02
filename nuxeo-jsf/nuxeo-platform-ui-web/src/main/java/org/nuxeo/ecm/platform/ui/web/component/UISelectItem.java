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

import java.util.Locale;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

/**
 * EasySelectItem from http://jsf-comp.sourceforge.net/components/easysi/index.html, adapted to work with single select
 * item.
 *
 * @author Cagatay-Mert
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UISelectItem extends javax.faces.component.UISelectItem {

    public static final String COMPONENT_TYPE = UISelectItem.class.getName();

    enum PropertyKeys {
        var, itemRendered, itemLabels, resolveItemLabelTwice,
        //
        localize, dbl10n;
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

    public boolean isResolveItemLabelTwice() {
        return Boolean.TRUE.equals(getStateHelper().eval(PropertyKeys.resolveItemLabelTwice, Boolean.FALSE));
    }

    @SuppressWarnings("boxing")
    public void setResolveItemLabelTwice(boolean resolveItemLabelTwice) {
        getStateHelper().put(PropertyKeys.resolveItemLabelTwice, resolveItemLabelTwice);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getItemLabels() {
        return (Map<String, String>) getStateHelper().eval(PropertyKeys.itemLabels);
    }

    public void setItemLabels(Map<String, String> itemLabels) {
        getStateHelper().put(PropertyKeys.itemLabels, itemLabels);
    }

    @SuppressWarnings("boxing")
    public boolean isLocalize() {
        return (Boolean) getStateHelper().eval(PropertyKeys.localize, Boolean.FALSE);
    }

    @SuppressWarnings("boxing")
    public void setLocalize(boolean localize) {
        getStateHelper().put(PropertyKeys.localize, localize);
    }

    @SuppressWarnings("boxing")
    public boolean isdbl10n() {
        return (Boolean) getStateHelper().eval(PropertyKeys.dbl10n, Boolean.FALSE);
    }

    @SuppressWarnings("boxing")
    public void setdbl10n(boolean dbl10n) {
        getStateHelper().put(PropertyKeys.dbl10n, dbl10n);
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

    protected String translate(FacesContext context, Locale locale, String label) {
        if (StringUtils.isBlank(label)) {
            return label;
        }
        String bundleName = context.getApplication().getMessageBundle();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }

    protected String retrieveItemLabel() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Locale locale = ctx.getViewRoot().getLocale();
        String label = null;
        if (isdbl10n()) {
            Map<String, String> labels = getItemLabels();
            if (labels != null) {
                if (labels.containsKey(locale.getLanguage())) {
                    label = labels.get(locale.getLanguage());
                } else {
                    // fallback on en
                    label = labels.get("en");
                }
            }
        }
        if (StringUtils.isBlank(label)) {
            Object labelObject = getItemLabel();
            label = labelObject != null ? labelObject.toString() : null;
        }
        if (isResolveItemLabelTwice() && ComponentTagUtils.isValueReference(label)) {
            ValueExpression ve = ctx.getApplication().getExpressionFactory().createValueExpression(ctx.getELContext(),
                    label, Object.class);
            if (ve != null) {
                Object newLabel = ve.getValue(ctx.getELContext());
                if (newLabel instanceof String) {
                    label = (String) newLabel;
                }
            }
        }
        if (isLocalize()) {
            label = translate(ctx, locale, label);
        }
        return label;
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
        return new SelectItem(value, label, null, isItemDisabled(), isItemEscaped());
    }

}