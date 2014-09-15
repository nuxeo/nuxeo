/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Suggest to search for documents meeting some specific search criteria.
 */
public class SearchDocumentsSuggestion extends Suggestion {

    private static final long serialVersionUID = 1L;

    protected final Map<String, Serializable> searchCriteria = new HashMap<String, Serializable>();

    public SearchDocumentsSuggestion(String id, String label, String iconURL) {
        super(id, CommonSuggestionTypes.SEARCH_DOCUMENTS, label, iconURL);
    }

    public SearchDocumentsSuggestion withSearchCriterion(String searchField,
            Serializable searchValue) {
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
