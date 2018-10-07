/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.platform.ui.web.api.WebActions.CURRENT_TAB_CHANGED_EVENT;
import static org.nuxeo.ecm.platform.ui.web.api.WebActions.CURRENT_TAB_SELECTED_EVENT;
import static org.nuxeo.ecm.user.invite.UserInvitationService.ValidationMethod.EMAIL;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserAdapter;
import org.nuxeo.ecm.platform.usermanager.UserAdapterImpl;
import org.nuxeo.ecm.platform.usermanager.exceptions.InvalidPasswordException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.ecm.user.invite.UserInvitationComponent;
import org.nuxeo.ecm.user.invite.UserInvitationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Handles users management related web actions.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
@Name("userManagementActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class UserManagementActions extends AbstractUserGroupManagement implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UserManagementActions.class);

    public static final String USERS_TAB = USER_CENTER_CATEGORY + ":" + USERS_GROUPS_HOME + ":" + "UsersHome";

    public static final String USERS_LISTING_CHANGED = "usersListingChanged";

    public static final String USERS_SEARCH_CHANGED = "usersSearchChanged";

    public static final String USER_SELECTED_CHANGED = "selectedUserChanged";

    public static final String SELECTED_LETTER_CHANGED = "selectedLetterChanged";

    protected String selectedLetter = "";

    protected DocumentModel selectedUser;

    protected DocumentModel newUser;

    protected boolean immediateCreation = false;

    protected boolean createAnotherUser = false;

    protected String defaultRepositoryName = null;

    protected String oldPassword;

    @Override
    protected String computeListingMode() {
        return userManager.getUserListingMode();
    }

    public DocumentModel getSelectedUser() {
        shouldResetStateOnTabChange = true;
        return selectedUser;
    }

    public void setSelectedUser(DocumentModel user) {
        fireSeamEvent(USER_SELECTED_CHANGED);
        selectedUser = user;
    }

    /**
     * @deprecated since version 5.5, use {@link #setSelectedUserName} instead.
     */
    @Deprecated
    public void setSelectedUser(String userName) {
        setSelectedUser(refreshUser(userName));
    }

    /**
     * UserRegistrationService userRegistrationService = Framework.getLocalService(UserRegistrationService.class);
     *
     * @since 5.5
     */
    public void setSelectedUserName(String userName) {
        setSelectedUser(refreshUser(userName));
    }

    public String getSelectedUserName() {
        return selectedUser.getId();
    }

    // refresh to get references
    protected DocumentModel refreshUser(String userName) {
        return userManager.getUserModel(userName);
    }

    public String getSelectedLetter() {
        return selectedLetter;
    }

    public void setSelectedLetter(String selectedLetter) {
        if (selectedLetter != null && !selectedLetter.equals(this.selectedLetter)) {
            this.selectedLetter = selectedLetter;
            fireSeamEvent(SELECTED_LETTER_CHANGED);
        }
        this.selectedLetter = selectedLetter;
    }

    public DocumentModel getNewUser() {
        if (newUser == null) {
            newUser = userManager.getBareUserModel();
        }
        return newUser;
    }

    public boolean getAllowEditUser() {
        return selectedUser != null && getCanEditUsers(true) && !BaseSession.isReadOnlyEntry(selectedUser);
    }

    protected boolean getCanEditUsers(boolean allowCurrentUser) {
        if (userManager.areUsersReadOnly()) {
            return false;
        }

        // if the selected user is the anonymous user, do not display
        // edit/password tabs
        if (selectedUser != null && userManager.getAnonymousUserId() != null
                && userManager.getAnonymousUserId().equals(selectedUser.getId())) {

            return false;
        }

        if (selectedUser != null) {
            NuxeoPrincipal selectedPrincipal = userManager.getPrincipal(selectedUser.getId());
            if (selectedPrincipal.isAdministrator() && !currentUser.isAdministrator()) {
                return false;
            }
        }

        if (webActions.checkFilter(USERS_GROUPS_MANAGEMENT_ACCESS_FILTER)) {
            return true;
        }
        if (allowCurrentUser && selectedUser != null) {
            if (currentUser.getName().equals(selectedUser.getId())) {
                return true;
            }
        }

        return false;
    }

    public boolean getAllowChangePassword() {
        return selectedUser != null && getCanEditUsers(true) && !BaseSession.isReadOnlyEntry(selectedUser);
    }

    public boolean getAllowCreateUser() {
        return getCanEditUsers(false);
    }

    public boolean getAllowDeleteUser() {
        return selectedUser != null && getCanEditUsers(false) && !BaseSession.isReadOnlyEntry(selectedUser);
    }

    public void clearSearch() {
        searchString = null;
        fireSeamEvent(USERS_SEARCH_CHANGED);
    }

    public void createUser() {
        try {
            if (immediateCreation) {
                // Create the user with password
                setSelectedUser(userManager.createUser(newUser));
                // Set the default value for the creation
                immediateCreation = false;
                facesMessages.add(StatusMessage.Severity.INFO,
                        resourcesAccessor.getMessages().get("info.userManager.userCreated"));
                if (createAnotherUser) {
                    showCreateForm = true;
                } else {
                    showCreateForm = false;
                    showUserOrGroup = true;
                    detailsMode = null;
                }
                fireSeamEvent(USERS_LISTING_CHANGED);
            } else {
                UserInvitationService userRegistrationService = Framework.getService(UserInvitationService.class);
                Map<String, Serializable> additionalInfos = new HashMap<String, Serializable>();
                additionalInfos.put(UserInvitationComponent.PARAM_ORIGINATING_USER , currentUser.getName());
                // Wrap the form as an invitation to the user
                UserAdapter newUserAdapter = new UserAdapterImpl(newUser, userManager);
                DocumentModel userRegistrationDoc = wrapToUserRegistration(newUserAdapter);
                userRegistrationService.submitRegistrationRequest(userRegistrationDoc, additionalInfos, EMAIL, true);

                facesMessages.add(StatusMessage.Severity.INFO,
                        resourcesAccessor.getMessages().get("info.userManager.userInvited"));
                if (createAnotherUser) {
                    showCreateForm = true;
                } else {
                    showCreateForm = false;
                    showUserOrGroup = false;
                    detailsMode = null;
                }

            }
            newUser = null;

        } catch (UserAlreadyExistsException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("error.userManager.userAlreadyExists"));
        } catch (InvalidPasswordException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("error.userManager.invalidPassword"));
        } catch (Exception e) {
            String message = e.getLocalizedMessage();
            if (e.getCause() != null) {
                message += e.getCause().getLocalizedMessage();
            }
            log.error(message, e);

            facesMessages.add(StatusMessage.Severity.ERROR, message);

        }
    }

    private String getDefaultRepositoryName() {
        if (defaultRepositoryName == null) {
            try {
                defaultRepositoryName = Framework.getService(RepositoryManager.class).getDefaultRepository().getName();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return defaultRepositoryName;
    }

    public void updateUser() {
        try {
            UpdateUserUnrestricted runner = new UpdateUserUnrestricted(getDefaultRepositoryName(), selectedUser);
            runner.runUnrestricted();
        } catch (InvalidPasswordException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("error.userManager.invalidPassword"));
        }

        detailsMode = DETAILS_VIEW_MODE;
        fireSeamEvent(USERS_LISTING_CHANGED);
    }

    public String changePassword() {
        try {
            updateUser();
        } catch (InvalidPasswordException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("error.userManager.invalidPassword"));
            return null;
        }
        detailsMode = DETAILS_VIEW_MODE;

        String message = resourcesAccessor.getMessages().get("label.userManager.password.changed");
        facesMessages.add(FacesMessage.SEVERITY_INFO, message);
        fireSeamEvent(USERS_LISTING_CHANGED);

        return null;
    }

    /**
     * @since 8.2
     */
    public String updateProfilePassword() {

        if (userManager.checkUsernamePassword(currentUser.getName(), oldPassword)) {

            try {
                Framework.doPrivileged(() -> userManager.updateUser(selectedUser));
            } catch (InvalidPasswordException e) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get("error.userManager.invalidPassword"));
                return null;
            }
        } else {
            String message = resourcesAccessor.getMessages().get("label.userManager.old.password.error");
            facesMessages.add(FacesMessage.SEVERITY_ERROR, message);
            return null;
        }

        String message = resourcesAccessor.getMessages().get("label.userManager.password.changed");
        facesMessages.add(FacesMessage.SEVERITY_INFO, message);
        detailsMode = DETAILS_VIEW_MODE;
        fireSeamEvent(USERS_LISTING_CHANGED);

        return null;
    }

    public void deleteUser() {
        userManager.deleteUser(selectedUser);
        selectedUser = null;
        showUserOrGroup = false;
        fireSeamEvent(USERS_LISTING_CHANGED);
    }

    public void validateUserName(FacesContext context, UIComponent component, Object value) {
        if (!(value instanceof String) || !StringUtils.containsOnly((String) value, VALID_CHARS)) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    ComponentUtils.translate(context, "label.userManager.wrong.username"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
    }

    /**
     * Verify that only administrators can add administrator groups.
     *
     * @param context
     * @param component
     * @param value
     * @since 5.9.2
     */
    public void validateGroups(FacesContext context, UIComponent component, Object value) {

        UIInput groupsComponent = getReferencedComponent("groupsValueHolderId", component);

        @SuppressWarnings("unchecked")
        List<String> groups = groupsComponent == null ? null : (List<String>) groupsComponent.getLocalValue();
        if (groups == null || groups.isEmpty()) {
            return;
        }
        if (!isAllowedToAdminGroups(groups)) {
            throwValidationException(context, "label.userManager.invalidGroupSelected");
        }
    }

    /**
     * Checks if the current user is allowed to aministrate (meaning add/remove) the given groups.
     *
     * @param groups
     * @return
     * @since 5.9.2
     */
    boolean isAllowedToAdminGroups(List<String> groups) {
        NuxeoPrincipalImpl nuxeoPrincipal = (NuxeoPrincipalImpl) currentUser;

        if (!nuxeoPrincipal.isAdministrator()) {
            List<String> adminGroups = getAllAdminGroups();

            for (String group : groups) {
                if (adminGroups.contains(group)) {
                    return false;
                }
            }

        }
        return true;
    }

    /**
     * Throw a validation exception with a translated message that is show in the UI.
     *
     * @param context the current faces context
     * @param message the error message
     * @param messageArgs the parameters for the message
     * @since 5.9.2
     */
    private void throwValidationException(FacesContext context, String message, Object... messageArgs) {
        FacesMessage fmessage = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                ComponentUtils.translate(context, message, messageArgs), null);
        throw new ValidatorException(fmessage);
    }

    /**
     * Return the value of the JSF component who's id is references in an attribute of the componet passed in parameter.
     *
     * @param attribute the attribute holding the target component id
     * @param component the component holding the attribute
     * @return the UIInput component, null otherwise
     * @since 5.9.2
     */
    private UIInput getReferencedComponent(String attribute, UIComponent component) {
        Map<String, Object> attributes = component.getAttributes();
        String targetComponentId = (String) attributes.get(attribute);

        if (targetComponentId == null) {
            log.error(String.format("Target component id (%s) not found in attributes", attribute));
            return null;
        }

        UIInput targetComponent = (UIInput) component.findComponent(targetComponentId);
        if (targetComponent == null) {
            return null;
        }

        return targetComponent;
    }

    public void validatePassword(FacesContext context, UIComponent component, Object value) {

        Object firstPassword = getReferencedComponent("firstPasswordInputId", component).getLocalValue();
        Object secondPassword = getReferencedComponent("secondPasswordInputId", component).getLocalValue();

        if (firstPassword == null || secondPassword == null) {
            log.error("Cannot validate passwords: value(s) not found");
            return;
        }

        if (!firstPassword.equals(secondPassword)) {
            throwValidationException(context, "label.userManager.password.not.match");
        }

    }

    private DocumentModel wrapToUserRegistration(UserAdapter newUserAdapter) {
        UserInvitationService userRegistrationService = Framework.getService(UserInvitationService.class);
        DocumentModel newUserRegistration = userRegistrationService.getUserRegistrationModel(null);

        // Map the values from the object filled in the form
        newUserRegistration.setPropertyValue(userRegistrationService.getConfiguration().getUserInfoUsernameField(),
                newUserAdapter.getName());
        newUserRegistration.setPropertyValue(userRegistrationService.getConfiguration().getUserInfoFirstnameField(),
                newUserAdapter.getFirstName());
        newUserRegistration.setPropertyValue(userRegistrationService.getConfiguration().getUserInfoLastnameField(),
                newUserAdapter.getLastName());
        newUserRegistration.setPropertyValue(userRegistrationService.getConfiguration().getUserInfoEmailField(),
                newUserAdapter.getEmail());
        newUserRegistration.setPropertyValue(userRegistrationService.getConfiguration().getUserInfoGroupsField(),
                newUserAdapter.getGroups().toArray());
        newUserRegistration.setPropertyValue(userRegistrationService.getConfiguration().getUserInfoCompanyField(),
                newUserAdapter.getCompany());

        String tenantId = newUserAdapter.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            tenantId = currentUser.getTenantId();
        }
        newUserRegistration.setPropertyValue(userRegistrationService.getConfiguration().getUserInfoTenantIdField(),
                tenantId);

        return newUserRegistration;
    }

    @Factory(value = "notReadOnly", scope = APPLICATION)
    public boolean isNotReadOnly() {
        return !Framework.isBooleanPropertyTrue("org.nuxeo.ecm.webapp.readonly.mode");
    }

    public List<String> getUserVirtualGroups(String userId) {
        NuxeoPrincipal principal = userManager.getPrincipal(userId);
        if (principal instanceof NuxeoPrincipalImpl) {
            NuxeoPrincipalImpl user = (NuxeoPrincipalImpl) principal;
            return user.getVirtualGroups();
        }
        return null;
    }

    public String viewUser(String userName) {
        webActions.setCurrentTabIds(MAIN_TAB_HOME + "," + USERS_TAB);
        setSelectedUser(userName);
        setShowUser(Boolean.TRUE.toString());
        return VIEW_HOME;
    }

    public String viewUser() {
        if (selectedUser != null) {
            return viewUser(selectedUser.getId());
        } else {
            return null;
        }
    }

    /**
     * @since 5.5
     */
    public void setShowUser(String showUser) {
        showUserOrGroup = Boolean.valueOf(showUser);
        // do not reset the state before actually viewing the user
        shouldResetStateOnTabChange = false;
    }

    protected void fireSeamEvent(String eventName) {
        Events evtManager = Events.instance();
        evtManager.raiseEvent(eventName);
    }

    @Factory(value = "anonymousUserDefined", scope = APPLICATION)
    public boolean anonymousUserDefined() {
        return userManager.getAnonymousUserId() != null;
    }

    @Observer(value = { USERS_LISTING_CHANGED })
    public void onUsersListingChanged() {
        contentViewActions.refreshOnSeamEvent(USERS_LISTING_CHANGED);
        contentViewActions.resetPageProviderOnSeamEvent(USERS_LISTING_CHANGED);
    }

    @Observer(value = { USERS_SEARCH_CHANGED })
    public void onUsersSearchChanged() {
        contentViewActions.refreshOnSeamEvent(USERS_SEARCH_CHANGED);
        contentViewActions.resetPageProviderOnSeamEvent(USERS_SEARCH_CHANGED);
    }

    @Observer(value = { SELECTED_LETTER_CHANGED })
    public void onSelectedLetterChanged() {
        contentViewActions.refreshOnSeamEvent(SELECTED_LETTER_CHANGED);
        contentViewActions.resetPageProviderOnSeamEvent(SELECTED_LETTER_CHANGED);
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
            CURRENT_TAB_SELECTED_EVENT + "_" + USERS_GROUPS_HOME_SUB_TAB })
    public void resetState() {
        if (shouldResetStateOnTabChange) {
            newUser = null;
            selectedUser = null;
            showUserOrGroup = false;
            showCreateForm = false;
            immediateCreation = false;
            detailsMode = DETAILS_VIEW_MODE;
        }
    }

    /**
     * @return The type of creation for the user.
     * @since 5.9.3
     */
    public boolean isImmediateCreation() {
        return immediateCreation;
    }

    /**
     * @param immediateCreation
     * @since 5.9.3
     */
    public void setImmediateCreation(boolean immediateCreation) {
        this.immediateCreation = immediateCreation;
    }

    public boolean isCreateAnotherUser() {
        return createAnotherUser;
    }

    public void setCreateAnotherUser(boolean createAnotherUser) {
        this.createAnotherUser = createAnotherUser;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
}
