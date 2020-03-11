/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Suggest to search for documents meeting some specific search criteria.
 *
 * @deprecated since 6.0. The redirection to the search tab is not handled by a Suggestion anymore.
 */
@Deprecated
public class SearchDocumentsSuggestion extends Suggestion {

    private static final long serialVersionUID = 1L;

    protected final Map<String, Serializable> searchCriteria = new HashMap<>();

    public SearchDocumentsSuggestion(String id, String label, String iconURL) {
        super(id, CommonSuggestionTypes.SEARCH_DOCUMENTS, label, iconURL);
    }

    public SearchDocumentsSuggestion withSearchCriterion(String searchField, Serializable searchValue) {
        searchCriteria.put(searchField, searchValue);
        return this;
    }

    public Map<String, Serializable> getSearchCriteria() {
        return searchCriteria;
    }

    @Override
    public String getObjectUrl() {
        // TODO Generate the url to access the search page
        return null;
    }

}
