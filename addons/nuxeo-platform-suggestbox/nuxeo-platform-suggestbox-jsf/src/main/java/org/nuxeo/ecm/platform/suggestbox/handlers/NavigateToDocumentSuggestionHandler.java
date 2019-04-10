/**
 *
 */

package org.nuxeo.ecm.platform.suggestbox.handlers;

import org.jboss.seam.Component;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.platform.faceted.search.jsf.FacetedSearchActions;
import org.nuxeo.ecm.platform.suggestbox.service.DocumentSuggestion;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.virtualnavigation.action.MultiNavTreeManager;
import org.nuxeo.ecm.webapp.context.NavigationContextBean;

/**
 * Handle DocumentSuggestion using a simple navigation in the JSF UI.
 */
@Operation(id = NavigateToDocumentSuggestionHandler.ID, category = Constants.CAT_UI, label = "Suggestion handler for navigation to document view", description = "Handles JSF navigation given a DocumentSuggestion as input.")
public class NavigateToDocumentSuggestionHandler {

    public static final String ID = "Suggestion.JSF.NavigateToDocument";

    @Param(name = "navigationTree", required = false)
    public String navigationTree = MultiNavTreeManager.STD_NAV_TREE;

    @Param(name = "updateNavigationTree", required = false)
    public boolean updateNavigationTree = true;

    @Param(name = "clearFacetedSearch", required = false)
    public boolean clearFacetedSearch = false;

    @OperationMethod
    public Object run(Object input) throws OperationException, ClientException {
        if (!(input instanceof DocumentSuggestion)) {
            throw new OperationException(
                    String.format("Expected an instance of DocumentSuggestion,"
                            + " got '%s'", input));
        }
        if (updateNavigationTree) {
            MultiNavTreeManager multiNavTreeManager = (MultiNavTreeManager) Component.getInstance(MultiNavTreeManager.class);
            multiNavTreeManager.setSelectedNavigationTree(navigationTree);
        }
        if (clearFacetedSearch) {
            FacetedSearchActions facetedSearchActions = (FacetedSearchActions) Component.getInstance(FacetedSearchActions.class);
            facetedSearchActions.clearSearch();
        }
        DocumentSuggestion suggestion = (DocumentSuggestion) input;
        NavigationContext navigationContext = (NavigationContext) Component.getInstance(NavigationContextBean.class);
        DocumentLocation docLoc = suggestion.getDocumentLocation();
        return navigationContext.navigateTo(
                new RepositoryLocation(docLoc.getServerName()),
                docLoc.getDocRef());
    }

}
