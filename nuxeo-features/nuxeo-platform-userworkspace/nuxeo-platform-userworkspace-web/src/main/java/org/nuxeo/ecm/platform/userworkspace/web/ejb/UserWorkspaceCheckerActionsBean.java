/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Martins
 */

package org.nuxeo.ecm.platform.userworkspace.web.ejb;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.userworkspace.constants.UserWorkspaceConstants;

/**
 * Standalone class to check if navigation is in the user workspaces
 *
 * @since 5.6
 * @author Thierry Martins <tm@nuxeo.com>
 */
@Scope(CONVERSATION)
@Name("userWorkspaceChecker")
public class UserWorkspaceCheckerActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;
    @In(create = true)
    protected transient NavigationContext navigationContext;

    @Factory(value = "isUserWorkspace", scope = EVENT)
    public Boolean computeIsUserWorkspace() {
        return navigationContext.getCurrentDocument().getPathAsString().contains(
                UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
    }
}
