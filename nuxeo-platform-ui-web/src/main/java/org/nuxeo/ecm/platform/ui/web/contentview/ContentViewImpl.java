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

/**
 * @author Anahide Tchertchian
 */
public class ContentViewImpl implements ContentView {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String category;

    protected String resultProvider;

    protected String selectionList;

    protected String pagination;

    protected String availableActions;

    protected String searchLayout;

    protected String resultLayout;

    protected Integer max;

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
