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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

/**
 * EasySelectItems from http://jsf-comp.sourceforge.net/components/easysi/index.html, adapted to work with jboss seam
 * ListDataModel instances.
 * <p>
 * Adapted to handle ordering and disabling of select items.
 *
 * @author Cagatay-Mert
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UISelectItems extends javax.faces.component.UISelectItems implements ResettableComponent {

    public static final String COMPONENT_TYPE = UISelectItems.class.getName();

    protected enum PropertyKeys {
        value, var, itemLabel, itemLabels, resolveItemLabelTwice,
        //
        itemLabelPrefix, itemLabelPrefixSeparator,
        //
        itemLabelSuffix, itemLabelSuffixSeparator,
        //
        itemValue, itemRendered, itemDisabled, itemEscaped,
        //
        ordering, caseSensitive,
        //
        displayIdAndLabel, displayIdAndLabelSeparator,
        //
        localize, dbl10n;
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

    @SuppressWarnings("unchecked")
    public Map<String, String> getItemLabels() {
        return (Map<String, String>) getStateHelper().eval(PropertyKeys.itemLabels);
    }

    public void setItemLabels(Map<String, String> itemLabels) {
        getStateHelper().put(PropertyKeys.itemLabels, itemLabels);
    }

    public String getItemLabelPrefix() {
        return (String) getStateHelper().eval(PropertyKeys.itemLabelPrefix);
    }

    public void setItemLabelPrefix(String itemLabelPrefix) {
        getStateHelper().put(PropertyKeys.itemLabelPrefix, itemLabelPrefix);
    }

    public String getItemLabelPrefixSeparator() {
        return (String) getStateHelper().eval(PropertyKeys.itemLabelPrefixSeparator, " ");
    }

    public void setItemLabelPrefixSeparator(String itemLabelPrefix) {
        getStateHelper().put(PropertyKeys.itemLabelPrefixSeparator, itemLabelPrefix);
    }

    public String getItemLabelSuffix() {
        return (String) getStateHelper().eval(PropertyKeys.itemLabelSuffix);
    }

    public void setItemLabelSuffix(String itemLabelSuffix) {
        getStateHelper().put(PropertyKeys.itemLabelSuffix, itemLabelSuffix);
    }

    public String getItemLabelSuffixSeparator() {
        return (String) getStateHelper().eval(PropertyKeys.itemLabelSuffixSeparator, " ");
    }

    public void setItemLabelSuffixSeparator(String itemLabelSuffix) {
        getStateHelper().put(PropertyKeys.itemLabelSuffixSeparator, itemLabelSuffix);
    }

    public Object getItemValue() {
        return getStateHelper().eval(PropertyKeys.itemValue);
    }

    public void setItemValue(Object itemValue) {
        getStateHelper().put(PropertyKeys.itemValue, itemValue);
    }

    public boolean isItemDisabled() {
        return Boolean.TRUE.equals(getStateHelper().eval(PropertyKeys.itemDisabled, Boolean.FALSE));
    }

    @SuppressWarnings("boxing")
    public void setItemDisabled(boolean itemDisabled) {
        getStateHelper().put(PropertyKeys.itemDisabled, itemDisabled);
    }

    public boolean isResolveItemLabelTwice() {
        return Boolean.TRUE.equals(getStateHelper().eval(PropertyKeys.resolveItemLabelTwice, Boolean.FALSE));
    }

    @SuppressWarnings("boxing")
    public void setResolveItemLabelTwice(boolean resolveItemLabelTwice) {
        getStateHelper().put(PropertyKeys.resolveItemLabelTwice, resolveItemLabelTwice);
    }

    public boolean isItemRendered() {
        return Boolean.TRUE.equals(getStateHelper().eval(PropertyKeys.itemRendered, Boolean.TRUE));
    }

    @SuppressWarnings("boxing")
    public void setItemRendered(boolean itemRendered) {
        getStateHelper().put(PropertyKeys.itemRendered, itemRendered);
    }

    public boolean isItemEscaped() {
        return Boolean.TRUE.equals(getStateHelper().eval(PropertyKeys.itemEscaped, Boolean.TRUE));
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
        return Boolean.TRUE.equals(getStateHelper().eval(PropertyKeys.caseSensitive));
    }

    @SuppressWarnings("boxing")
    public void setCaseSensitive(boolean caseSensitive) {
        getStateHelper().put(PropertyKeys.caseSensitive, caseSensitive);
    }

    public boolean isDisplayIdAndLabel() {
        return Boolean.TRUE.equals(getStateHelper().eval(PropertyKeys.displayIdAndLabel));
    }

    @SuppressWarnings("boxing")
    public void setDisplayIdAndLabel(boolean displayIdAndLabel) {
        getStateHelper().put(PropertyKeys.displayIdAndLabel, displayIdAndLabel);
    }

    public String getDisplayIdAndLabelSeparator() {
        return (String) getStateHelper().eval(PropertyKeys.displayIdAndLabelSeparator, " ");
    }

    public void setDisplayIdAndLabelSeparator(String idAndLabelSeparator) {
        getStateHelper().put(PropertyKeys.displayIdAndLabelSeparator, idAndLabelSeparator);
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
        List<SelectItem> items = new SelectItemsFactory() {
            @Override
            protected String getVar() {
                return UISelectItems.this.getVar();
            }

            @Override
            protected SelectItem createSelectItem() {
                return UISelectItems.this.createSelectItem();
            }

        }.createSelectItems(value);

        String ordering = getOrdering();
        boolean caseSensitive = isCaseSensitive();
        if (!StringUtils.isBlank(ordering)) {
            Collections.sort(items, new SelectItemComparator(ordering, Boolean.valueOf(caseSensitive)));
        }
        return items.toArray(new SelectItem[0]);
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
        String label = retrieveItemLabel();
        if (isDisplayIdAndLabel() && label != null) {
            label = value + getDisplayIdAndLabelSeparator() + label;
        }
        // make sure label is never blank
        if (StringUtils.isBlank(label)) {
            label = String.valueOf(value);
        }
        String labelPrefix = getItemLabelPrefix();
        if (!StringUtils.isBlank(labelPrefix)) {
            label = labelPrefix + getItemLabelPrefixSeparator() + label;
        }
        String labelSuffix = getItemLabelSuffix();
        if (!StringUtils.isBlank(labelSuffix)) {
            label = label + getItemLabelSuffixSeparator() + labelSuffix;
        }
        return new SelectItem(value, label, null, isItemDisabled(), isItemEscaped());
    }

    /**
     * Reset the local value set, useful to reset cache on ajax action when using a shuttle widget for instance.
     *
     * @since 5.7
     */
    @Override
    public void resetCachedModel() {
        setValue(null);
    }

}
