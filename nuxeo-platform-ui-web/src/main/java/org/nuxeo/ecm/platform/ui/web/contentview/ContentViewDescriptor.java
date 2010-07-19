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
package org.nuxeo.ecm.platform.ui.web.contentview;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for content view registration.
 *
 * @author Anahide Tchertchian
 */
@XObject("contentView")
public class ContentViewDescriptor {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    String name;

    @XNode("coreQueryPageProvider")
    CoreQueryPageProviderDescriptor coreQueryPageProvider;

    @XNode("genericPageProvider")
    GenericPageProviderDescriptor genericPageProvider;

    @XNode("selectionList")
    String selectionList;

    @XNode("pagination")
    String pagination;

    @XNodeList(value = "actions@category", type = ArrayList.class, componentType = String.class)
    List<String> actionCategories;

    @XNode("searchLayout")
    String searchLayout;

    @XNode("resultLayout")
    String resultLayout;

    @XNode("cacheKey")
    String cacheKey;

    @XNodeList(value = "refresh/event", type = ArrayList.class, componentType = String.class)
    List<String> eventNames;

    public String getName() {
        return name;
    }

    public CoreQueryPageProviderDescriptor getCoreQueryPageProvider() {
        return coreQueryPageProvider;
    }

    public GenericPageProviderDescriptor getGenericPageProvider() {
        return genericPageProvider;
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

    public String getSearchLayoutName() {
        return searchLayout;
    }

    public String getResultLayoutName() {
        return resultLayout;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public List<String> getRefreshEventNames() {
        return eventNames;
    }

}
