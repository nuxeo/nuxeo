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

import org.jboss.seam.Component;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.suggestbox.service.UserSuggestion;
import org.nuxeo.ecm.webapp.security.UserManagementActions;

/**
 * Handle UserSuggestion using a simple navigation in the JSF UI.
 */
@Operation(id = NavigateToUserSuggestionHandler.ID, category = Constants.CAT_UI, label = "Suggestion handler for navigation to user view", description = "Handles JSF navigation given a UserSuggestion as input.", addToStudio = false)
public class NavigateToUserSuggestionHandler {

    public static final String ID = "Suggestion.JSF.NavigateToUser";

    @OperationMethod
    public Object run(Object input) throws OperationException, ClientException {
        if (!(input instanceof UserSuggestion)) {
            throw new OperationException(String.format(
                    "Expected an instance of UserSuggestion," + " got '%s'",
                    input));
        }
        UserSuggestion suggestion = (UserSuggestion) input;
        UserManagementActions userManagementActions = (UserManagementActions) Component.getInstance(UserManagementActions.class);
        return userManagementActions.viewUser(suggestion.getUserId());
    }

}
