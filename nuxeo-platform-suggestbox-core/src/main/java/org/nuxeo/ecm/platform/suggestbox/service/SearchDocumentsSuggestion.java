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

    public SearchDocumentsSuggestion(String label, String iconURL) {
        super(CommonSuggestionTypes.SEARCH_DOCUMENTS, label, iconURL);
    }

    public SearchDocumentsSuggestion withSearchCriterion(String searchField,
            Serializable searchValue) {
        searchCriteria.put(searchField, searchValue);
        return this;
    }

    public Map<String, Serializable> getSearchCriteria() {
        return searchCriteria;
    }

}
