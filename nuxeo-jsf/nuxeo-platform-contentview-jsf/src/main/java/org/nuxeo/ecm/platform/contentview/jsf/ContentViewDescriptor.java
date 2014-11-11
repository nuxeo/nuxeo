/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
            if (referencePageProvider != null
                    && referencePageProvider.isEnabled()) {
                pageProviderName = referencePageProvider.getName();
            } else if (coreQueryPageProvider != null
                    && coreQueryPageProvider.isEnabled()
                    && coreQueryPageProvider.getName() != null) {
                pageProviderName = coreQueryPageProvider.getName();
            } else if (genericPageProvider != null
                    && genericPageProvider.isEnabled()
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
            if (referencePageProvider != null
                    && referencePageProvider.isEnabled()) {
                pageProviderProperties = referencePageProvider.getProperties();
            } else if (coreQueryPageProvider != null
                    && coreQueryPageProvider.isEnabled()) {
                pageProviderProperties = coreQueryPageProvider.getProperties();

            } else if (genericPageProvider != null
                    && genericPageProvider.isEnabled()) {
                pageProviderProperties = genericPageProvider.getProperties();
            }
        }
        return pageProviderProperties;
    }

    public ContentViewDescriptor clone() {
        ContentViewDescriptor clone = new ContentViewDescriptor();
        clone.name = getName();
        clone.enabled = isEnabled();
        clone.title = getTitle();
        clone.translateTitle = getTranslateTitle();
        clone.emptySentence = getEmptySentence();
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
            clone.actionCategories = new ArrayList<String>();
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
            clone.resultLayouts = new ArrayList<ContentViewLayout>();
            for (ContentViewLayout item : resultLayouts) {
                clone.resultLayouts.add(item.clone());
            }
        }
        clone.resultColumns = getResultColumnsBinding();
        clone.resultLayout = getResultLayoutBinding();
        List<String> flags = getFlags();
        if (flags != null) {
            clone.flags = new ArrayList<String>();
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
            clone.refreshEventNames = new ArrayList<String>();
            clone.refreshEventNames.addAll(refresh);
        }
        List<String> reset = getResetEventNames();
        if (reset != null) {
            clone.resetEventNames = new ArrayList<String>();
            clone.resetEventNames.addAll(reset);
        }
        return clone;
    }
}
