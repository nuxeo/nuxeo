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
 * $Id: DirectoryEntryOutputComponent.java 23036 2007-07-27 11:34:11Z btatar $
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;

/**
 * Component to display a chained directory entry.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class ChainSelectOutputComponent extends UIOutput {

    public static final String COMPONENT_TYPE = "nxdirectory.ChainSelectOutput";

    public static final String COMPONENT_FAMILY = "nxdirectory.ChainSelectOutput";

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(ChainSelect.class);

    private static final String DISPLAY_ID_AND_LABEL_SEPARATOR = " ";

    private static final String DEFAULT_ENTRY_SEPARATOR = ", ";

    protected Boolean displayIdAndLabel;

    protected Boolean localize;

    protected String display;

    protected String directoryNameList;

    protected Boolean displayObsoleteEntries;

    protected String keySeparator;

    protected String displayKeySeparator;

    protected Boolean qualifiedParentKeys;

    protected Boolean handleMultipleValues = false;

    private String entrySeparator;

    private String cssStyle;

    private String cssStyleClass;

    public ChainSelectOutputComponent() {
        setRendererType(COMPONENT_TYPE);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public String getKeySeparator() {
        String ret = null;
        ValueBinding vb = getValueBinding("keySeparator");
        if (vb != null) {
            ret = (String) vb.getValue(getFacesContext());
        } else {
            ret = keySeparator;
        }
        if (ret == null) {
            ret = ChainSelect.DEFAULT_KEY_SEPARATOR;
        }
        return ret;
    }

    public void setDisplayKeySeparator(String keySeparator) {
        displayKeySeparator = keySeparator;
    }

    public String getDisplayKeySeparator() {
        String ret = null;
        ValueBinding vb = getValueBinding("displayKeySeparator");
        if (vb != null) {
            ret = (String) vb.getValue(getFacesContext());
        } else {
            ret = displayKeySeparator;
        }
        if (ret == null) {
            ret = getKeySeparator();
        }
        return ret;
    }

    public void setKeySeparator(String keySeparator) {
        this.keySeparator = keySeparator;
    }

    public boolean getQualifiedParentKeys() {
        Boolean ret = null;
        ValueBinding vb = getValueBinding("qualifiedParentKeys");
        if (vb != null) {
            ret = (Boolean) vb.getValue(getFacesContext());
        } else {
            ret = qualifiedParentKeys;
        }
        return Boolean.TRUE.equals(ret);
    }

    public boolean getHandleMultipleValues() {
        Boolean ret = null;
        ValueBinding vb = getValueBinding("handleMultipleValues");
        if (vb != null) {
            ret = (Boolean) vb.getValue(getFacesContext());
        } else {
            ret = handleMultipleValues;
        }
        return Boolean.TRUE.equals(ret);
    }

    public void setHandleMultipleValues(boolean handleMultipleValues) {
        this.handleMultipleValues = handleMultipleValues;
    }

    public void setQualifiedParentKeys(boolean qualifiedParentKeys) {
        this.qualifiedParentKeys = qualifiedParentKeys;
    }

    /**
     * @deprecated use display=id|label|idAndLabel instead
     */
    @Deprecated
    public boolean getDisplayIdAndLabel() {
        Boolean ret;
        ValueBinding vb = getValueBinding("displayIdAndLabel");
        if (vb != null) {
            ret = (Boolean) vb.getValue(getFacesContext());
        } else {
            ret = displayIdAndLabel;
        }
        return Boolean.TRUE.equals(ret);
    }

    public void setDisplayIdAndLabel(boolean displayIdAndLabel) {
        this.displayIdAndLabel = displayIdAndLabel;
    }

    public boolean getLocalize() {
        return Boolean.TRUE.equals(localize);
    }

    public void setLocalize(boolean localize) {
        this.localize = localize;
    }

    /**
     * Hide legacy "displayIdAndLabel" property. Use "display" if set; else if "displayIdAndLabel" is true, return
     * "idAndLabel", else default to "label".
     *
     * @return whether to display the id, the label or both
     */
    public String getDisplay() {
        String ret;
        ValueBinding vb = getValueBinding("display");
        if (vb != null) {
            ret = (String) vb.getValue(getFacesContext());
        } else {
            ret = display;
        }

        if (ret == null) {
            boolean displayIdAndLabel = getDisplayIdAndLabel();
            ret = displayIdAndLabel ? "idAndLabel" : "label";
        }

        return ret;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[12];
        values[0] = super.saveState(context);
        values[1] = displayIdAndLabel;
        values[2] = localize;
        values[3] = display;
        values[4] = displayObsoleteEntries;
        values[5] = directoryNameList;
        values[6] = qualifiedParentKeys;
        values[7] = keySeparator;
        values[8] = cssStyle;
        values[9] = cssStyleClass;
        values[10] = entrySeparator;
        values[11] = handleMultipleValues;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        displayIdAndLabel = (Boolean) values[1];
        localize = (Boolean) values[2];
        display = (String) values[3];
        displayObsoleteEntries = (Boolean) values[4];
        directoryNameList = (String) values[5];
        qualifiedParentKeys = (Boolean) values[6];
        keySeparator = (String) values[7];
        cssStyle = (String) values[8];
        cssStyleClass = (String) values[9];
        entrySeparator = (String) values[10];
        handleMultipleValues = (Boolean) values[11];
    }

    public boolean getDisplayObsoleteEntries() {
        return Boolean.TRUE.equals(displayObsoleteEntries);
    }

    /**
     * Transform a comma-separated list of keys into a selection. The list can be separated by the <b>keySeparator</b>
     * string
     *
     * @param keyEnum the comma-separated list of keys
     * @return
     */
    public Selection createSelection(String keyEnum) {
        String keySeparator = getKeySeparator();
        String[] columns = StringUtils.split(keyEnum, keySeparator);

        List<String> keyList = new ArrayList<>();
        List<DirectorySelectItem> itemList = new ArrayList<>();
        String directoryNameList = getDirectoryNameList();
        String[] directoryNames = StringUtils.split(directoryNameList, ",");
        boolean qualifiedParentKeys = getQualifiedParentKeys();
        boolean displayObsoleteEntries = getDisplayObsoleteEntries();
        String display = getDisplay();

        for (int i = 0; i < directoryNames.length; i++) {
            directoryNames[i] = directoryNames[i].trim();
        }

        for (int i = 0; i < columns.length; i++) {
            String id = columns[i];

            String directoryName = directoryNames[i];

            Map<String, Serializable> filter = new HashMap<>();
            filter.put("id", id);

            if (i == 0) {
                if (DirectoryHelper.instance().hasParentColumn(directoryName)) {
                    // explicitely filter on NULL parent in a xvocabulary
                    filter.put("parent", null);
                }
            } else {
                String parentId;
                if (qualifiedParentKeys) {
                    parentId = StringUtils.join(keyList.iterator(), keySeparator);
                } else {
                    parentId = columns[i - 1];
                }
                filter.put("parent", parentId);
            }

            keyList.add(id);

            if (!displayObsoleteEntries) {
                filter.put("obsolete", 0);
            }
            DirectorySelectItem item = DirectoryHelper.instance().getSelectItem(directoryName, filter);
            if (item == null) {
                item = new DirectorySelectItem(id, id);
            }
            String itemId = (String) item.getValue();
            String label = item.getLabel();
            if (getLocalize()) {
                label = translate(getFacesContext(), label);
            }
            if ("id".equals(display)) {
                label = id;
            } else if ("idAndLabel".equals(display)) {
                label = itemId + DISPLAY_ID_AND_LABEL_SEPARATOR + label;
            }
            item.setLabel(label);
            itemList.add(item);
        }
        return new Selection(itemList.toArray(new DirectorySelectItem[columns.length]));
    }

    protected static String translate(FacesContext context, String label) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }

    public String getEntrySeparator() {
        ValueBinding vb = getValueBinding("entrySeparator");
        if (vb != null) {
            vb.getValue(getFacesContext());
        }
        return entrySeparator == null ? DEFAULT_ENTRY_SEPARATOR : entrySeparator;
    }

    public void setEntrySeparator(String entrySeparator) {
        this.entrySeparator = entrySeparator;
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

    public void setDisplayObsoleteEntries(Boolean displayObsoleteEntries) {
        this.displayObsoleteEntries = displayObsoleteEntries;
    }

    public void setDisplayObsoleteEntries(boolean displayObsoleteEntries) {
        this.displayObsoleteEntries = displayObsoleteEntries;
    }

    public void setQualifiedParentKeys(Boolean qualifiedParentKeys) {
        this.qualifiedParentKeys = qualifiedParentKeys;
    }

    public String getDirectoryNameList() {
        String ret;
        ValueBinding vb = getValueBinding("directoryNameList");
        if (vb != null) {
            ret = (String) vb.getValue(getFacesContext());
        } else {
            ret = directoryNameList;
        }
        return ret;
    }

    public void setDirectoryNameList(String directoryNameList) {
        this.directoryNameList = directoryNameList;
    }

}
