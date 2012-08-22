/**
 *
 */

package org.nuxeo.ecm.platform.suggestbox.handlers;

import org.jboss.seam.Component;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.suggestbox.service.GroupSuggestion;
import org.nuxeo.ecm.webapp.security.GroupManagementActions;

/**
 * Handle GroupSuggestion using a simple navigation in the JSF UI.
 */
@Operation(id = NavigateToGroupSuggestionHandler.ID, category = Constants.CAT_UI, label = "Suggestion handler for navigation to group view", description = "Handles JSF navigation given a GroupSuggestion as input.")
public class NavigateToGroupSuggestionHandler {

    public static final String ID = "Suggestion.JSF.NavigateToGroup";

    @OperationMethod
    public Object run(Object input) throws OperationException, ClientException {
        if (!(input instanceof GroupSuggestion)) {
            throw new OperationException(String.format(
                    "Expected an instance of GroupSuggestion," + " got '%s'",
                    input));
        }
        GroupSuggestion suggestion = (GroupSuggestion) input;
        GroupManagementActions groupManagementActions = (GroupManagementActions) Component.getInstance(GroupManagementActions.class);
        return groupManagementActions.viewGroup(suggestion.getGroupId());
    }

}
