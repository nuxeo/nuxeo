/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.jsf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.core.ReferencePageProviderDescriptor;

/**
 * Descriptor for content view registration.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("contentView")
public class ContentViewDescriptor {

    @XNode("@name")
    String name;

    @XNode("@enabled")
    boolean enabled = true;

    @XNode("title")
    String title;

    @XNode("translateTitle")
    Boolean translateTitle;

    @XNode("emptySentence")
    String emptySentence;

    /**
     * @since 7.4
     */
    @XNode("waitForExecutionSentence")
    String waitForExecutionSentence;

    @XNode("translateEmptySentence")
    Boolean translateEmptySentence;

    @XNode("iconPath")
    String iconPath;

    @XNode("coreQueryPageProvider")
    CoreQueryPageProviderDescriptor coreQueryPageProvider;

    @XNode("genericPageProvider")
    GenericPageProviderDescriptor genericPageProvider;

    @XNode("pageProvider")
    ReferencePageProviderDescriptor referencePageProvider;

    @XNode("selectionList")
    String selectionList;

    @XNode("pagination")
    String pagination;

    @XNodeList(value = "actions@category", type = ArrayList.class, componentType = String.class)
    List<String> actionCategories;

    @XNode("searchDocument")
    String searchDocument;

    @XNode("searchLayout")
    ContentViewLayoutImpl searchLayout;

    @XNode("resultLayouts@append")
    Boolean appendResultLayouts;

    @XNodeList(value = "resultLayouts/layout", type = ArrayList.class, componentType = ContentViewLayoutImpl.class)
    List<ContentViewLayout> resultLayouts;

    @XNode("resultColumns")
    String resultColumns;

    /**
     * @since 6.0
     */
    @XNode("resultLayout")
    String resultLayout;

    @XNodeList(value = "flags/flag", type = ArrayList.class, componentType = String.class)
    List<String> flags;

    @XNode("cacheKey")
    String cacheKey;

    @XNode("cacheSize")
    Integer cacheSize;

    @XNode("useGlobalPageSize")
    Boolean useGlobalPageSize;

    @XNode("showTitle")
    Boolean showTitle;

    @XNode("showPageSizeSelector")
    Boolean showPageSizeSelector;

    @XNode("showRefreshCommand")
    Boolean showRefreshCommand;

    @XNode("showFilterForm")
    Boolean showFilterForm;

    /**
     * @since 7.4
     */
    @XNode("waitForExecution")
    Boolean waitForExecution;

    @XNodeList(value = "refresh/event", type = ArrayList.class, componentType = String.class)
    List<String> refreshEventNames;

    @XNodeList(value = "reset/event", type = ArrayList.class, componentType = String.class)
    List<String> resetEventNames;

    public String getName() {
        return name;
    }

    public CoreQueryPageProviderDescriptor getCoreQueryPageProvider() {
        return coreQueryPageProvider;
    }

    public GenericPageProviderDescriptor getGenericPageProvider() {
        return genericPageProvider;
    }

    public ReferencePageProviderDescriptor getReferencePageProvider() {
        return referencePageProvider;
    }

    // @since 6.0
    protected String pageProviderName;

    // @since 6.0
    protected Map<String, String> pageProviderProperties;

    public String getSelectionListName() {
        return selectionList;
    }

    public String getPagination() {
        return pagination;
    }

    public List<String> getActionCategories() {
        return actionCategories;
    }

    public ContentViewLayoutImpl getSearchLayout() {
        return searchLayout;
    }

    public Boolean getAppendResultLayouts() {
        return appendResultLayouts;
    }

    public List<ContentViewLayout> getResultLayouts() {
        return resultLayouts;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public Integer getCacheSize() {
        return cacheSize;
    }

    public List<String> getRefreshEventNames() {
        return refreshEventNames;
    }

    public List<String> getResetEventNames() {
        return resetEventNames;
    }

    public Boolean getUseGlobalPageSize() {
        return useGlobalPageSize;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getTitle() {
        return title;
    }

    public Boolean getTranslateTitle() {
        return translateTitle;
    }

    public String getSearchDocumentBinding() {
        return searchDocument;
    }

    public String getResultColumnsBinding() {
        return resultColumns;
    }

    /**
     * @since 6.0
     */
    public String getResultLayoutBinding() {
        return resultLayout;
    }

    public List<String> getFlags() {
        return flags;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @since 5.4.2
     */
    public Boolean getShowTitle() {
        return showTitle;
    }

    /**
     * @since 5.4.2
     */
    public Boolean getShowPageSizeSelector() {
        if (showPageSizeSelector == null) {
            // default value
            return Boolean.TRUE;
        }
        return showPageSizeSelector;
    }

    /**
     * @since 5.4.2
     */
    public Boolean getShowRefreshCommand() {
        if (showRefreshCommand == null) {
            // default value
            return Boolean.TRUE;
        }
        return showRefreshCommand;
    }

    /**
     * @since 5.4.2
     */
    public Boolean getShowFilterForm() {
        return showFilterForm;
    }

    /**
     * @since 5.4.2
     */
    public String getEmptySentence() {
        return emptySentence;
    }

    /**
     * @since 7.4
     */
    public String getWaitForExecutionSentence() {
        return waitForExecutionSentence;
    }

    /**
     * @since 5.4.2
     */
    public Boolean getTranslateEmptySentence() {
        return translateEmptySentence;
    }

    /**
     * @since 6.0
     */
    public String getPageProviderName() {
        if (pageProviderName == null) {
            if (referencePageProvider != null && referencePageProvider.isEnabled()) {
                pageProviderName = referencePageProvider.getName();
            } else if (coreQueryPageProvider != null && coreQueryPageProvider.isEnabled()
                    && coreQueryPageProvider.getName() != null) {
                pageProviderName = coreQueryPageProvider.getName();
            } else if (genericPageProvider != null && genericPageProvider.isEnabled()
                    && genericPageProvider.getName() != null) {
                pageProviderName = genericPageProvider.getName();
            } else {
                pageProviderName = getName();
            }
        }
        return pageProviderName;
    }

    /**
     * @since 6.0
     */
    public Map<String, String> getPageProviderProperties() {
        if (pageProviderProperties == null) {
            if (referencePageProvider != null && referencePageProvider.isEnabled()) {
                pageProviderProperties = referencePageProvider.getProperties();
            } else if (coreQueryPageProvider != null && coreQueryPageProvider.isEnabled()) {
                pageProviderProperties = coreQueryPageProvider.getProperties();

            } else if (genericPageProvider != null && genericPageProvider.isEnabled()) {
                pageProviderProperties = genericPageProvider.getProperties();
            }
        }
        return pageProviderProperties;
    }

    /**
     * @since 7.4
     */
    public Boolean getWaitForExecution() {
        return waitForExecution;
    }

    /**
     * @since 7.4
     */
    public void merge(ContentViewDescriptor newDesc) {
        this.setEnabled(newDesc.isEnabled());

        String title = newDesc.getTitle();
        if (title != null) {
            this.title = title;
        }

        Boolean translateTitle = newDesc.getTranslateTitle();
        if (translateTitle != null) {
            this.translateTitle = translateTitle;
        }

        String emptySentence = newDesc.getEmptySentence();
        if (emptySentence != null) {
            this.emptySentence = emptySentence;
        }

        String waitForExecutionSentence = newDesc.getWaitForExecutionSentence();
        if (waitForExecutionSentence != null) {
            this.waitForExecutionSentence = waitForExecutionSentence;
        }

        Boolean translateEmptySentence = newDesc.getTranslateEmptySentence();
        if (translateEmptySentence != null) {
            this.translateEmptySentence = translateEmptySentence;
        }

        String iconPath = newDesc.getIconPath();
        if (iconPath != null) {
            this.iconPath = iconPath;
        }

        List<String> actions = newDesc.getActionCategories();
        if (actions != null && !actions.isEmpty()) {
            this.actionCategories = actions;
        }

        String cacheKey = newDesc.getCacheKey();
        if (cacheKey != null) {
            this.cacheKey = cacheKey;
        }

        Integer cacheSize = newDesc.getCacheSize();
        if (cacheSize != null) {
            this.cacheSize = cacheSize;
        }

        CoreQueryPageProviderDescriptor coreDesc = newDesc.getCoreQueryPageProvider();
        if (coreDesc != null && coreDesc.isEnabled()) {
            this.coreQueryPageProvider = coreDesc;
            // make sure other page providers are reset
            this.genericPageProvider = null;
            this.referencePageProvider = null;
        }

        GenericPageProviderDescriptor genDesc = newDesc.getGenericPageProvider();
        if (genDesc != null && genDesc.isEnabled()) {
            this.genericPageProvider = genDesc;
            // make sure other page providers are reset
            this.coreQueryPageProvider = null;
            this.referencePageProvider = null;
        }

        ReferencePageProviderDescriptor refDesc = newDesc.getReferencePageProvider();
        if (refDesc != null && refDesc.isEnabled()) {
            this.referencePageProvider = refDesc;
            // make sure other page providers are reset
            this.coreQueryPageProvider = null;
            this.genericPageProvider = null;
        }

        String pagination = newDesc.getPagination();
        if (pagination != null) {
            this.pagination = pagination;
        }

        List<String> events = newDesc.getRefreshEventNames();
        if (events != null && !events.isEmpty()) {
            this.refreshEventNames = events;
        }
        events = newDesc.getResetEventNames();
        if (events != null && !events.isEmpty()) {
            this.resetEventNames = events;
        }

        ContentViewLayoutImpl searchLayout = newDesc.getSearchLayout();
        if (searchLayout != null) {
            this.searchLayout = searchLayout;
        }

        List<ContentViewLayout> resultLayouts = newDesc.getResultLayouts();
        if (resultLayouts != null) {
            Boolean appendResultLayout = newDesc.getAppendResultLayouts();
            if (Boolean.TRUE.equals(appendResultLayout) || resultLayouts.isEmpty()) {
                List<ContentViewLayout> allLayouts = new ArrayList<>();
                if (this.resultLayouts != null) {
                    allLayouts.addAll(this.resultLayouts);
                }
                allLayouts.addAll(resultLayouts);
                this.resultLayouts = allLayouts;
            } else {
                this.resultLayouts = resultLayouts;
            }
        }

        List<String> flags = newDesc.getFlags();
        if (flags != null && !flags.isEmpty()) {
            this.flags = flags;
        }

        String selectionList = newDesc.getSelectionListName();
        if (selectionList != null) {
            this.selectionList = selectionList;
        }

        Boolean useGlobalPageSize = newDesc.getUseGlobalPageSize();
        if (useGlobalPageSize != null) {
            this.useGlobalPageSize = useGlobalPageSize;
        }

        Boolean showTitle = newDesc.getShowTitle();
        if (showTitle != null) {
            this.showTitle = showTitle;
        }

        // avoid override when setting the default value => use the field, not
        // the API, for merge
        Boolean showPageSizeSelector = newDesc.showPageSizeSelector;
        if (showPageSizeSelector != null) {
            this.showPageSizeSelector = showPageSizeSelector;
        }

        Boolean showRefreshCommand = newDesc.showRefreshCommand;
        if (showRefreshCommand != null) {
            this.showRefreshCommand = showRefreshCommand;
        }

        Boolean showFilterForm = newDesc.getShowFilterForm();
        if (showFilterForm != null) {
            this.showFilterForm = showFilterForm;
        }

        String searchDocument = newDesc.getSearchDocumentBinding();
        if (searchDocument != null) {
            this.searchDocument = searchDocument;
        }

        String resultCols = newDesc.getResultColumnsBinding();
        if (resultCols != null) {
            this.resultColumns = resultCols;
        }

        String resultLayout = newDesc.getResultLayoutBinding();
        if (resultLayout != null) {
            this.resultLayout = resultLayout;
        }

        Boolean waitForFilter = newDesc.getWaitForExecution();
        if (waitForFilter != null) {
            this.waitForExecution = waitForFilter;
        }

    }

    @Override
    public ContentViewDescriptor clone() {
        ContentViewDescriptor clone = new ContentViewDescriptor();
        clone.name = getName();
        clone.enabled = isEnabled();
        clone.title = getTitle();
        clone.translateTitle = getTranslateTitle();
        clone.emptySentence = getEmptySentence();
        clone.waitForExecutionSentence = getWaitForExecutionSentence();
        clone.translateEmptySentence = getTranslateEmptySentence();
        clone.iconPath = getIconPath();
        CoreQueryPageProviderDescriptor cpp = getCoreQueryPageProvider();
        if (cpp != null) {
            clone.coreQueryPageProvider = cpp.clone();
        }
        GenericPageProviderDescriptor gpp = getGenericPageProvider();
        if (gpp != null) {
            clone.genericPageProvider = gpp.clone();
        }
        ReferencePageProviderDescriptor rpp = getReferencePageProvider();
        if (rpp != null) {
            clone.referencePageProvider = rpp.clone();
        }
        clone.selectionList = getSelectionListName();
        clone.pagination = getPagination();
        List<String> actionCats = getActionCategories();
        if (actionCats != null) {
            clone.actionCategories = new ArrayList<>();
            clone.actionCategories.addAll(actionCats);
        }
        clone.searchDocument = getSearchDocumentBinding();
        ContentViewLayoutImpl searchLayout = getSearchLayout();
        if (searchLayout != null) {
            clone.searchLayout = searchLayout.clone();
        }
        clone.appendResultLayouts = getAppendResultLayouts();
        List<ContentViewLayout> resultLayouts = getResultLayouts();
        if (resultLayouts != null) {
            clone.resultLayouts = new ArrayList<>();
            for (ContentViewLayout item : resultLayouts) {
                clone.resultLayouts.add(item.clone());
            }
        }
        clone.resultColumns = getResultColumnsBinding();
        clone.resultLayout = getResultLayoutBinding();
        List<String> flags = getFlags();
        if (flags != null) {
            clone.flags = new ArrayList<>();
            clone.flags.addAll(flags);
        }
        clone.cacheKey = getCacheKey();
        clone.cacheSize = getCacheSize();
        clone.useGlobalPageSize = getUseGlobalPageSize();
        clone.showTitle = getShowTitle();
        clone.showPageSizeSelector = getShowPageSizeSelector();
        clone.showRefreshCommand = getShowRefreshCommand();
        clone.showFilterForm = getShowFilterForm();
        List<String> refresh = getRefreshEventNames();
        if (refresh != null) {
            clone.refreshEventNames = new ArrayList<>();
            clone.refreshEventNames.addAll(refresh);
        }
        List<String> reset = getResetEventNames();
        if (reset != null) {
            clone.resetEventNames = new ArrayList<>();
            clone.resetEventNames.addAll(reset);
        }
        clone.waitForExecution = getWaitForExecution();
        return clone;
    }
}
