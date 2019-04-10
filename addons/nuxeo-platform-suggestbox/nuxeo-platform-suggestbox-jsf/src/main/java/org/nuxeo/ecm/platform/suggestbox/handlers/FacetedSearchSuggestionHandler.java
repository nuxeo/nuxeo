/**
 *
 */

package org.nuxeo.ecm.platform.suggestbox.handlers;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.jboss.seam.Component;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.faceted.search.jsf.FacetedSearchActions;
import org.nuxeo.ecm.platform.suggestbox.service.SearchDocumentsSuggestion;
import org.nuxeo.ecm.virtualnavigation.action.MultiNavTreeManager;

/**
 * Handle SearchDocumentsSuggestion using the faceted search JSF UI.
 */
@Operation(id = FacetedSearchSuggestionHandler.ID, category = Constants.CAT_UI, label = "Suggestion handler for navigation to faceted search view", description = "Handles JSF navigation given a SearchDocumentsSuggestion as input.")
public class FacetedSearchSuggestionHandler {

    public static final String ID = "Suggestion.JSF.NavigateToFacetedSearch";

    @OperationMethod
    public Object run(Object input) throws OperationException, ClientException {
        if (!(input instanceof SearchDocumentsSuggestion)) {
            throw new OperationException(String.format(
                    "Expected an instance of SearchDocumentsSuggestion,"
                            + " got '%s'", input));
        }
        SearchDocumentsSuggestion suggestion = (SearchDocumentsSuggestion) input;

        MultiNavTreeManager multiNavTreeManager = (MultiNavTreeManager) Component.getInstance(MultiNavTreeManager.class);
        FacetedSearchActions facetedSearchActions = (FacetedSearchActions) Component.getInstance(FacetedSearchActions.class);
        ContentViewActions contentViewActions = (ContentViewActions) Component.getInstance(ContentViewActions.class);

        facetedSearchActions.clearSearch();
        facetedSearchActions.setCurrentContentViewName(null);
        String contentViewName = facetedSearchActions.getCurrentContentViewName();
        ContentView contentView = contentViewActions.getContentView(contentViewName);
        DocumentModel dm = contentView.getSearchDocumentModel();

        for (Map.Entry<String, Serializable> searchEntry : suggestion.getSearchCriteria().entrySet()) {
            String searchField = searchEntry.getKey();
            Serializable searchValue = searchEntry.getValue();
            Property searchProperty = dm.getProperty(searchField);
            if (searchProperty.isList()) {
                // list type is used for multi-valued option fields such as
                // fsd_dc_creator.
                dm.setPropertyValue(searchField,
                        (Serializable) Collections.singleton(searchValue));
            } else {
                dm.setPropertyValue(searchField, searchValue);
            }
        }
        multiNavTreeManager.setSelectedNavigationTree("facetedSearch");
        // JSF view id for the faceted search results page
        return "faceted_search_results";
    }

}
