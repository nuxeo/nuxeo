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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.ui.web.component.ResettableComponent;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

import com.sun.faces.facelets.component.UIRepeat;

/**
 * DOCUMENT ME.
 * <p>
 * Refactor me and it's christmas.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class ChainSelect extends UIInput implements ResettableComponent {

    public static final String COMPONENT_TYPE = "nxdirectory.chainSelect";

    public static final String COMPONENT_FAMILY = "nxdirectory.chainSelect";

    public static final String DEFAULT_KEY_SEPARATOR = "/";

    public static final String DEFAULT_PARENT_KEY = null;

    private static final Log log = LogFactory.getLog(ChainSelect.class);

    // Direct access from ChainSelectStatus
    Map<Integer, NestedChainSelectComponentInfo> compInfos = new HashMap<Integer, NestedChainSelectComponentInfo>();

    /**
     * The keys of the selected items in chain controls.
     */
    private List<String> keyList = new ArrayList<String>();

    private String onchange;

    private Map<String, DirectorySelectItem>[] optionList;

    private Integer size;

    private boolean localize;

    private boolean multiSelect = false;

    private boolean allowRootSelection = false;

    private boolean allowBranchSelection = false;

    private boolean qualifiedParentKeys = false;

    private Selection[] selections;

    // XXX AT: this attribute is useless, value is already there to store that
    private Selection[] componentValue;

    private Boolean displayValueOnly;

    private String displayValueOnlyStyle;

    private String displayValueOnlyStyleClass;

    private String cssStyle;

    private String cssStyleClass;

    private boolean multiParentSelect = false;

    /**
     * The index of the last selection box that was selected.
     */
    private int lastSelectedComponentIndex;

    /**
     * This field is used to separate the levels of on hierarchical vocabulary.This way all parents of a record will be
     * separated through this field.
     */
    private String keySeparator;

    /**
     * Value used to filter on parent key when searching for a hierarchical directory roots.
     * <p>
     * If not set, will use null.
     */
    protected String defaultRootKey;

    /**
     * New attribute to handle bad behaviour on ajax re-render, forcing local cache refresh
     *
     * @since 5.6
     */
    protected Boolean resetCacheOnUpdate;

    public boolean isAllowBranchSelection() {
        return allowBranchSelection;
    }

    public void setAllowBranchSelection(boolean allowBranchSelection) {
        this.allowBranchSelection = allowBranchSelection;
    }

    public boolean isAllowRootSelection() {
        return allowRootSelection;
    }

    public void setAllowRootSelection(boolean allowRootSelection) {
        this.allowRootSelection = allowRootSelection;
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        componentValue = (Selection[]) values[1];
        optionList = (Map<String, DirectorySelectItem>[]) values[2];
        localize = (Boolean) values[3];
        size = (Integer) values[4];
        multiSelect = (Boolean) values[5];
        allowRootSelection = (Boolean) values[6];
        allowBranchSelection = (Boolean) values[7];
        selections = (Selection[]) values[8];
        qualifiedParentKeys = (Boolean) values[9];
        displayValueOnly = (Boolean) values[10];
        displayValueOnlyStyle = (String) values[11];
        displayValueOnlyStyleClass = (String) values[12];
        multiParentSelect = (Boolean) values[13];
        cssStyle = (String) values[14];
        cssStyleClass = (String) values[15];
        keySeparator = (String) values[16];
        lastSelectedComponentIndex = (Integer) values[17];
        compInfos = (Map<Integer, NestedChainSelectComponentInfo>) values[18];
        keyList = (List<String>) values[19];
        onchange = (String) values[20];
        defaultRootKey = (String) values[21];
        resetCacheOnUpdate = (Boolean) values[22];
    }

    @Override
    public Object saveState(FacesContext arg0) {
        Object[] values = new Object[23];
        values[0] = super.saveState(arg0);
        values[1] = componentValue;
        values[2] = optionList;
        values[3] = localize;
        values[4] = size;
        values[5] = multiSelect;
        values[6] = allowRootSelection;
        values[7] = allowBranchSelection;
        values[8] = selections;
        values[9] = qualifiedParentKeys;
        values[10] = displayValueOnly;
        values[11] = displayValueOnlyStyle;
        values[12] = displayValueOnlyStyleClass;
        values[13] = multiParentSelect;
        values[14] = cssStyle;
        values[15] = cssStyleClass;
        values[16] = keySeparator;
        values[17] = lastSelectedComponentIndex;
        values[18] = compInfos;
        values[19] = keyList;
        values[20] = onchange;
        values[21] = defaultRootKey;
        values[22] = resetCacheOnUpdate;
        return values;
    }

    public List<String> getSelectionKeyList() {
        return keyList;
    }

    public void addToSelectionKeyList(String key) {
        keyList.add(key);
    }

    @Override
    public void decode(FacesContext context) {
        if (getDisplayValueOnly()) {
            return;
        }

        setValid(true);
        rebuildOptions();

        if (!multiParentSelect) {
            componentValue = selections;
            String[] value = encodeValue(componentValue);
            if (!multiSelect) {
                setSubmittedValue(value[0]);
            } else {
                if (!multiParentSelect) {
                    // remove the "" entry from the submitted value
                    List<String> list = new ArrayList<String>(Arrays.asList(value));
                    list.remove("");
                    value = list.toArray(new String[list.size()]);
                }
                setSubmittedValue(value);
            }
        } else {
            String[] value = encodeValue(componentValue);
            setSubmittedValue(value);
        }

        // identify the repeat child tag that displays
        // current added selections to dynamically set
        // it's iterable value
        List<UIComponent> children = getChildren();
        for (UIComponent child : children) {
            if (!(child instanceof UIRepeat)) {
                continue;
            }
            UIRepeat component = (UIRepeat) child;
            if (component.getId().equals("current_selections")) {
                component.setValue(componentValue);
            }
        }
    }

    public static String format(Object o) {
        if (o == null) {
            return "NULL";
        }
        if (o instanceof String[]) {
            return formatAr((String[]) o);
        } else if (o instanceof String) {
            return (String) o;
        } else {
            return o.getClass().getName();
        }
    }

    public static String formatAr(String[] ar) {
        if (ar == null) {
            return "NULL";
        }
        if (ar.length == 0) {
            return "[]";
        } else {
            return '[' + StringUtils.join(ar, ", ") + ']';
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        init();
        rebuildOptions();
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("div", this);
        if (cssStyle != null) {
            writer.writeAttribute("style", cssStyle, "style");
        }
        if (cssStyleClass != null) {
            writer.writeAttribute("class", cssStyleClass, "class");
        }
        writer.writeAttribute("id", getClientId(context), "id");

        super.encodeBegin(context);
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("div");
    }

    public Object getProperty(String name) {
        ValueExpression ve = getValueExpression(name);
        if (ve != null) {
            try {
                return ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            Map<String, Object> attrMap = getAttributes();
            return attrMap.get(name);
        }
    }

    public String getStringProperty(String name, String defaultValue) {
        String value = (String) getProperty(name);
        return value != null ? value : defaultValue;
    }

    public Boolean getBooleanProperty(String name, boolean defaultValue) {
        Boolean value = (Boolean) getProperty(name);
        return value != null ? value : Boolean.valueOf(defaultValue);
    }

    public Boolean getLocalize() {
        return localize;
    }

    public void setLocalize(Boolean localize) {
        this.localize = localize;
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

    public void setCSsStyleClass(String cssStyleClass) {
        this.cssStyleClass = cssStyleClass;
    }

    public String getOnchange() {
        if (onchange != null) {
            return onchange;
        }
        ValueExpression ve = getValueExpression("onchange");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        }
        return null;
    }

    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }

    public Selection getSelection(int i) {
        if (selections == null) {
            throw new NuxeoException("ChainSelect is mis-behaving, it's probable you're experiencing issue NXP-5762");
        }
        return selections[i];
    }

    public void setSelections(Selection[] sels) {
        selections = sels;
    }

    public Integer getSize() {
        return size;
    }

    @SuppressWarnings("unchecked")
    public void setSize(Integer size) {
        optionList = new LinkedHashMap[size];
        this.size = size;
    }

    public Map<String, DirectorySelectItem> getOptions(int index) {
        return optionList[index];
    }

    public void setOptions(int index, Map<String, DirectorySelectItem> opts) {
        optionList[index] = opts;
    }

    /**
     * If the user changes selection for position k, all options for n>k will be reset. We only have to rebuild options
     * for position k+1.
     */
    public void rebuildOptions() {
        // for (int i = 0; i < size; i++) {
        // if (optionList[i] != null) {
        // continue;
        // }
        // if (i == 0
        // || (selections.length != 0 && selections[0].getColumnValue(i - 1) !=
        // null)) {
        // rebuildOptions(i);
        // }
        // }
    }

    public ChainSelectListboxComponent getComponent(UIComponent parent, int i) {
        ChainSelectListboxComponent c = null;
        Iterator<UIComponent> children = parent.getFacetsAndChildren();
        if (children != null) {
            UIComponent child = null;
            while (children.hasNext()) {
                child = (UIComponent) children.next();
                if (child instanceof ChainSelectListboxComponent) {
                    Integer index = ((ChainSelectListboxComponent) child).getIndex();
                    if (i == index) {
                        c = (ChainSelectListboxComponent) child;
                        break;
                    }
                } else {
                    // explore subcomps
                    c = getComponent(child, i);
                    if (c != null) {
                        break;
                    }
                }
            }
        }
        return c;
    }

    public ChainSelectListboxComponent getComponent(int i) {
        return getComponent(this, i);
    }

    public boolean isMultiSelect() {
        return multiSelect;
    }

    public void setMultiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
    }

    public Selection[] getSelections() {
        return selections;
    }

    public boolean isQualifiedParentKeys() {
        return qualifiedParentKeys;
    }

    public void setQualifiedParentKeys(boolean fullyQualifiedParentKey) {
        qualifiedParentKeys = fullyQualifiedParentKey;
    }

    public Boolean getDisplayValueOnly() {
        if (displayValueOnly != null) {
            return displayValueOnly;
        }
        return false;
    }

    public void setDisplayValueOnly(Boolean displayValueOnly) {
        this.displayValueOnly = displayValueOnly;
    }

    public String getDisplayValueOnlyStyle() {
        return displayValueOnlyStyle;
    }

    public void setDisplayValueOnlyStyle(String displayValueOnlyStyle) {
        this.displayValueOnlyStyle = displayValueOnlyStyle;
    }

    public String getDisplayValueOnlyStyleClass() {
        return displayValueOnlyStyleClass;
    }

    public void setDisplayValueOnlyStyleClass(String displayValueOnlyStyleClass) {
        this.displayValueOnlyStyleClass = displayValueOnlyStyleClass;
    }

    public boolean getMultiParentSelect() {
        return multiParentSelect;
    }

    public void setMultiParentSelect(boolean multiParentSelect) {
        this.multiParentSelect = multiParentSelect;
        if (multiParentSelect) {
            multiSelect = true;
        }
    }

    public String[] encodeValue(Selection[] selections) {
        String[] keys = new String[selections.length];
        for (int i = 0; i < selections.length; i++) {
            keys[i] = selections[i].getValue(keySeparator);
        }
        return keys;
    }

    private void init() {
        if (componentValue == null) {
            Object value = getValue();
            if (value == null) {
                componentValue = new Selection[0];
                selections = new Selection[1];
                selections[0] = new Selection(new DirectorySelectItem[0]);
                return;
            }
            String[] rows;
            if (multiSelect) {
                if (value instanceof String[]) {
                    rows = (String[]) value;
                } else if (value instanceof Object[]) {
                    Object[] values = (Object[]) value;
                    rows = new String[values.length];
                    for (int i = 0; i < rows.length; i++) {
                        rows[i] = String.valueOf(values[i]);
                    }
                } else if (value instanceof List) {
                    List valueList = (List) value;
                    rows = new String[valueList.size()];
                    for (int i = 0; i < rows.length; i++) {
                        rows[i] = String.valueOf(valueList.get(i));
                    }
                } else {
                    rows = new String[] {};
                }
            } else {
                rows = new String[] { (String) value };
            }

            componentValue = new Selection[rows.length];
            for (int i = 0; i < rows.length; i++) {
                String[] columns = StringUtils.split(rows[i], getKeySeparator());
                componentValue[i] = createSelection(columns);
            }

            if (multiParentSelect) {
                selections = new Selection[1];
                selections[0] = new Selection(new DirectorySelectItem[0]);
            } else {
                selections = componentValue;
            }
        }
    }

    public Selection createSelection(List<String> columns) {
        return createSelection(columns.toArray(new String[columns.size()]));
    }

    public Selection createSelection(String[] columns) {
        List<String> keyList = new ArrayList<String>();
        List<DirectorySelectItem> itemList = new ArrayList<DirectorySelectItem>();
        for (int i = 0; i < columns.length; i++) {
            String id = columns[i];

            String directoryName = null;
            VocabularyEntryList directoryValues = null;
            boolean displayObsoleteEntries = false;

            NestedChainSelectComponentInfo compInfo = compInfos.get(i);
            if (compInfo != null) {
                directoryName = compInfo.directoryName;
                directoryValues = compInfo.directoryValues;
                displayObsoleteEntries = compInfo.displayObsoleteEntries;
            } else {
                // fallback to the old solution
                ChainSelectListboxComponent comp = getComponent(i);
                if (comp != null) {
                    directoryName = comp.getStringProperty("directoryName", null);
                    directoryValues = comp.getDirectoryValues();
                    displayObsoleteEntries = comp.getBooleanProperty("displayObsoleteEntries", false);
                }
            }

            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("id", id);

            if (i == 0) {
                if (directoryName != null) {
                    if (DirectoryHelper.instance().hasParentColumn(directoryName)) {
                        filter.put("parent", getDefaultRootKey());
                    }
                }
            } else {
                String parentId;
                if (qualifiedParentKeys) {
                    parentId = StringUtils.join(keyList.iterator(), getKeySeparator());
                } else {
                    parentId = columns[i - 1];
                }
                filter.put("parent", parentId);
            }

            keyList.add(id);

            if (!displayObsoleteEntries) {
                filter.put("obsolete", 0);
            }
            List<DirectorySelectItem> items = null;
            if (directoryName != null) {
                items = DirectoryHelper.instance().getSelectItems(directoryName, filter);
            } else {
                items = DirectoryHelper.getSelectItems(directoryValues, filter);
            }
            if (items == null) {
                throw new IllegalStateException(String.format("Item not found: directoryName=%s, filter=%s",
                        directoryName, filter));
            }
            if (items.isEmpty()) {
                log.warn(String.format("No selection for dir %s ", directoryName));
                return new Selection(itemList.toArray(new DirectorySelectItem[0]));
            } else {
                if (items.size() != 1) {
                    log.warn(String.format("Too many items (%s) found: directoryName=%s, filter=%s",
                            Integer.toString(items.size()), directoryName, filter));
                }
                itemList.add(items.get(0));
            }
        }
        return new Selection(itemList.toArray(new DirectorySelectItem[columns.length]));
    }

    public Selection[] getComponentValue() {
        return componentValue;
    }

    public void setComponentValue(Selection[] componentValue) {
        this.componentValue = componentValue;
    }

    public int getLastSelectedComponentIndex() {
        return lastSelectedComponentIndex;
    }

    public void setLastSelectedComponentIndex(int index) {
        lastSelectedComponentIndex = index;
    }

    /**
     * This structure is needed to keep data for dynamically generated components.
     */
    static class NestedChainSelectComponentInfo {

        String directoryName;

        VocabularyEntryList directoryValues;

        boolean displayObsoleteEntries;

        boolean localize;

        String display;

    }

    public void setCompAtIndex(int index, ChainSelectListboxComponent comp) {

        NestedChainSelectComponentInfo compInfo = new NestedChainSelectComponentInfo();

        compInfo.directoryName = comp.getStringProperty("directoryName", null);
        compInfo.directoryValues = comp.getDirectoryValues();
        compInfo.displayObsoleteEntries = comp.getBooleanProperty("displayObsoleteEntries", false);
        compInfo.localize = comp.getBooleanProperty("localize", false);
        compInfo.display = comp.getDisplay();

        compInfos.put(index, compInfo);
    }

    public String getKeySeparator() {
        return keySeparator != null ? keySeparator : DEFAULT_KEY_SEPARATOR;
    }

    public void setKeySeparator(String keySeparator) {
        this.keySeparator = keySeparator;
    }

    public String getDefaultRootKey() {
        ValueExpression ve = getValueExpression("defaultRootKey");
        if (ve != null) {
            return (String) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return defaultRootKey;
        }
    }

    public void setDefaultRootKey(String defaultRootKey) {
        this.defaultRootKey = defaultRootKey;
    }

    @Override
    public void validateValue(FacesContext context, Object newValue) {
        super.validateValue(context, newValue);
        if (!isValid()) {
            return;
        }

        if (newValue instanceof String) {
            String newValueStr = (String) newValue;
            if (StringUtils.isEmpty(newValueStr)) {
                return;
            }

            String[] rows = StringUtils.split(newValueStr, getKeySeparator());
            boolean allowBranchSelection = Boolean.TRUE.equals(getBooleanProperty("allowBranchSelection", false));
            if (!allowBranchSelection && rows.length != size) {
                String messageStr = ComponentUtils.translate(context, "label.chainSelect.incomplete_selection");
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, messageStr, messageStr);
                context.addMessage(getClientId(context), message);
                setValid(false);
            }
        }
    }

    /**
     * @since 5.6
     */
    public Boolean getResetCacheOnUpdate() {
        if (resetCacheOnUpdate != null) {
            return resetCacheOnUpdate;
        }
        ValueExpression ve = getValueExpression("resetCacheOnUpdate");
        if (ve != null) {
            try {
                return Boolean.valueOf(Boolean.TRUE.equals(ve.getValue(getFacesContext().getELContext())));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return Boolean.FALSE;
        }
    }

    /**
     * @since 5.6
     */
    public void setResetCacheOnUpdate(Boolean resetCacheOnUpdate) {
        this.resetCacheOnUpdate = resetCacheOnUpdate;
    }

    /**
     * Override update method to reset cached value and ensure good re-render in ajax
     *
     * @since 5.6
     */
    @Override
    public void processUpdates(FacesContext context) {
        super.processUpdates(context);
        if (Boolean.TRUE.equals(getResetCacheOnUpdate()) && isValid()) {
            componentValue = new Selection[0];
        }
    }

    /**
     * Reset the chain select cached model
     *
     * @since 5.7
     */
    @Override
    public void resetCachedModel() {
        if (getValueExpression("value") != null) {
            setValue(null);
            setLocalValueSet(false);
        }
        setSubmittedValue(null);
        setComponentValue(null);
    }

}
