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

    protected String resultProviderName;

    protected String selectionList;

    protected String pagination;

    protected String availableActions;

    protected String searchLayoutName;

    protected String resultLayoutName;

    protected Integer max;

    public ContentViewImpl(String name, String category,
            String resultProviderName, String selectionList, String pagination,
            String availableActions, String searchLayoutName,
            String resultLayoutName, Integer max) {
        super();
        this.name = name;
        this.category = category;
        this.resultProviderName = resultProviderName;
        this.selectionList = selectionList;
        this.pagination = pagination;
        this.availableActions = availableActions;
        this.searchLayoutName = searchLayoutName;
        this.resultLayoutName = resultLayoutName;
        this.max = max;
    }

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

    public String getResultProviderName() {
        return resultProviderName;
    }

    public void setResultProviderName(String resultProviderName) {
        this.resultProviderName = resultProviderName;
    }

    public String getSelectionListName() {
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

    public String getSearchLayoutName() {
        return searchLayoutName;
    }

    public void setSearchLayoutName(String searchLayoutName) {
        this.searchLayoutName = searchLayoutName;
    }

    public String getResultLayoutName() {
        return resultLayoutName;
    }

    public void setResultLayoutName(String resultLayoutName) {
        this.resultLayoutName = resultLayoutName;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

}
