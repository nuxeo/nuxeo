/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Martins
 */

package org.nuxeo.ecm.platform.userworkspace.web.ejb;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.userworkspace.constants.UserWorkspaceConstants;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Standalone class to check if navigation is in the user workspaces and if needed get the current personal workspace
 * path
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

    @In(required = false, create = true)
    protected transient CoreSession documentManager;

    protected Boolean isUserWorkspace;

    protected String currentPersonalWorkspacePath;

    @Factory(value = "isUserWorkspace", scope = EVENT)
    public Boolean computeIsUserWorkspace() {
        if (isUserWorkspace == null && navigationContext.getCurrentDocument() != null) {
            isUserWorkspace = navigationContext.getCurrentDocument().getPathAsString().contains(
                    UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT);
        }
        return isUserWorkspace;
    }

    @Factory(value = "currentPersonalWorkspacePath", scope = EVENT)
    public String getCurrentPersonalWorkspace() {
        if (currentPersonalWorkspacePath == null && Boolean.TRUE.equals(isUserWorkspace)) {
            // Do not compute path if not necessary
            Path path = navigationContext.getCurrentDocument().getPath();
            String lastSegment = "";
            while (!path.isRoot() || !path.isEmpty()) {
                if (UserWorkspaceConstants.USERS_PERSONAL_WORKSPACES_ROOT.equals(path.lastSegment())) {
                    if (lastSegment.isEmpty()) {
                        currentPersonalWorkspacePath = "";
                    } else {
                        currentPersonalWorkspacePath = path.append(lastSegment).toString();
                    }
                    return currentPersonalWorkspacePath;
                } else {
                    lastSegment = path.lastSegment();
                }
                path = path.removeLastSegments(1);
            }
        }
        return currentPersonalWorkspacePath;
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED, EventNames.LOCATION_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void reset() {
        isUserWorkspace = null;
        currentPersonalWorkspacePath = null;
    }
}
