/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
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

    @In(create = true)
    protected transient UserManager userManager;

    protected String mode = PROFILE_VIEW_MODE;

    protected DocumentModel userProfileDocument;

    protected DocumentModel currentUserProfile;

    public void updateUser() {
        if (userProfileDocument != null) {
            // Ensure to remove user schema from datamodel when saving changes
            // on user profile, otherwise an exception is thrown, see
            // NXP-11397.
            userProfileDocument.getDataModels().remove(userManager.getUserSchemaName());
            documentManager.saveDocument(userProfileDocument);
            documentManager.save();
        }

        // Update selected user after profile to prevent from
        // org.nuxeo.ecm.webapp.security.UserManagementActions.USER_SELECTED_CHANGED
        // event to reset userProfileDocument field.
        userManagementActions.updateUser();

        mode = PROFILE_VIEW_MODE;
    }

    public String getMode() {
        return mode;
    }

    public boolean getCanEdit() {
        return userManagementActions.getAllowEditUser() && userManagementActions.isNotReadOnly();
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

    public DocumentModel getUserProfileDocument() {
        // Need to set selectedUser in UserManagementActions to avoid an NPE
        // when calling updateUser() if UserManagementActions#selectedUser has
        // been set to null meanwhile (by opening a new tab for instance).
        getCurrentUserModel();
        if (userProfileDocument == null) {
            userProfileDocument = Framework.getService(UserProfileService.class).getUserProfileDocument(documentManager);
            String locale = (String) userProfileDocument.getPropertyValue(UserProfileConstants.USER_PROFILE_LOCALE);
            if (StringUtils.isEmpty(locale)) {
                String currentLocale = localeSelector.getLocaleString();
                if (!StringUtils.isEmpty(currentLocale)) {
                    userProfileDocument.setPropertyValue(UserProfileConstants.USER_PROFILE_LOCALE, currentLocale);
                }
            }
        }
        return userProfileDocument;
    }

    public DocumentModel getUserProfileDocument(String userName) {
        UserProfileService userProfileService = Framework.getService(UserProfileService.class);
        return userProfileService.getUserProfileDocument(userName, documentManager);
    }

    public DocumentModel getUserProfile() {
        if (currentUserProfile == null) {
            UserProfileService userProfileService = Framework.getService(UserProfileService.class);
            currentUserProfile = userProfileService.getUserProfile(getCurrentUserModel(), documentManager);
        }
        return currentUserProfile;
    }

    public DocumentModel getSelectedUserProfile() {
        DocumentModel selectedUser = userManagementActions.getSelectedUser();
        if (selectedUser == null) {
            return null;
        }
        if (userProfileDocument == null) {
            UserProfileService userProfileService = Framework.getService(UserProfileService.class);
            userProfileDocument = userProfileService.getUserProfile(selectedUser, documentManager);
        }
        return userProfileDocument;
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
            CURRENT_TAB_SELECTED_EVENT + "_" + USERS_GROUPS_HOME_SUB_TAB, USERS_LISTING_CHANGED, USER_SELECTED_CHANGED }, create = false)
    @BypassInterceptors
    public void resetState() {
        userProfileDocument = null;
        currentUserProfile = null;
    }
}
