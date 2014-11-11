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

    protected Long pageSize;

    protected Long currentPage;

    protected Object[] parameters;

    protected DocumentModel searchDocument;

    protected List<SortInfo> sortInfos;

    protected ContentViewLayout resultLayout;

    protected List<String> resultColumns;

    public String getContentViewName() {
        return contentViewName;
    }

    public void setContentViewName(String contentViewName) {
        this.contentViewName = contentViewName;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public Long getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Long currentPage) {
        this.currentPage = currentPage;
    }

    public Object[] getQueryParameters() {
        return parameters;
    }

    public void setQueryParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public DocumentModel getSearchDocumentModel() {
        return searchDocument;
    }

    public void setSearchDocumentModel(DocumentModel searchDocument) {
        this.searchDocument = searchDocument;
    }

    public List<SortInfo> getSortInfos() {
        return sortInfos;
    }

    public void setSortInfos(List<SortInfo> sortInfos) {
        this.sortInfos = sortInfos;
    }

    public ContentViewLayout getResultLayout() {
        return resultLayout;
    }

    public void setResultLayout(ContentViewLayout resultLayout) {
        this.resultLayout = resultLayout;
    }

    public List<String> getResultColumns() {
        return resultColumns;
    }

    public void setResultColumns(List<String> resultColumns) {
        this.resultColumns = resultColumns;
    }

}
