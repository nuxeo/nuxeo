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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Anahide Tchertchian
 */
@XObject("contentView")
public class ContentViewDescriptor implements ContentView {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    String name;

    @XNode("@category")
    String category;

    @XNode("resultProvider")
    String resultProvider;

    @XNode("selectionList")
    String selectionList;

    @XNode("pagination")
    String pagination;

    @XNode("availableActions@category")
    String availableActions;

    @XNode("searchLayout")
    String searchLayout;

    @XNode("resultLayout")
    String resultLayout;

    @XNode("max")
    Integer max = new Integer(0);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getResultProvider() {
        return resultProvider;
    }

    public void setResultProvider(String resultProvider) {
        this.resultProvider = resultProvider;
    }

    public String getSelectionList() {
        return selectionList;
    }

    public void setSelectionList(String selectionList) {
        this.selectionList = selectionList;
    }

    public String getPagination() {
        return pagination;
    }

    public void setPagination(String pagination) {
        this.pagination = pagination;
    }

    public String getAvailableActionsCategory() {
        return availableActions;
    }

    public void setAvailableActionsCategory(String availableActions) {
        this.availableActions = availableActions;
    }

    public String getSearchLayout() {
        return searchLayout;
    }

    public void setSearchLayout(String searchLayout) {
        this.searchLayout = searchLayout;
    }

    public String getResultLayout() {
        return resultLayout;
    }

    public void setResultLayout(String resultLayout) {
        this.resultLayout = resultLayout;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

}
