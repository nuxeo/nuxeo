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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UISelectItem;
import javax.faces.component.html.HtmlSelectOneListbox;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public abstract class ChainSelectBase extends UIInput implements NamingContainer {

    private static final Log log = LogFactory.getLog(ChainSelect.class);

    protected static final String DISPLAY_LABEL = "label";

    protected static final String DISPLAY_ID = "id";

    protected static final String DISPLAY_IDLABEL = "idAndLabel";

    protected static final String DEFAULT_KEYSEPARATOR = "/";

    protected static final String SELECT = "selectListbox";

    public static final String VOCABULARY_SCHEMA = "vocabulary";

    /** Directory with a parent column. */
    public static final String XVOCABULARY_SCHEMA = "xvocabulary";

    /**
     * Parent column.
     *
     * @since 9.3
     */
    public static final String PARENT_COLUMN = "parent";

    protected String directoryNames;

    protected String keySeparator = DEFAULT_KEYSEPARATOR;

    protected boolean qualifiedParentKeys = false;

    protected int depth;

    protected String display = DISPLAY_LABEL;

    protected boolean translate;

    protected boolean showObsolete;

    protected String style;

    protected String styleClass;

    protected int listboxSize;

    protected boolean allowBranchSelection;

    protected String reRender;

    private boolean displayValueOnly;

    protected String defaultRootKey;

    protected Map<String, String[]> selectionMap = new HashMap<>();

    protected ChainSelectBase() {
        HtmlSelectOneListbox select = new HtmlSelectOneListbox();
        getFacets().put(SELECT, select);
    }

    public String getDirectory(int level) {
        String[] directories = getDirectories();
        if (isRecursive()) {
            return directories[0];
        } else {
            if (level < directories.length) {
                return directories[level];
            } else {
                return null;
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        ChainSelectState chainState = (ChainSelectState) values[1];
        selectionMap = (Map<String, String[]>) values[2];

        depth = chainState.getDepth();
        display = chainState.getDisplay();
        directoryNames = chainState.getDirectoryNames();
        keySeparator = chainState.getKeySeparator();
        qualifiedParentKeys = chainState.getQualifiedParentKeys();
        showObsolete = chainState.getShowObsolete();
        listboxSize = chainState.getListboxSize();
        style = chainState.getStyle();
        styleClass = chainState.getStyleClass();
        translate = chainState.getTranslate();
        allowBranchSelection = chainState.getAllowBranchSelection();
        reRender = chainState.getReRender();
        displayValueOnly = chainState.getDisplayValueOnly();
        defaultRootKey = chainState.getDefaultRootKey();
    }

    @Override
    public Object saveState(FacesContext context) {
        ChainSelectState chainState = new ChainSelectState();
        chainState.setDepth(depth);
        chainState.setDisplay(display);
        chainState.setDirectoryNames(directoryNames);
        chainState.setKeySeparator(keySeparator);
        chainState.setQualifiedParentKeys(qualifiedParentKeys);
        chainState.setShowObsolete(showObsolete);
        chainState.setStyle(style);
        chainState.setStyleClass(styleClass);
        chainState.setTranslate(translate);
        chainState.setListboxSize(listboxSize);
        chainState.setAllowBranchSelection(allowBranchSelection);
        chainState.setReRender(reRender);
        chainState.setDisplayValueOnly(displayValueOnly);
        chainState.setDefaultRootKey(defaultRootKey);

        Object[] values = new Object[3];
        values[0] = super.saveState(context);
        values[1] = chainState;
        values[2] = selectionMap;

        return values;
    }

    protected HtmlSelectOneListbox getListbox(FacesContext context, int level) {
        String componentId = getComponentId(level);

        HtmlSelectOneListbox listbox = new HtmlSelectOneListbox();
        getChildren().add(listbox);

        listbox.setId(componentId);
        listbox.getChildren().clear();

        String reRender = getReRender();
        if (reRender == null) {
            reRender = getId();
        }

        UIComponent support = context.getApplication().createComponent("org.ajax4jsf.ajax.Support");
        support.getAttributes().put("event", "onchange");
        support.getAttributes().put("reRender", reRender);
        support.getAttributes().put("immediate", Boolean.TRUE);
        support.getAttributes().put("id", componentId + "_a4jSupport");
        listbox.getChildren().add(support);

        return listbox;
    }

    protected void encodeListbox(FacesContext context, int level, String[] selectedKeys) throws IOException {
        HtmlSelectOneListbox listbox = getListbox(context, level);
        listbox.setSize(getListboxSize());

        List<DirectoryEntry> items;
        if (level <= selectedKeys.length) {
            items = getDirectoryEntries(level, selectedKeys);
        } else {
            items = new ArrayList<>();
        }

        UISelectItem emptyItem = new UISelectItem();
        emptyItem.setItemLabel(ComponentUtils.translate(context, "label.vocabulary.selectValue"));
        emptyItem.setItemValue("");
        emptyItem.setId(context.getViewRoot().createUniqueId());
        listbox.getChildren().add(emptyItem);

        for (DirectoryEntry child : items) {
            UISelectItem selectItem = new UISelectItem();
            String itemValue = child.getId();
            String itemLabel = child.getLabel();
            itemLabel = computeItemLabel(context, itemValue, itemLabel);

            selectItem.setItemValue(itemValue);
            selectItem.setItemLabel(itemLabel);
            selectItem.setId(context.getViewRoot().createUniqueId());
            listbox.getChildren().add(selectItem);
        }

        if (level < selectedKeys.length) {
            listbox.setValue(selectedKeys[level]);
        }

        ComponentUtils.encodeComponent(context, listbox);
    }

    public String[] getDirectories() {
        return StringUtils.split(getDirectoryNames(), ",");
    }

    public boolean isRecursive() {
        return getDirectories().length != getDepth();
    }

    /**
     * Computes the items that should be displayed for the nth listbox, depending on the options that have been selected
     * in the previous ones.
     *
     * @param level the index of the listbox for which to compute the items
     * @param selectedKeys the keys for the items selected on the previous levels
     * @return a list of directory items
     */
    public List<DirectoryEntry> getDirectoryEntries(int level, String[] selectedKeys) {

        assert level <= selectedKeys.length;

        List<DirectoryEntry> result = new ArrayList<>();
        String directoryName = getDirectory(level);

        DirectoryService service = DirectoryHelper.getDirectoryService();
        try (Session session = service.open(directoryName)) {
            String schema = service.getDirectorySchema(directoryName);
            Map<String, Serializable> filter = new HashMap<>();

            if (level == 0) {
                if (schema.equals(XVOCABULARY_SCHEMA)) {
                    filter.put(PARENT_COLUMN, null);
                }
            } else {
                if (getQualifiedParentKeys()) {
                    Iterator<String> iter = Arrays.asList(selectedKeys).subList(0, level).iterator();
                    String fullPath = StringUtils.join(iter, getKeySeparator());
                    filter.put(PARENT_COLUMN, fullPath);
                } else {
                    filter.put(PARENT_COLUMN, selectedKeys[level - 1]);
                }
            }

            if (!getShowObsolete()) {
                filter.put("obsolete", "0");
            }

            Set<String> emptySet = Collections.emptySet();
            Map<String, String> orderBy = new LinkedHashMap<>();

            // adding sorting suport
            if (schema.equals(VOCABULARY_SCHEMA) || schema.equals(XVOCABULARY_SCHEMA)) {
                orderBy.put("ordering", "asc");
                orderBy.put("id", "asc");
            }

            DocumentModelList entries = session.query(filter, emptySet, orderBy);
            for (DocumentModel entry : entries) {
                DirectoryEntry newNode = new DirectoryEntry(schema, entry);
                result.add(newNode);
            }
        }

        return result;
    }

    /**
     * Resolves a list of keys (a selection) to a list of coresponding directory items. Example: [a, b, c] is resolved
     * to [getNode(a), getNode(b), getNode(c)]
     *
     * @param keys
     * @return
     */
    public List<DirectoryEntry> resolveKeys(String[] keys) {
        List<DirectoryEntry> result = new ArrayList<>();

        DirectoryService service = DirectoryHelper.getDirectoryService();
        for (int level = 0; level < keys.length; level++) {
            String directoryName = getDirectory(level);
            try (Session session = service.open(directoryName)) {
                String schema = service.getDirectorySchema(directoryName);
                Map<String, Serializable> filter = new HashMap<>();

                if (level == 0) {
                    if (schema.equals(XVOCABULARY_SCHEMA)) {
                        filter.put(PARENT_COLUMN, null);
                    }
                } else {
                    if (getQualifiedParentKeys()) {
                        Iterator<String> iter = Arrays.asList(keys).subList(0, level).iterator();
                        String fullPath = StringUtils.join(iter, getKeySeparator());
                        filter.put(PARENT_COLUMN, fullPath);
                    } else {
                        filter.put(PARENT_COLUMN, keys[level - 1]);
                    }
                }
                filter.put("id", keys[level]);

                DocumentModelList entries = session.query(filter);
                if (entries == null || entries.isEmpty()) {
                    log.warn("keyList could not be resolved at level " + level);
                    break;
                }
                DirectoryEntry node = new DirectoryEntry(schema, entries.get(0));
                result.add(node);

            }
        }
        return result;
    }

    public String getComponentId(int level) {
        String directory = getDirectory(level);
        if (isRecursive()) {
            return directory + '_' + level;
        } else {
            return directory + '_' + level;
        }
    }

    public String getKeySeparator() {
        ValueExpression ve = getValueExpression("keySeparator");
        if (ve != null) {
            return (String) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return keySeparator;
        }
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

    public boolean getDisplayValueOnly() {
        ValueExpression ve = getValueExpression("displayValueOnly");
        if (ve != null) {
            Boolean value = (Boolean) ve.getValue(FacesContext.getCurrentInstance().getELContext());
            return value == null ? false : value;
        } else {
            return displayValueOnly;
        }
    }

    public void setDisplayValueOnly(boolean displayValueOnly) {
        this.displayValueOnly = displayValueOnly;
    }

    public int getListboxSize() {
        ValueExpression ve = getValueExpression("listboxSize");
        if (ve != null) {
            return (Integer) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return listboxSize;
        }
    }

    public void setListboxSize(int listboxSize) {
        this.listboxSize = listboxSize;
    }

    public String getDisplay() {
        ValueExpression ve = getValueExpression("display");
        if (ve != null) {
            return (String) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return display != null ? display : DISPLAY_LABEL;
        }
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public boolean getQualifiedParentKeys() {
        ValueExpression ve = getValueExpression("qualifiedParentKeys");
        if (ve != null) {
            return (Boolean) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return qualifiedParentKeys;
        }
    }

    public String getDirectoryNames() {
        ValueExpression ve = getValueExpression("directoryNames");
        if (ve != null) {
            return (String) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return directoryNames;
        }
    }

    public void setDirectoryNames(String directoryNames) {
        this.directoryNames = directoryNames;
    }

    public int getDepth() {
        int myDepth;
        ValueExpression ve = getValueExpression("depth");
        if (ve != null) {
            myDepth = (Integer) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            myDepth = depth;
        }

        return myDepth != 0 ? myDepth : getDirectories().length;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getStyle() {
        ValueExpression ve = getValueExpression("style");
        if (ve != null) {
            return (String) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return style;
        }
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getStyleClass() {
        ValueExpression ve = getValueExpression("styleClass");
        if (ve != null) {
            return (String) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return styleClass;
        }
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    public boolean getTranslate() {
        ValueExpression ve_translate = getValueExpression("translate");
        if (ve_translate != null) {
            return (Boolean) ve_translate.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return translate;
        }
    }

    public void setTranslate(boolean translate) {
        this.translate = translate;
    }

    public boolean getShowObsolete() {
        ValueExpression ve = getValueExpression("showObsolete");
        if (ve != null) {
            return (Boolean) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return showObsolete;
        }
    }

    public void setShowObsolete(boolean showObsolete) {
        this.showObsolete = showObsolete;
    }

    public boolean getAllowBranchSelection() {
        ValueExpression ve = getValueExpression("allowBranchSelection");
        if (ve != null) {
            return (Boolean) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return allowBranchSelection;
        }
    }

    public void setAllowBranchSelection(boolean allowBranchSelection) {
        this.allowBranchSelection = allowBranchSelection;
    }

    public String getReRender() {
        ValueExpression ve = getValueExpression("reRender");
        if (ve != null) {
            return (String) ve.getValue(FacesContext.getCurrentInstance().getELContext());
        } else {
            return reRender;
        }
    }

    public void setReRender(String reRender) {
        this.reRender = reRender;
    }

    protected String[] getValueAsArray(String value) {
        if (value == null) {
            return new String[0];
        }
        return StringUtils.split(value, getKeySeparator());
    }

    protected String getValueAsString(String[] ar) {
        return StringUtils.join(ar, getKeySeparator());
    }

    protected String computeItemLabel(FacesContext context, String id, String label) {
        boolean translate = getTranslate();
        String display = getDisplay();

        String translatedLabel = label;
        if (translate) {
            translatedLabel = ComponentUtils.translate(context, label);
        }

        if (DISPLAY_ID.equals(display)) {
            return id;
        } else if (DISPLAY_LABEL.equals(display)) {
            return translatedLabel;
        } else if (DISPLAY_IDLABEL.equals(display)) {
            return id + ' ' + translatedLabel;
        } else {
            throw new RuntimeException(
                    "invalid value for attribute 'display'; should be either 'id', 'label' or 'idAndLabel'");
        }
    }

    public abstract String[] getSelection();

    protected void decodeSelection(FacesContext context) {
        List<String> selectedKeyList = new ArrayList<>();
        Map<String, String> parameters = context.getExternalContext().getRequestParameterMap();

        String[] selection = getSelection();
        for (int level = 0; level < getDepth(); level++) {
            String clientId = getClientId(context) + SEPARATOR_CHAR + getComponentId(level);
            String value = parameters.get(clientId);
            if (StringUtils.isEmpty(value)) {
                break;
            }
            selectedKeyList.add(value);

            // compare the old value with the new one; if they differ
            // the new list of keys is finished
            if (level >= selection.length) {
                break;
            }
            String oldValue = selection[level];
            if (!value.equals(oldValue)) {
                break;
            }
        }
        selection = selectedKeyList.toArray(new String[selectedKeyList.size()]);
        setSelection(selection);
    }

    protected void setSelection(String[] selection) {
        String clientId = getClientId(FacesContext.getCurrentInstance());
        selectionMap.put(clientId, selection);
    }

    protected boolean validateEntry(FacesContext context, String[] keys) {
        if (!getAllowBranchSelection() && keys.length != getDepth()) {
            String messageStr = ComponentUtils.translate(context, "label.chainSelect.incomplete_selection");
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, messageStr, messageStr);
            context.addMessage(getClientId(context), message);
            setValid(false);
            return false;
        } else {
            return true;
        }
    }

}
