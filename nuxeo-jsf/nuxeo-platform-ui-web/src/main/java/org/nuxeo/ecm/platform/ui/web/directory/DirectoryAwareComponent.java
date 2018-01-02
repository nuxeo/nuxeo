/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: DirectoryAwareComponent.java 29914 2008-02-06 14:46:40Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;

/**
 * Directory-aware abstract component.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public abstract class DirectoryAwareComponent extends UIInput {

    protected String directoryName;

    protected Map<String, SelectItem> options;

    protected Boolean displayIdAndLabel;

    protected Boolean displayObsoleteEntries;

    protected Boolean localize;

    protected Boolean notDisplayDefaultOption;

    protected Boolean displayValueOnly;

    protected String displayValueOnlyStyle;

    protected String displayValueOnlyStyleClass;

    /**
     * This field is used to specify what to display from the entry of a directory,the id, label or both of them.
     */
    protected String display;

    protected String onchange;

    protected String onclick;

    protected String onselect;

    protected String filter;

    protected String size;

    protected String ordering;

    protected VocabularyEntryList directoryValues;

    protected Boolean caseSensitive;

    protected String getStringValue(String name, String defaultValue) {
        ValueExpression ve = getValueExpression(name);
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return defaultValue;
        }
    }

    protected Boolean getBooleanValue(String name, boolean defaultValue) {
        ValueExpression ve = getValueExpression(name);
        if (ve != null) {
            try {
                return !Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext()));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return defaultValue;
        }
    }

    public String getDirectoryName() {
        if (directoryName != null) {
            return directoryName;
        }
        return getStringValue("directoryName", null);
    }

    public void setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
    }

    public Boolean getDisplayIdAndLabel() {
        if (displayIdAndLabel != null) {
            return displayIdAndLabel;
        }
        return getBooleanValue("displayIdAndLabel", false);
    }

    public void setDisplayIdAndLabel(Boolean displayIdAndLabel) {
        this.displayIdAndLabel = displayIdAndLabel;
    }

    public Boolean getLocalize() {
        if (localize != null) {
            return localize;
        }
        return getBooleanValue("localize", false);
    }

    public void setLocalize(Boolean localize) {
        this.localize = localize;
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

    public Map<String, SelectItem> getOptions() {
        // rebuild systematically since a single component may be rendered
        // several times in the same page for different directories
        VocabularyEntryList directoryValues = getDirectoryValues();
        String directoryName = getDirectoryName();

        options = new LinkedHashMap<String, SelectItem>();
        if (StringUtils.isEmpty(directoryName) && directoryValues == null) {
            return options;
            // throw new RuntimeException("directoryName and directoryValues
            // cannot be both null");
        }

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        String parentFilter = getFilter();
        if (parentFilter != null) {
            filter.put("parent", parentFilter);
        }
        if (!getDisplayObsoleteEntries()) {
            filter.put("obsolete", 0);
        }

        List<DirectorySelectItem> optionList;
        if (!StringUtils.isEmpty(directoryName)) {
            optionList = DirectoryHelper.instance().getSelectItems(directoryName, filter, getLocalize());
        } else if (directoryValues != null) {
            optionList = DirectoryHelper.getSelectItems(directoryValues, filter, getLocalize());
        } else {
            optionList = new ArrayList<DirectorySelectItem>();
        }
        String ordering = getOrdering();
        Boolean caseSensitive = getCaseSensitive();
        if (ordering != null && !"".equals(ordering)) {
            Collections.sort(optionList, new DirectorySelectItemComparator(ordering, caseSensitive));
        }

        for (DirectorySelectItem item : optionList) {
            options.put((String) item.getValue(), item);
        }

        return options;
    }

    public void setOptions(Map<String, SelectItem> options) {
        this.options = options;
    }

    public Boolean getDisplayObsoleteEntries() {
        if (displayObsoleteEntries != null) {
            return displayObsoleteEntries;
        }
        return getBooleanValue("displayObsoleteEntries", false);
    }

    public void setDisplayObsoleteEntries(Boolean displayObsoleteEntries) {
        this.displayObsoleteEntries = displayObsoleteEntries;
    }

    public Boolean getNotDisplayDefaultOption() {
        if (notDisplayDefaultOption != null) {
            return notDisplayDefaultOption;
        }
        return getBooleanValue("notDisplayDefaultOption", false);
    }

    public void setNotDisplayDefaultOption(Boolean notDisplayDefaultOption) {
        this.notDisplayDefaultOption = notDisplayDefaultOption;
    }

    public Boolean getDisplayValueOnly() {
        if (displayValueOnly != null) {
            return displayValueOnly;
        }
        return getBooleanValue("displayValueOnly", false);
    }

    public void setDisplayValueOnly(Boolean displayValueOnly) {
        this.displayValueOnly = displayValueOnly;
    }

    public String getDisplayValueOnlyStyle() {
        if (displayValueOnlyStyle != null) {
            return displayValueOnlyStyle;
        }
        return getStringValue("displayValueOnlyStyle", null);
    }

    public void setDisplayValueOnlyStyle(String displayValueOnlyStyle) {
        this.displayValueOnlyStyle = displayValueOnlyStyle;
    }

    public String getDisplayValueOnlyStyleClass() {
        if (displayValueOnlyStyleClass != null) {
            return displayValueOnlyStyleClass;
        }
        return getStringValue("displayValueOnlyStyleClass", null);
    }

    public void setDisplayValueOnlyStyleClass(String displayValueOnlyStyleClass) {
        this.displayValueOnlyStyleClass = displayValueOnlyStyleClass;
    }

    public String getDisplay() {
        if (display != null) {
            return display;
        }
        return getStringValue("display", null);
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getOnchange() {
        if (onchange != null) {
            return onchange;
        }
        return getStringValue("onchange", null);
    }

    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }

    public String getOnclick() {
        if (onclick != null) {
            return onclick;
        }
        return getStringValue("onclick", null);
    }

    public void setOnclick(String onclick) {
        this.onclick = onclick;
    }

    public String getOnselect() {
        if (onselect != null) {
            return onselect;
        }
        return getStringValue("onselect", null);
    }

    public void setOnselect(String onselect) {
        this.onselect = onselect;
    }

    public String getFilter() {
        if (filter != null) {
            return filter;
        }
        return getStringValue("filter", null);
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getSize() {
        if (size != null) {
            return size;
        }
        return getStringValue("size", null);
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getOrdering() {
        if (ordering != null) {
            return ordering;
        }
        return getStringValue("ordering", "label");
    }

    public void setOrdering(String ordering) {
        this.ordering = ordering;
    }

    public VocabularyEntryList getDirectoryValues() {
        ValueExpression ve = getValueExpression("directoryValues");
        if (ve != null) {
            try {
                return (VocabularyEntryList) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return null;
        }
    }

    public void setDirectoryValues(VocabularyEntryList directoryValues) {
        this.directoryValues = directoryValues;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[19];
        values[0] = super.saveState(context);
        values[1] = directoryName;
        values[2] = options;
        values[3] = displayIdAndLabel;
        values[4] = displayObsoleteEntries;
        values[5] = localize;
        values[6] = notDisplayDefaultOption;
        values[7] = displayValueOnly;
        values[8] = displayValueOnlyStyle;
        values[9] = displayValueOnlyStyleClass;
        values[10] = display;
        values[11] = onchange;
        values[12] = filter;
        values[13] = size;
        values[14] = ordering;
        values[15] = directoryValues;
        values[16] = caseSensitive;
        values[17] = onclick;
        values[18] = onselect;
        return values;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        directoryName = (String) values[1];
        options = (Map<String, SelectItem>) values[2];
        displayIdAndLabel = (Boolean) values[3];
        displayObsoleteEntries = (Boolean) values[4];
        localize = (Boolean) values[5];
        notDisplayDefaultOption = (Boolean) values[6];
        displayValueOnly = (Boolean) values[7];
        displayValueOnlyStyle = (String) values[8];
        displayValueOnlyStyleClass = (String) values[9];
        display = (String) values[10];
        onchange = (String) values[11];
        filter = (String) values[12];
        size = (String) values[13];
        ordering = (String) values[14];
        directoryValues = (VocabularyEntryList) values[15];
        caseSensitive = (Boolean) values[16];
        onclick = (String) values[17];
        onselect = (String) values[18];
    }

    public Boolean getBooleanProperty(String key, Boolean defaultValue) {
        Map<String, Object> map = getAttributes();
        Boolean value = (Boolean) map.get(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public String getStringProperty(String key, String defaultValue) {
        Map<String, Object> map = getAttributes();
        String value = (String) map.get(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

}
