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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;

/**
 * @deprecated : component is useless (not declared correctly in deployment-fragment.xml and bugg) should be refactored
 *             instead
 */
@Deprecated
public class ChainSelectMultiListboxComponent extends UIInput {

    public static final String COMPONENT_TYPE = "nxdirectory.chainSelectMultiListbox";

    public static final String COMPONENT_FAMILY = "nxdirectory.chainSelectMultiListbox";

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(ChainSelectMultiListboxComponent.class);

    public boolean ajaxUpdated = false;

    private List<String> directoriesNames;

    private List<VocabularyEntryList> directoriesValues;

    private Boolean displayIdAndLabel;

    private Boolean displayObsoleteEntries = false;

    private String onchange;

    private int index;

    private String ordering;

    private String displayIdAndLabelSeparator = " ";

    private String cssStyle;

    private String cssStyleClass;

    private String size;

    private Boolean localize = false;

    private String displayValueOnlySeparator;

    private String display;

    public ChainSelectMultiListboxComponent() {
        setRendererType(COMPONENT_TYPE);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public boolean isMultiSelect() {
        ChainSelect chain = getChain();
        if (size != null && Integer.valueOf(size) < 2) {
            // this allows the last element to be a simple select box event
            // though the global chain is a multiselect thanks to some ajax add
            // button
            return false;
        }
        return index == chain.getSize() - 1 && chain.getBooleanProperty("multiSelect", false);
    }

    public String getDisplayIdAndLabelSeparator() {
        return displayIdAndLabelSeparator;
    }

    public void setDisplayIdAndLabelSeparator(String displayIdAndLabelSeparator) {
        this.displayIdAndLabelSeparator = displayIdAndLabelSeparator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        index = (Integer) values[1];
        displayIdAndLabel = (Boolean) values[2];
        displayIdAndLabelSeparator = (String) values[3];
        displayObsoleteEntries = (Boolean) values[4];
        ajaxUpdated = (Boolean) values[5];
        directoriesNames = (List<String>) values[6];
        localize = (Boolean) values[7];
        displayValueOnlySeparator = (String) values[10];
        onchange = (String) values[11];
        cssStyle = (String) values[12];
        cssStyleClass = (String) values[13];
        size = (String) values[14];
        directoriesValues = (List<VocabularyEntryList>) values[15];
        ordering = (String) values[16];
        display = (String) values[17];
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[18];
        values[0] = super.saveState(context);
        values[1] = index;
        values[2] = displayIdAndLabel;
        values[3] = displayIdAndLabelSeparator;
        values[4] = displayObsoleteEntries;
        values[5] = ajaxUpdated;
        values[6] = directoriesNames;
        values[7] = localize;
        values[10] = displayValueOnlySeparator;
        values[11] = onchange;
        values[12] = cssStyle;
        values[13] = cssStyleClass;
        values[14] = size;
        values[15] = directoriesValues;
        values[16] = ordering;
        values[17] = display;
        return values;
    }

    public List<String> getDirectoriesNamesArray() {
        return directoriesNames;
    }

    public String getDirectoriesNames() {
        // concat dir names
        StringBuilder buf = new StringBuilder();
        String comma = "";
        for (String directoryName : directoriesNames) {
            buf.append(comma);
            comma = ",";
            buf.append(directoryName);
        }
        return buf.toString();
    }

    public void setDirectoriesNames(String newDirectiriesNames) {
        if (newDirectiriesNames == null) {
            throw new IllegalArgumentException("null newDirectiriesNames");
        }
        directoriesNames = Arrays.asList(newDirectiriesNames.split(","));
    }

    /*
     * public List<VocabularyEntryList> getDirectoriesValues() { List<VocabularyEntryList> list = new
     * ArrayList<VocabularyEntryList>(); ValueBinding vb = getValueBinding("directoryValues"); if (vb != null) {
     * list.add( (VocabularyEntryList) vb.getValue(FacesContext.getCurrentInstance()) ); } else { //return null; }
     * return list; }
     */
    public void setDirectoriesValues(List<VocabularyEntryList> directoriesValues) {
        this.directoriesValues = directoriesValues;
    }

    public Map<String, DirectorySelectItem> getOptions() {
        return getChain().getOptions(index);
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

    public String getOrdering() {
        return ordering;
    }

    public void setOrdering(String sortCriteria) {
        ordering = sortCriteria;
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

    public Integer getIndex() {
        return index;
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

    // /
    public ChainSelectListboxComponent[] createSingleComponents() {

        final int ncomp = directoriesNames.size();
        ChainSelectListboxComponent[] sComps = new ChainSelectListboxComponent[ncomp];

        int i = 0;
        for (String dirName : directoriesNames) {
            ChainSelectListboxComponent comp = new ChainSelectListboxComponent();

            comp.setId(getId() + i);
            comp.setIndex(i);
            comp.setDirectoryName(dirName);
            comp.setCssStyle(cssStyle);
            comp.setCssStyleClass(cssStyleClass);
            comp.setLocalize(localize);
            comp.setSize(size);

            String id1 = getId().split(":")[0];
            String id2 = getId();
            // XXX AT: yeah, right, that seems to be a very good idea.
            // "A4J.AJAX.Submit('_viewRoot','j_id202',event,{'parameters':{'j_id202:j_id286':'j_id202:j_id286'},'actionUrl':'/nuxeo/documents/tabs/document_externe_edit.faces'})";
            onchange = "A4J.AJAX.Submit('_viewRoot','" + id1 + "',event,{'parameters':{'" + id2 + "':'" + id2
                    + "'},'actionUrl':'" + BaseURL.getContextPath() + "/documents/tabs/document_externe_edit.faces'})";

            comp.setOnchange(onchange);

            // VocabularyEntryList directoryValues =
            // getDirectoriesValues().get(0);
            // comp.setDirectoryValues(directoryValues);

            comp.setParent(getParent());

            final List<UIComponent> children = comp.getChildren();
            children.addAll(getChildren());

            sComps[i++] = comp;
        }

        return sComps;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    /*
     * @Override public boolean getRendersChildren() { return true; }
     */
}
