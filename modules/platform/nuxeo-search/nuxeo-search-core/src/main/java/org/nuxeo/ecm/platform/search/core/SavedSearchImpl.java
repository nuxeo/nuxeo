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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 8.3
 */
public class SavedSearchImpl implements SavedSearch {

    private static final long serialVersionUID = 1L;
    private DocumentModel doc;

    public SavedSearchImpl(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public String getId() {
        return doc.getId();
    }

    @Override
    public String getTitle() {
        return getPropertyValue(SavedSearchConstants.TITLE_PROPERTY_NAME);
    }

    @Override
    public Map<String, String> getNamedParams() {
        List<Map<String, String>> paramsProperty = getPropertyValue(SavedSearchConstants.NAMED_PARAMS_PROPERTY_NAME);
        Map<String, String> params = new HashMap<>();
        for (Map<String, String> map : paramsProperty) {
            params.put(map.get("key"), map.get("value"));
        }
        return params;
    }

    @Override
    public String getQueryParams() {
        return getPropertyValue(SavedSearchConstants.QUERY_PARAMS_PROPERTY_NAME);
    }

    @Override
    public String getQuery() {
        return getPropertyValue(SavedSearchConstants.QUERY_PROPERTY_NAME);
    }

    @Override
    public String getQueryLanguage() {
        return getPropertyValue(SavedSearchConstants.QUERY_LANGUAGE_PROPERTY_NAME);
    }

    @Override
    public String getPageProviderName() {
        return getPropertyValue(SavedSearchConstants.PAGE_PROVIDER_NAME_PROPERTY_NAME);
    }

    @Override
    public Long getPageSize() {
        return getPropertyValue(SavedSearchConstants.PAGE_SIZE_PROPERTY_NAME);
    }

    @Override
    public Long getCurrentPageIndex() {
        return getPropertyValue(SavedSearchConstants.CURRENT_PAGE_INDEX_PROPERTY_NAME);
    }

    @Override
    public Long getMaxResults() {
        return getPropertyValue(SavedSearchConstants.MAX_RESULTS_PROPERTY_NAME);
    }

    @Override
    public String getSortBy() {
        return getPropertyValue(SavedSearchConstants.SORT_BY_PROPERTY_NAME);
    }

    @Override
    public String getSortOrder() {
        return getPropertyValue(SavedSearchConstants.SORT_ORDER_PROPERTY_NAME);
    }

    @Override
    public String getContentViewData() {
        return getPropertyValue(SavedSearchConstants.CONTENT_VIEW_DATA_PROPERTY_NAME);
    }

    @Override
    public DocumentModel getDocument() {
        return doc;
    }

    @Override
    public void setTitle(String title) {
        doc.setPropertyValue(SavedSearchConstants.TITLE_PROPERTY_NAME, title);
    }

    @Override
    public void setNamedParams(Map<String, String> params) {
        if (params != null) {
            List<Map<String, String>> paramsProperty = getPropertyValue(SavedSearchConstants.NAMED_PARAMS_PROPERTY_NAME);
            if (paramsProperty == null) {
                paramsProperty = new ArrayList<>();
            }

            Map<String, String> variable;
            for (String key : params.keySet()) {
                String value = params.get(key);
                variable = new HashMap<>(2);
                variable.put("key", key);
                variable.put("value", value);
                paramsProperty.add(variable);
            }
            doc.setPropertyValue(SavedSearchConstants.NAMED_PARAMS_PROPERTY_NAME, (Serializable) paramsProperty);
        }
    }

    @Override
    public void setQueryParams(String queryParams) {
        doc.setPropertyValue(SavedSearchConstants.QUERY_PARAMS_PROPERTY_NAME, queryParams);
    }

    @Override
    public void setQuery(String query) {
        doc.setPropertyValue(SavedSearchConstants.QUERY_PROPERTY_NAME, query);
    }

    @Override
    public void setQueryLanguage(String queryLanguage) {
        doc.setPropertyValue(SavedSearchConstants.QUERY_LANGUAGE_PROPERTY_NAME, queryLanguage);
    }

    @Override
    public void setPageProviderName(String pageProviderName) {
        doc.setPropertyValue(SavedSearchConstants.PAGE_PROVIDER_NAME_PROPERTY_NAME, pageProviderName);
    }

    @Override
    public void setPageSize(Long pageSize) {
        doc.setPropertyValue(SavedSearchConstants.PAGE_SIZE_PROPERTY_NAME, pageSize);
    }

    @Override
    public void setCurrentPageIndex(Long currentPageIndex) {
        doc.setPropertyValue(SavedSearchConstants.CURRENT_PAGE_INDEX_PROPERTY_NAME, currentPageIndex);
    }

    @Override
    public void setMaxResults(Long maxResults) {
        doc.setPropertyValue(SavedSearchConstants.MAX_RESULTS_PROPERTY_NAME, maxResults);
    }

    @Override
    public void setSortBy(String sortBy) {
        doc.setPropertyValue(SavedSearchConstants.SORT_BY_PROPERTY_NAME, sortBy);
    }

    @Override
    public void setSortOrder(String sortOrder) {
        doc.setPropertyValue(SavedSearchConstants.SORT_ORDER_PROPERTY_NAME, sortOrder);
    }

    @Override
    public void setContentViewData(String contentViewData) {
        doc.setPropertyValue(SavedSearchConstants.CONTENT_VIEW_DATA_PROPERTY_NAME, contentViewData);
    }

    @Override
    public void setDocument(DocumentModel doc) {
        this.doc = doc;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getPropertyValue(String propertyName) {
        Serializable value = doc.getPropertyValue(propertyName);
        if (value instanceof Object[]) {
            value = new ArrayList<>(Arrays.asList((Object[]) value));
        }
        return (T) value;
    }

    /**
     * @since 9.3
     */
    @Override
    public Long getCurrentPageOffset() {
        return getPropertyValue(SavedSearchConstants.CURRENT_PAGE_OFFSET_PROPERTY_NAME);
    }
}
