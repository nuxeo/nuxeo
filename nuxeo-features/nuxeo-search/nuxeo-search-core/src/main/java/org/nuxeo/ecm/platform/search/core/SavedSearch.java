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
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 8.3
 */
public interface SavedSearch extends Serializable {

    String getId();

    String getTitle();

    Map<String, String> getNamedParams();

    String getQueryParams();

    String getQuery();

    String getQueryLanguage();

    String getPageProviderName();

    Long getPageSize();

    Long getCurrentPageIndex();

    /**
     * @since 9.3
     */
    Long getCurrentPageOffset();

    Long getMaxResults();

    String getSortBy();

    String getSortOrder();

    String getContentViewData();

    DocumentModel getDocument();

    void setTitle(String title);

    void setNamedParams(Map<String, String> params);

    void setQueryParams(String queryParams);

    void setQuery(String query);

    void setQueryLanguage(String queryLanguage);

    void setPageProviderName(String pageProviderName);

    void setPageSize(Long pageSize);

    void setCurrentPageIndex(Long currentPageIndex);

    void setMaxResults(Long maxResults);

    void setSortBy(String sortBy);

    void setSortOrder(String sortOrder);

    void setContentViewData(String contentViewData);

    void setDocument(DocumentModel doc);

}
