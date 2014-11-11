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

    private static final long serialVersionUID = 1L;

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
    Boolean showPageSizeSelector = Boolean.TRUE;

    @XNode("showRefreshPage")
    Boolean showRefreshPage = Boolean.TRUE;

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
        return refreshEventNames;
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

    public List<String> getFlags() {
        return flags;
    }

    public boolean isEnabled() {
        return enabled;
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
        return showPageSizeSelector;
    }

    /**
     * @since 5.4.2
     */
    public Boolean getShowRefreshPage() {
        return showRefreshPage;
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

}
