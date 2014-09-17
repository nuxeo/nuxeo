/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: UIDirectorySelectItems.java 29556 2008-01-23 00:59:39Z jcarsique $
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.component.UISelectItems;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

/**
 * Component that deals with a list of select items from a directory.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIDirectorySelectItems extends UISelectItems {

    public static final String COMPONENT_TYPE = UIDirectorySelectItems.class.getName();

    private static final Log log = LogFactory.getLog(UIDirectorySelectItems.class);

    enum PropertyKeys {
        value
    }

    enum DirPropertyKeys {
        directoryName, keySeparator, itemOrdering, allValues,
        //
        displayAll, displayObsoleteEntries, filter, localize, dbl10n;
    }

    // setters & getters

    public Long getItemOrdering() {
        return (Long) getStateHelper().eval(DirPropertyKeys.itemOrdering);
    }

    public void setItemOrdering(Long itemOrdering) {
        getStateHelper().put(DirPropertyKeys.itemOrdering, itemOrdering);
    }

    public String getKeySeparator() {
        return (String) getStateHelper().eval(DirPropertyKeys.keySeparator);
    }

    public void setKeySeparator(String keySeparator) {
        getStateHelper().put(DirPropertyKeys.keySeparator, keySeparator);
    }

    public String getDirectoryName() {
        return (String) getStateHelper().eval(DirPropertyKeys.directoryName);
    }

    public void setDirectoryName(String directoryName) {
        getStateHelper().put(DirPropertyKeys.directoryName, directoryName);
    }

    public SelectItem[] getAllValues() {
        return (SelectItem[]) getStateHelper().eval(DirPropertyKeys.allValues);
    }

    public void setAllValues(SelectItem[] allValues) {
        getStateHelper().put(DirPropertyKeys.allValues, allValues);
    }

    @SuppressWarnings("boxing")
    public boolean isDisplayAll() {
        return (Boolean) getStateHelper().eval(DirPropertyKeys.displayAll,
                Boolean.TRUE);
    }

    @SuppressWarnings("boxing")
    public void setDisplayAll(boolean displayAll) {
        getStateHelper().put(DirPropertyKeys.displayAll, displayAll);
    }

    @SuppressWarnings("boxing")
    public boolean isDisplayObsoleteEntries() {
        return (Boolean) getStateHelper().eval(
                DirPropertyKeys.displayObsoleteEntries, Boolean.FALSE);
    }

    @SuppressWarnings("boxing")
    public void setDisplayObsoleteEntries(boolean displayObsoleteEntries) {
        getStateHelper().put(DirPropertyKeys.displayObsoleteEntries,
                displayObsoleteEntries);
    }

    public String getFilter() {
        return (String) getStateHelper().eval(DirPropertyKeys.filter);
    }

    public void setFilter(String filter) {
        getStateHelper().put(DirPropertyKeys.filter, filter);
    }

    @SuppressWarnings("boxing")
    public boolean isLocalize() {
        return (Boolean) getStateHelper().eval(DirPropertyKeys.localize,
                Boolean.FALSE);
    }

    @SuppressWarnings("boxing")
    public void setLocalize(boolean localize) {
        getStateHelper().put(DirPropertyKeys.localize, localize);
    }

    @SuppressWarnings("boxing")
    public boolean isdbl10n() {
        return (Boolean) getStateHelper().eval(DirPropertyKeys.dbl10n,
                Boolean.FALSE);
    }

    @SuppressWarnings("boxing")
    public void setdbl10n(boolean dbl10n) {
        getStateHelper().put(DirPropertyKeys.dbl10n, dbl10n);
    }

    @Override
    public Object getValue() {
        DirectorySelectItemsFactory f = new DirectorySelectItemsFactory() {

            @Override
            protected String getVar() {
                return UIDirectorySelectItems.this.getVar();
            }

            @Override
            protected DirectorySelectItem createSelectItem() {
                return UIDirectorySelectItems.this.createSelectItem();
            }

            @Override
            protected String retrieveSelectEntryId() {
                return UIDirectorySelectItems.this.retrieveSelectEntryId();
            }

            @Override
            protected String getOrdering() {
                return UIDirectorySelectItems.this.getOrdering();
            }

            @Override
            protected boolean isCaseSensitive() {
                return UIDirectorySelectItems.this.isCaseSensitive();
            }

            @Override
            protected boolean isDisplayObsoleteEntries() {
                return UIDirectorySelectItems.this.isDisplayObsoleteEntries();
            }

            @Override
            protected String getDirectoryName() {
                return UIDirectorySelectItems.this.getDirectoryName();
            }

            @Override
            protected String getKeySeparator() {
                return UIDirectorySelectItems.this.getKeySeparator();
            }

            @Override
            protected String getFilter() {
                return UIDirectorySelectItems.this.getFilter();
            }

        };

        if (isDisplayAll()) {
            setAllValues(f.createAllSelectItems());
            return getAllValues();
        } else {
            Object value = getStateHelper().eval(PropertyKeys.value);
            return f.createSelectItems(value);
        }
    }

    protected String retrieveSelectEntryId() {
        return (String) getItemValue();
    }

    @Override
    protected DirectorySelectItem createSelectItem() {
        if (!isItemRendered()) {
            return null;
        }
        DocumentModel docEntry = null;
        FacesContext ctx = FacesContext.getCurrentInstance();
        Object entry = ComponentTagUtils.resolveElExpression(ctx,
                String.format("#{%s}", getVar()));
        String schema = null;
        if (entry instanceof DocumentModel) {
            docEntry = (DocumentModel) entry;
            schema = docEntry.getSchemas()[0];
        } else {
            return null;
        }
        // entry id might have been resolved thanks to item value, so let's
        // use directly the doc entry id for option value
        String value = docEntry.getId();
        if (value == null) {
            return null;
        }
        Object labelObject = getItemLabel();
        String label = labelObject != null ? labelObject.toString() : null;
        // lookup label property, hardcode the "label_" prefix for now
        if (label == null && docEntry != null) {
            // fallback on directory default label
            label = (String) docEntry.getProperties(schema).get("label");
        }
        // compute ordering
        Long ordering = getItemOrdering();
        if (ordering == null && docEntry != null) {
            try {
                // fallback on default ordering key
                ordering = (Long) docEntry.getProperties(schema).get("ordering");
            } catch (ClassCastException e) {
                // nevermind
            }
        }
        if (isLocalize() && label != null) {
            Locale locale = ctx.getViewRoot().getLocale();
            if (isdbl10n()) {
                // lookup label property, hardcode the "label_" prefix for now
                if (entry instanceof DocumentModel) {
                    // resolve property key
                    String defaultPattern = "label_en";
                    String pattern = "label_" + locale.getCountry();
                    if (docEntry.getProperties(schema).containsKey(pattern)) {
                        label = (String) docEntry.getProperties(schema).get(
                                pattern);
                    } else {
                        label = (String) docEntry.getProperties(schema).get(
                                defaultPattern);
                    }
                } else {
                    log.error("Could not compute translated label for entry "
                            + value);
                }
            } else {
                label = translate(ctx, locale, label);
            }
        }
        if (isDisplayIdAndLabel() && label != null) {
            label = value + getDisplayIdAndLabelSeparator() + label;
        }
        // make sure label is never blank
        if (StringUtils.isBlank(label)) {
            label = value;
        }
        String labelPrefix = getItemLabelPrefix();
        if (!StringUtils.isBlank(labelPrefix)) {
            label = labelPrefix + getItemLabelPrefixSeparator() + label;
        }
        String labelSuffix = getItemLabelSuffix();
        if (!StringUtils.isBlank(labelSuffix)) {
            label = label + getItemLabelSuffixSeparator() + labelSuffix;
        }
        return new DirectorySelectItem(value, label, ordering == null ? 0L
                : ordering.longValue(), isItemDisabled(), isItemEscaped());
    }

    protected String translate(FacesContext context, Locale locale, String label) {
        String bundleName = context.getApplication().getMessageBundle();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }

}