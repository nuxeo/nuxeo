/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

    // default to true for BBB
    protected boolean executed = true;

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

    @Override
    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    @Override
    public boolean isExecuted() {
        return executed;
    }

}
