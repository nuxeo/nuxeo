/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * Default implementation of {@link ContentViewState}
 *
 * @since 5.4.2
 */
public class ContentViewStateImpl implements ContentViewState {

    private static final long serialVersionUID = 1L;

    protected String contentViewName;

    protected String pageProviderName;

    protected Long pageSize;

    protected Long currentPage;

    protected Object[] parameters;

    protected DocumentModel searchDocument;

    protected List<SortInfo> sortInfos;

    protected ContentViewLayout resultLayout;

    protected List<String> resultColumns;

    @Override
    public String getContentViewName() {
        return contentViewName;
    }

    @Override
    public void setContentViewName(String contentViewName) {
        this.contentViewName = contentViewName;
    }

    @Override
    public Long getPageSize() {
        return pageSize;
    }

    @Override
    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public Long getCurrentPage() {
        return currentPage;
    }

    @Override
    public void setCurrentPage(Long currentPage) {
        this.currentPage = currentPage;
    }

    @Override
    public Object[] getQueryParameters() {
        return parameters;
    }

    @Override
    public void setQueryParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public DocumentModel getSearchDocumentModel() {
        return searchDocument;
    }

    @Override
    public void setSearchDocumentModel(DocumentModel searchDocument) {
        this.searchDocument = searchDocument;
    }

    @Override
    public List<SortInfo> getSortInfos() {
        return sortInfos;
    }

    @Override
    public void setSortInfos(List<SortInfo> sortInfos) {
        this.sortInfos = sortInfos;
    }

    @Override
    public ContentViewLayout getResultLayout() {
        return resultLayout;
    }

    @Override
    public void setResultLayout(ContentViewLayout resultLayout) {
        this.resultLayout = resultLayout;
    }

    @Override
    public List<String> getResultColumns() {
        return resultColumns;
    }

    @Override
    public void setResultColumns(List<String> resultColumns) {
        this.resultColumns = resultColumns;
    }

    @Override
    public String getPageProviderName() {
        return pageProviderName;
    }

    @Override
    public void setPageProviderName(String pageProviderName) {
        this.pageProviderName = pageProviderName;
    }

}
