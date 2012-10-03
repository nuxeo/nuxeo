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

import static org.nuxeo.ecm.platform.ui.web.api.WebActions.CURRENT_TAB_CHANGED_EVENT;
import static org.nuxeo.ecm.platform.ui.web.api.WebActions.CURRENT_TAB_SELECTED_EVENT;
import static org.nuxeo.ecm.webapp.security.AbstractUserGroupManagement.MAIN_TABS_CATEGORY;
import static org.nuxeo.ecm.webapp.security.AbstractUserGroupManagement.NUXEO_ADMIN_CATEGORY;
import static org.nuxeo.ecm.webapp.security.AbstractUserGroupManagement.USERS_GROUPS_HOME_SUB_TAB;
import static org.nuxeo.ecm.webapp.security.AbstractUserGroupManagement.USERS_GROUPS_MANAGER_SUB_TAB;
import static org.nuxeo.ecm.webapp.security.AbstractUserGroupManagement.USER_CENTER_CATEGORY;
import static org.nuxeo.ecm.webapp.security.UserManagementActions.USERS_LISTING_CHANGED;
import static org.nuxeo.ecm.webapp.security.UserManagementActions.USER_SELECTED_CHANGED;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
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
        if (userProfileDocument != null) {
            documentManager.saveDocument(userProfileDocument);
            documentManager.save();
        }

        // Update selected user after profile to prevent from
        // org.nuxeo.ecm.webapp.security.UserManagementActions.USER_SELECTED_CHANGED
        // event to reset userProfileDocument field.
        userManagementActions.updateUser();

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
        DocumentModel selectedUser = userManagementActions.getSelectedUser();
        DocumentModel currentUserModel = currentUser.getModel();
        if (selectedUser == null || !selectedUser.getId().equals(currentUserModel.getId())) {
            userManagementActions.setSelectedUser(currentUserModel);
        }
        return currentUserModel;
    }

    public DocumentModel getUserProfileDocument() throws ClientException {
        // Need to set selectedUser in UserManagementActions to avoid an NPE
        // when calling updateUser() if UserManagementActions#selectedUser has
        // been set to null meanwhile (by opening a new tab for instance).
        getCurrentUserModel();
        if (userProfileDocument == null) {
            userProfileDocument = getUserProfileService().getUserProfileDocument(
                    documentManager);
            String locale = (String) userProfileDocument.getPropertyValue(UserProfileConstants.USER_PROFILE_LOCALE);
            if (StringUtils.isEmpty(locale)) {
                String currentLocale = localeSelector.getLocaleString();
                if (!StringUtils.isEmpty(currentLocale)) {
                    userProfileDocument.setPropertyValue(
                            UserProfileConstants.USER_PROFILE_LOCALE,
                            currentLocale);
                }
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
        if (userProfileDocument == null) {
            userProfileDocument = getUserProfileService().getUserProfile(
                    selectedUser, documentManager);
        }
        return userProfileDocument;
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

    @Observer(value = { CURRENT_TAB_CHANGED_EVENT + "_" + MAIN_TABS_CATEGORY,
            CURRENT_TAB_CHANGED_EVENT + "_" + NUXEO_ADMIN_CATEGORY,
            CURRENT_TAB_CHANGED_EVENT + "_" + USER_CENTER_CATEGORY,
            CURRENT_TAB_CHANGED_EVENT + "_" + USERS_GROUPS_MANAGER_SUB_TAB,
            CURRENT_TAB_CHANGED_EVENT + "_" + USERS_GROUPS_HOME_SUB_TAB,
            CURRENT_TAB_SELECTED_EVENT + "_" + MAIN_TABS_CATEGORY,
            CURRENT_TAB_SELECTED_EVENT + "_" + NUXEO_ADMIN_CATEGORY,
            CURRENT_TAB_SELECTED_EVENT + "_" + USER_CENTER_CATEGORY,
            CURRENT_TAB_SELECTED_EVENT + "_" + USERS_GROUPS_MANAGER_SUB_TAB,
            CURRENT_TAB_SELECTED_EVENT + "_" + USERS_GROUPS_HOME_SUB_TAB,
            USERS_LISTING_CHANGED, USER_SELECTED_CHANGED }, create = false)
    @BypassInterceptors
    public void resetState() {
        userProfileDocument = null;
    }
}
