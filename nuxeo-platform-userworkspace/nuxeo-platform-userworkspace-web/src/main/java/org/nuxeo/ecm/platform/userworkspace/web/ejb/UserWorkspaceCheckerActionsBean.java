package org.nuxeo.ecm.platform.userworkspace.web.ejb;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.userworkspace.constants.UserWorkspaceConstants;

/**
 * Standalone class to check if navigation is in the user workspaces
 *
 * @author Thierry Martins <tm@nuxeo.com>
 */
@Scope(CONVERSATION)
@Name("userWorkspaceChecker")
public class UserWorkspaceCheckerActionsBean {

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @Factory(value = "isUserWorkspace", scope = EVENT)
    public Boolean computeIsUserWorkspace() {
        return navigationContext.getCurrentDocument().getPathAsString().contains(
                UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
    }
}
