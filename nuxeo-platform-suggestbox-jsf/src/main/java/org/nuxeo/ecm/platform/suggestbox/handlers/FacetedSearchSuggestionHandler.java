/*
 * (C) Copyright 2011-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.suggestbox.handlers;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.suggestbox.service.SearchDocumentsSuggestion;

/**
 * Handle SearchDocumentsSuggestion using the faceted search JSF UI.
 * <p>
 * When suggestion is chosen, the search document model attached to the current
 * faceted search content view is impacted. If none of the document fields
 * matches the configuration to impact, another content view is looked up to
 * see if it can take into account the search criteria.
 */
@Operation(id = FacetedSearchSuggestionHandler.ID, category = Constants.CAT_UI, label = "Suggestion handler for navigation to faceted search view", description = "Handles JSF navigation given a SearchDocumentsSuggestion as input.", addToStudio = false)
public class FacetedSearchSuggestionHandler {

    public static final String ID = "Suggestion.JSF.NavigateToFacetedSearch";

    @OperationMethod
    public Object run(Object input) throws OperationException, ClientException {
        /* TODO reactivate when the Search tab will be deployed
         * if (!(input instanceof SearchDocumentsSuggestion)) {
            throw new OperationException(String.format(
                    "Expected an instance of SearchDocumentsSuggestion,"
                            + " got '%s'", input));
        }
        SearchDocumentsSuggestion suggestion = (SearchDocumentsSuggestion) input;

        MultiNavTreeManager multiNavTreeManager = (MultiNavTreeManager) Component.getInstance(MultiNavTreeManager.class);
        FacetedSearchActions facetedSearchActions = (FacetedSearchActions) Component.getInstance(FacetedSearchActions.class);
        ContentViewActions contentViewActions = (ContentViewActions) Component.getInstance(ContentViewActions.class);

        facetedSearchActions.clearSearch();
        String contentViewName = facetedSearchActions.getCurrentContentViewName();
        if (!impactContentView(contentViewActions, contentViewName, suggestion)) {
            // reset current faceted search, and find a content view that's
            // impacted
            List<String> cvNames = facetedSearchActions.getContentViewNames();
            if (cvNames != null && cvNames.size() > 1) {
                for (String cvName : cvNames) {
                    facetedSearchActions.setCurrentContentViewName(cvName);
                    if (impactContentView(contentViewActions, cvName,
                            suggestion)) {
                        break;
                    }
                }
            }
        }

        multiNavTreeManager.setSelectedNavigationTree("facetedSearch");
        // JSF view id for the faceted search results page*/
        return "faceted_search_results";
    }

    /**
     * Set search criteria on given faceted search content view, and returns a
     * boolean stating if at least one of the document properties was impacted.
     *
     * @since 5.8
     */
    protected boolean impactContentView(ContentViewActions contentViewActions,
            String contentViewName, SearchDocumentsSuggestion suggestion)
            throws ClientException {
        boolean set = false;
        ContentView contentView = contentViewActions.getContentView(contentViewName);
        DocumentModel dm = contentView.getSearchDocumentModel();
        if (dm != null) {
            for (Map.Entry<String, Serializable> searchEntry : suggestion.getSearchCriteria().entrySet()) {
                String searchField = searchEntry.getKey();
                Serializable searchValue = searchEntry.getValue();
                try {
                    Property searchProperty = dm.getProperty(searchField);
                    if (searchProperty.isList()) {
                        // list type is used for multi-valued option fields
                        // such as fsd_dc_creator.
                        dm.setPropertyValue(
                                searchField,
                                (Serializable) Collections.singleton(searchValue));
                    } else {
                        dm.setPropertyValue(searchField, searchValue);
                    }
                    set = true;
                } catch (PropertyNotFoundException e) {
                    // assume property does not exist on this document model
                    continue;
                }
            }
        }
        return set;
    }

}
