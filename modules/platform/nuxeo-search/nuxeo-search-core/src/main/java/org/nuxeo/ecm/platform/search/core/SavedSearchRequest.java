/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.ecm.platform.search.core;

import java.util.Map;

/**
 * @since 8.3
 */
public class SavedSearchRequest {

    protected String id;

    protected String title;

    protected String queryParams;

    protected Map<String, String> namedParams;

    protected String query;

    protected String queryLanguage;

    protected String pageProviderName;

    protected Long pageSize;

    protected Long currentPageIndex;

    protected Long maxResults;

    protected String sortBy;

    protected String sortOrder;

    protected String contentViewData;

    public SavedSearchRequest(String id, String title, String queryParams, Map<String, String> namedParams,
            String query, String queryLanguage, String pageProviderName, Long pageSize, Long currentPageIndex,
            Long maxResults, String sortBy, String sortOrder, String contentViewData) {
        this.id = id;
        this.title = title;
        this.queryParams = queryParams;
        this.namedParams = namedParams;
        this.query = query;
        this.queryLanguage = queryLanguage;
        this.pageProviderName = pageProviderName;
        this.pageSize = pageSize;
        this.currentPageIndex = currentPageIndex;
        this.maxResults = maxResults;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.contentViewData = contentViewData;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getQueryParams() {
        return queryParams;
    }

    public Map<String, String> getNamedParams() {
        return namedParams;
    }

    public String getQuery() {
        return query;
    }

    public String getQueryLanguage() {
        return queryLanguage;
    }

    public String getPageProviderName() {
        return pageProviderName;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public Long getCurrentPageIndex() {
        return currentPageIndex;
    }

    public Long getMaxResults() {
        return maxResults;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public String getContentViewData() {
        return contentViewData;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setQueryParams(String queryParams) {
        this.queryParams = queryParams;
    }

    public void setNamedParams(Map<String, String> namedParams) {
        this.namedParams = namedParams;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setQueryLanguage(String queryLanguage) {
        this.queryLanguage = queryLanguage;
    }

    public void setPageProviderName(String pageProviderName) {
        this.pageProviderName = pageProviderName;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }

    public void setCurrentPageIndex(Long currentPageIndex) {
        this.currentPageIndex = currentPageIndex;
    }

    public void setMaxResults(Long maxResults) {
        this.maxResults = maxResults;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setContentViewData(String contentViewData) {
        this.contentViewData = contentViewData;
    }

}
