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

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.security.UserManagementActions;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam component to manage user profile editing
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @since 5.5
 */
@Name("userProfileActions")
@Scope(ScopeType.CONVERSATION)
public class UserProfileActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PROFILE_VIEW_MODE = "view";

    public static final String PROFILE_EDIT_MODE = "edit";

    public static final String PROFILE_EDIT_PASSWORD_MODE = "editPassword";

    protected transient UserProfileService userProfileService;

    @In(create = true)
    protected transient UserManagementActions userManagementActions;

    @In(create = true)
    protected NuxeoPrincipal currentUser;

    @In(create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    protected String mode = PROFILE_VIEW_MODE;

    protected DocumentModel userProfileDocument;

    public void updateUser() throws ClientException {
        userManagementActions.updateUser();
        if (userProfileDocument != null) {
            documentManager.saveDocument(userProfileDocument);
            documentManager.save();
        }
        mode = PROFILE_VIEW_MODE;
    }

    public String getMode() throws ClientException {
        return mode;
    }

    public boolean getCanEdit() throws ClientException {
        return userManagementActions.getAllowEditUser()
                && userManagementActions.isNotReadOnly();
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public DocumentModel getCurrentUserModel() {
        DocumentModel currentUserModel = currentUser.getModel();
        userManagementActions.setSelectedUser(currentUserModel);
        return currentUserModel;
    }

    public DocumentModel getUserProfileDocument() throws ClientException {
        if (userProfileDocument == null) {
            userProfileDocument = getUserProfileService().getUserProfileDocument(
                    documentManager);
            String locale = (String) userProfileDocument.getPropertyValue(UserProfileConstants.USER_PROFILE_LOCALE);
            if (StringUtils.isEmpty(locale)) {
                userProfileDocument.setPropertyValue(
                        UserProfileConstants.USER_PROFILE_LOCALE,
                        localeSelector.getLocaleString());
            }
        }
        return userProfileDocument;
    }

    public DocumentModel getUserProfileDocument(String userName)
            throws ClientException {
        return getUserProfileService().getUserProfileDocument(userName,
                documentManager);
    }

    public DocumentModel getUserProfile() throws ClientException {
        return getUserProfileService().getUserProfile(getCurrentUserModel(),
                documentManager);
    }

    public DocumentModel getSelectedUserProfile() throws ClientException {
        DocumentModel selectedUser = userManagementActions.getSelectedUser();
        if (selectedUser == null) {
            return null;
        }
        return getUserProfileService().getUserProfile(selectedUser,
                documentManager);
    }

    protected UserProfileService getUserProfileService() throws ClientException {
        if (userProfileService == null) {
            try {
                userProfileService = Framework.getService(UserProfileService.class);
            } catch (Exception e) {
                throw new ClientException("Failed to get UserProfileService", e);
            }
        }
        return userProfileService;
    }

}
