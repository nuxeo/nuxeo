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

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * Implementations of this interface represent a content view state that can be used to restore a given content view.
 * <p>
 * State is restricted to given getters and setters. Some state information is actually taken on the page provider.
 *
 * @since 5.4.2
 */
public interface ContentViewState extends Serializable {

    String getContentViewName();

    void setContentViewName(String contentViewName);

    Long getPageSize();

    void setPageSize(Long pageSize);

    Long getCurrentPage();

    void setCurrentPage(Long currentPage);

    Object[] getQueryParameters();

    void setQueryParameters(Object[] parameters);

    DocumentModel getSearchDocumentModel();

    void setSearchDocumentModel(DocumentModel searchDocument);

    List<SortInfo> getSortInfos();

    void setSortInfos(List<SortInfo> sortInfos);

    ContentViewLayout getResultLayout();

    void setResultLayout(ContentViewLayout resultLayout);

    List<String> getResultColumns();

    void setResultColumns(List<String> resultColumns);

    /**
     * @since 7.1
     */
    String getPageProviderName();

    /**
     * @since 7.1
     */
    void setPageProviderName(String name);

    /**
     * @since 8.1
     */
    void setExecuted(boolean executed);

    /**
     * @since 8.1
     */
    boolean isExecuted();

}
