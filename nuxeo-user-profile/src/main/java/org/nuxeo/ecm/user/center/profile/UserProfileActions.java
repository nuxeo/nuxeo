/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.user.center.profile;

import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_FACET;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.webapp.security.UserManagementActions;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam component to manage user profile editing
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @since 5.4.3
 */
@Name("userProfileActions")
@Scope(ScopeType.CONVERSATION)
public class UserProfileActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PROFILE_VIEW_MODE = "view";

    public static final String PROFILE_EDIT_MODE = "edit";

    public static final String PROFILE_EDIT_PASSWORD_MODE = "editPassword";

    @In(create = true)
    protected transient UserManagementActions userManagementActions;

    @In(create = true)
    protected NuxeoPrincipal currentUser;

    @In(create = true)
    protected transient CoreSession documentManager;

    protected String mode = PROFILE_VIEW_MODE;

    protected DocumentModel userProfileDocument;

    public void updateUser() throws ClientException {
        userManagementActions.updateUser();
        documentManager.saveDocument(userProfileDocument);
        documentManager.save();
        mode = PROFILE_VIEW_MODE;
    }

    public String getMode() throws ClientException {
        userManagementActions.setSelectedUser(currentUser.getName());
        return mode;
    }

    public boolean getCanEdit() throws ClientException {
        userManagementActions.setSelectedUser(currentUser.getName());
        return userManagementActions.getAllowEditUser() && userManagementActions.isNotReadOnly();
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public DocumentModel getUserProfileDocument() throws ClientException {
        if (userProfileDocument == null) {
            try {
                UserWorkspaceService userWorkspaceService = Framework.getService(UserWorkspaceService.class);
                if (userWorkspaceService != null) {
                    userProfileDocument = userWorkspaceService.getCurrentUserPersonalWorkspace(
                            documentManager, null);

                }
            } catch (Exception e) {
                throw new ClientException("Failed to get UserWorkspaceService",
                        e);
            }
        }
        if (!userProfileDocument.hasFacet(USER_PROFILE_FACET)) {
            userProfileDocument.addFacet(USER_PROFILE_FACET);
            userProfileDocument = documentManager.saveDocument(userProfileDocument);
        }
        return userProfileDocument;
    }

    public void setUserProfileDocument(DocumentModel userProfileDocument) {
        this.userProfileDocument = userProfileDocument;
    }

}
