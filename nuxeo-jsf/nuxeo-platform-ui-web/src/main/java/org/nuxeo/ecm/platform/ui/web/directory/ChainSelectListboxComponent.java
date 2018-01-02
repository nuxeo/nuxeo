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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class ChainSelectListboxComponent extends UIInput implements ClientBehaviorHolder {

    public static final String COMPONENT_TYPE = "nxdirectory.chainSelectListbox";

    public static final String COMPONENT_FAMILY = "nxdirectory.chainSelectListbox";

    private static final Log log = LogFactory.getLog(ChainSelectListboxComponent.class);

    public boolean ajaxUpdated = false;

    private String directoryName;

    private VocabularyEntryList directoryValues;

    private Boolean displayIdAndLabel = false;

    private Boolean displayObsoleteEntries = false;

    private String onchange;

    private int index;

    private String displayIdAndLabelSeparator = " ";

    private String cssStyle;

    private String cssStyleClass;

    private String size;

    private Boolean localize = false;

    private String displayValueOnlySeparator;

    private String ordering;

    private String display;

    public ChainSelectListboxComponent() {
        setRendererType(COMPONENT_TYPE);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public boolean isMultiSelect() {
        ChainSelect chain = getChain();
        if (chain != null) {
            boolean isLastSelect = getIndex() == chain.getSize() - 1;
            boolean isChainMultiSelect = chain.getBooleanProperty("multiSelect", false);
            return isLastSelect && isChainMultiSelect;
        }
        return false;
    }

    public String getDisplayIdAndLabelSeparator() {
        return displayIdAndLabelSeparator;
    }

    public void setDisplayIdAndLabelSeparator(String displayIdAndLabelSeparator) {
        this.displayIdAndLabelSeparator = displayIdAndLabelSeparator;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        index = (Integer) values[1];
        displayIdAndLabel = (Boolean) values[2];
        displayIdAndLabelSeparator = (String) values[3];
        displayObsoleteEntries = (Boolean) values[4];
        ajaxUpdated = (Boolean) values[5];
        directoryName = (String) values[6];
        localize = (Boolean) values[7];
        displayValueOnlySeparator = (String) values[10];
        onchange = (String) values[11];
        cssStyle = (String) values[12];
        cssStyleClass = (String) values[13];
        size = (String) values[14];
        directoryValues = (VocabularyEntryList) values[15];
        ordering = (String) values[16];
        display = (String) values[17];
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[18];
        values[0] = super.saveState(context);
        values[1] = getIndex();
        values[2] = displayIdAndLabel;
        values[3] = displayIdAndLabelSeparator;
        values[4] = displayObsoleteEntries;
        values[5] = ajaxUpdated;
        values[6] = directoryName;
        values[7] = localize;
        values[10] = displayValueOnlySeparator;
        values[11] = onchange;
        values[12] = cssStyle;
        values[13] = cssStyleClass;
        values[14] = size;
        values[15] = directoryValues;
        values[16] = ordering;
        values[17] = display;
        return values;
    }

    public String getDirectoryName() {
        ValueBinding vb = getValueBinding("directoryName");
        if (vb != null) {
            return (String) vb.getValue(FacesContext.getCurrentInstance());
        } else {
            return directoryName;
        }
    }

    public void setDirectoryName(String newDirectory) {
        directoryName = newDirectory;
    }

    public VocabularyEntryList getDirectoryValues() {
        ValueBinding vb = getValueBinding("directoryValues");
        if (vb != null) {
            return (VocabularyEntryList) vb.getValue(FacesContext.getCurrentInstance());
        } else {
            return null;
        }
    }

    public void setDirectoryValues(VocabularyEntryList directoryValues) {
        this.directoryValues = directoryValues;
    }

    public Map<String, DirectorySelectItem> getOptions() {
        index = getIndex();
        if (index == 0 || getChain().getSelection(0).getColumnValue(index - 1) != null) {
            return rebuildOptions();
        }
        return new HashMap<String, DirectorySelectItem>();
    }

    public Boolean getDisplayIdAndLabel() {
        return displayIdAndLabel;
    }

    public void setDisplayIdAndLabel(Boolean displayIdAndLabel) {
        this.displayIdAndLabel = displayIdAndLabel;
    }

    public Boolean getDisplayObsoleteEntries() {
        return displayObsoleteEntries;
    }

    public void setDisplayObsoleteEntries(Boolean showObsolete) {
        displayObsoleteEntries = showObsolete;
    }

    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }

    public String getOnchange() {
        return onchange;
    }

    public ChainSelect getChain() {
        UIComponent component = getParent();
        while (component != null && !(component instanceof ChainSelect)) {
            component = component.getParent();
        }
        return (ChainSelect) component;
    }

    public Object getProperty(String name) {
        ValueBinding vb = getValueBinding(name);
        if (vb != null) {
            return vb.getValue(FacesContext.getCurrentInstance());
        } else {
            return getAttributes().get(name);
        }
    }

    public String getStringProperty(String name, String defaultValue) {
        String value = (String) getProperty(name);
        return value != null ? value : defaultValue;
    }

    public Boolean getBooleanProperty(String name, Boolean defaultValue) {
        Boolean value = (Boolean) getProperty(name);
        return value != null ? value : defaultValue;
    }

    public String getDisplayValueOnlySeparator() {
        return displayValueOnlySeparator;
    }

    public void setDisplayValueOnlySeparator(String displayValueOnlySeparator) {
        this.displayValueOnlySeparator = displayValueOnlySeparator;
    }

    public boolean isAjaxUpdated() {
        return ajaxUpdated;
    }

    public void setAjaxUpdated(boolean ajaxUpdated) {
        this.ajaxUpdated = ajaxUpdated;
    }

    /**
     * @return position of this component in the parent children list
     */
    public Integer getIndex() {
        // return index;
        ValueBinding vb = getValueBinding("index");
        if (vb != null) {
            return (Integer) vb.getValue(FacesContext.getCurrentInstance());
        } else {
            return index;
        }
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getCssStyle() {
        return cssStyle;
    }

    public void setCssStyle(String cssStyle) {
        this.cssStyle = cssStyle;
    }

    public String getCssStyleClass() {
        return cssStyleClass;
    }

    public void setCssStyleClass(String cssStyleClass) {
        this.cssStyleClass = cssStyleClass;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Boolean getLocalize() {
        return localize;
    }

    public void setLocalize(Boolean localize) {
        this.localize = localize;
    }

    /**
     * Reload listbox values based on previous selections in the chain. (functionality moved from ChainSelect)
     */
    public LinkedHashMap<String, DirectorySelectItem> rebuildOptions() {

        index = getIndex();

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        Boolean displayObsolete = getBooleanProperty("displayObsoleteEntries", Boolean.FALSE);
        if (!displayObsolete) {
            filter.put("obsolete", 0);
        }

        String directoryName = getDirectoryName();
        String defaultRootKey = getChain().getDefaultRootKey();
        if (index == 0) {
            if (directoryName != null) {
                if (DirectoryHelper.instance().hasParentColumn(directoryName)) {
                    filter.put("parent", defaultRootKey);
                }
            } else {
                filter.put("parent", defaultRootKey);
            }
        } else {
            boolean qualifiedParentKeys = getChain().isQualifiedParentKeys();
            String keySeparator = getChain().getKeySeparator();
            Selection sel = getChain().getSelections()[0];
            String parentValue = sel.getParentKey(index, qualifiedParentKeys, keySeparator);
            if (parentValue == null) {
                // use default parent key
                parentValue = defaultRootKey;
            }
            filter.put("parent", parentValue);
        }

        VocabularyEntryList directoryValues = getDirectoryValues();

        List<DirectorySelectItem> list;
        if (directoryName != null) {
            list = DirectoryHelper.instance().getSelectItems(directoryName, filter);
        } else {
            list = DirectoryHelper.getSelectItems(directoryValues, filter);
        }

        for (DirectorySelectItem item : list) {
            String id = (String) item.getValue();
            String label = item.getLabel();
            String translatedLabel = label;
            Boolean localize = getBooleanProperty("localize", Boolean.FALSE);
            if (localize) {
                translatedLabel = translate(label);
            }
            item.setLocalizedLabel(translatedLabel);

            String displayedLabel = translatedLabel;
            Boolean displayIdAndLabel = getBooleanProperty("displayIdAndLabel", Boolean.FALSE);
            if (displayIdAndLabel) {
                displayedLabel = id + displayIdAndLabelSeparator + label;
            }
            item.setDisplayedLabel(displayedLabel);
        }
        String ordering = getStringProperty("ordering", "");
        if (ordering != null && !"".equals(ordering)) {
            Collections.sort(list, new DirectorySelectItemComparator(ordering));
        }

        LinkedHashMap<String, DirectorySelectItem> options = new LinkedHashMap<String, DirectorySelectItem>();
        options.clear();
        for (DirectorySelectItem item : list) {
            options.put((String) item.getValue(), item);
        }

        return options;
    }

    private static String translate(String label) {
        FacesContext context = FacesContext.getCurrentInstance();
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }

    /**
     * This method reads submitted data and rebuilds the current list of values based on selections in the parent
     * components.
     */
    @Override
    public void decode(FacesContext context) {
        // FIXME: this code is nonsense, what it's doing and why is
        // perfectly unclear

        ChainSelect chain = getChain();
        if (chain.getDisplayValueOnly()) {
            return;
        }

        index = getIndex();

        chain.setCompAtIndex(index, this);
        List<String> keyList = chain.getSelectionKeyList();
        int size = chain.getSize();
        String formerValue = chain.getSelection(0).getColumnValue(index);

        if (index == 0) {
            chain.setLastSelectedComponentIndex(Integer.MAX_VALUE);
            keyList.clear();
        }

        if (chain.getLastSelectedComponentIndex() < index) {
            for (int i = index; i < size; i++) {
                chain.setOptions(i, null);
            }
            Map<String, DirectorySelectItem> options = rebuildOptions();
            chain.setOptions(index, options);
            return;
        }

        Map<String, String> requestMap = context.getExternalContext().getRequestParameterMap();
        Map<String, String[]> requestValueMap = context.getExternalContext().getRequestParameterValuesMap();

        String name = getClientId(context);

        String value = requestMap.get(name);
        if (value == null || value.length() == 0) {
            if (chain.getLastSelectedComponentIndex() > index) {
                chain.setLastSelectedComponentIndex(index);
            }

            for (int i = index; i < chain.getSize(); i++) {
                chain.setOptions(i, null);
            }
            Map<String, DirectorySelectItem> options = rebuildOptions();
            chain.setOptions(index, options);
        } else {
            keyList.add(value);
        }

        if (!StringUtils.equals(value, formerValue)) {
            chain.setLastSelectedComponentIndex(index);

            for (int i = index; i < chain.getSize(); i++) {
                chain.setOptions(i, null);
            }
        }

        String[] lastValues = requestValueMap.get(name);

        boolean lastValueIsOk = lastValues != null && lastValues.length != 0 && !StringUtils.isEmpty(lastValues[0]);

        Selection[] selections;

        boolean stop = chain.getLastSelectedComponentIndex() < index;
        if (index == size - 1 && lastValueIsOk && !stop) {
            String[] keyListArray = new String[size];
            selections = new Selection[lastValues.length];
            keyListArray = keyList.toArray(keyListArray);
            for (int i = 0; i < lastValues.length; i++) {
                keyListArray[size - 1] = lastValues[i];
                selections[i] = chain.createSelection(keyListArray);
            }
        } else {
            selections = new Selection[1];
            String[] columns = keyList.toArray(new String[0]);
            selections[0] = chain.createSelection(columns);
        }

        if (chain.getLastSelectedComponentIndex() == index) {
            chain.setSelections(selections);
        }

        Map<String, DirectorySelectItem> options = rebuildOptions();
        chain.setOptions(index, options);
    }

    public String getOrdering() {
        return ordering;
    }

    public void setOrdering(String ordering) {
        this.ordering = ordering;
    }

    public String getDisplay() {
        if (display != null) {
            return display;
        } else if (Boolean.TRUE.equals(displayIdAndLabel)) {
            return "idAndLabel";
        }
        return "label";
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    /**
     * @since 6.0
     */
    private static final Collection<String> EVENT_NAMES = Collections.unmodifiableCollection(Arrays.asList("blur",
            "change", "valueChange", "click", "dblclick", "focus", "keydown", "keypress", "keyup", "mousedown",
            "mousemove", "mouseout", "mouseover", "mouseup", "select"));

    @Override
    public Collection<String> getEventNames() {
        return EVENT_NAMES;
    }

    @Override
    public String getDefaultEventName() {
        return "valueChange";
    }

}
