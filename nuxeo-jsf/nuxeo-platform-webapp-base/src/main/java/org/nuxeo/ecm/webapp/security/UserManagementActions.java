/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.platform.ui.web.api.WebActions.CURRENT_TAB_CHANGED_EVENT;
import static org.nuxeo.ecm.platform.ui.web.api.WebActions.CURRENT_TAB_SELECTED_EVENT;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
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
public class UserManagementActions extends AbstractUserGroupManagement
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UserManagementActions.class);

    public static final String USERS_TAB = USER_CENTER_CATEGORY + ":"
            + USERS_GROUPS_HOME + ":" + "UsersHome";

    public static final String USERS_LISTING_CHANGED = "usersListingChanged";

    public static final String USERS_SEARCH_CHANGED = "usersSearchChanged";

    public static final String USER_SELECTED_CHANGED = "selectedUserChanged";

    public static final String SELECTED_LETTER_CHANGED = "selectedLetterChanged";

    protected String selectedLetter = "";

    protected DocumentModel selectedUser;

    protected DocumentModel newUser;

    @Override
    protected String computeListingMode() throws ClientException {
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
    public void setSelectedUser(String userName) throws ClientException {
        setSelectedUser(refreshUser(userName));
    }

    /**
     * @since 5.5
     */
    public void setSelectedUserName(String userName) throws ClientException {
        setSelectedUser(refreshUser(userName));
    }

    public String getSelectedUserName() throws ClientException {
        return selectedUser.getId();
    }

    // refresh to get references
    protected DocumentModel refreshUser(String userName) throws ClientException {
        return userManager.getUserModel(userName);
    }

    public String getSelectedLetter() {
        return selectedLetter;
    }

    public void setSelectedLetter(String selectedLetter) {
        if (selectedLetter != null
                && !selectedLetter.equals(this.selectedLetter)) {
            this.selectedLetter = selectedLetter;
            fireSeamEvent(SELECTED_LETTER_CHANGED);
        }
        this.selectedLetter = selectedLetter;
    }

    public DocumentModel getNewUser() throws ClientException {
        if (newUser == null) {
            newUser = userManager.getBareUserModel();
        }
        return newUser;
    }

    public boolean getAllowEditUser() throws ClientException {
        return getCanEditUsers(true)
                && !BaseSession.isReadOnlyEntry(selectedUser);
    }

    protected boolean getCanEditUsers(boolean allowCurrentUser)
            throws ClientException {
        if (userManager.areUsersReadOnly()) {
            return false;
        }

        // if the selected user is the anonymous user, do not display
        // edit/password tabs
        if (selectedUser != null
                && userManager.getAnonymousUserId() != null
                && userManager.getAnonymousUserId().equals(selectedUser.getId())) {

            return false;
        }

        if (selectedUser != null) {
            NuxeoPrincipal selectedPrincipal = userManager.getPrincipal(selectedUser.getId());
            if(selectedPrincipal.isAdministrator() && !((NuxeoPrincipal) currentUser).isAdministrator()) {
                return false;
            }
        }

        if (currentUser instanceof NuxeoPrincipal) {
            NuxeoPrincipal pal = (NuxeoPrincipal) currentUser;
            if (webActions.checkFilter(USERS_GROUPS_MANAGEMENT_ACCESS_FILTER)) {
                return true;
            }
            if (allowCurrentUser && selectedUser != null) {
                if (pal.getName().equals(selectedUser.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean getAllowChangePassword() throws ClientException {
        return getCanEditUsers(true)
                && !BaseSession.isReadOnlyEntry(selectedUser);
    }

    public boolean getAllowCreateUser() throws ClientException {
        return getCanEditUsers(false);
    }

    public boolean getAllowDeleteUser() throws ClientException {
        return getCanEditUsers(false)
                && !BaseSession.isReadOnlyEntry(selectedUser);
    }

    public void clearSearch() {
        searchString = null;
        fireSeamEvent(USERS_SEARCH_CHANGED);
    }

    public void createUser() throws ClientException {
        createUser(false);
    }

    public void createUser(boolean createAnotherUser) throws ClientException {
        try {
            setSelectedUser(userManager.createUser(newUser));
            newUser = null;
            facesMessages.add(
                    StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "info.userManager.userCreated"));
            if (createAnotherUser) {
                showCreateForm = true;
            } else {
                showCreateForm = false;
                showUserOrGroup = true;
                detailsMode = null;
            }
            fireSeamEvent(USERS_LISTING_CHANGED);
        } catch (UserAlreadyExistsException e) {
            facesMessages.add(
                    StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "error.userManager.userAlreadyExists"));
        }
    }

    public void updateUser() throws ClientException {
        try {
            userManager.updateUser(selectedUser);
            detailsMode = DETAILS_VIEW_MODE;
            fireSeamEvent(USERS_LISTING_CHANGED);
        } catch (Exception t) {
            throw ClientException.wrap(t);
        }
    }

    public String changePassword() throws ClientException {
        updateUser();
        detailsMode = DETAILS_VIEW_MODE;

        String message = resourcesAccessor.getMessages().get(
                "label.userManager.password.changed");
        facesMessages.add(FacesMessage.SEVERITY_INFO, message);
        fireSeamEvent(USERS_LISTING_CHANGED);

        return null;
    }

    public void deleteUser() throws ClientException {
        try {
            userManager.deleteUser(selectedUser);
            selectedUser = null;
            showUserOrGroup = false;
            fireSeamEvent(USERS_LISTING_CHANGED);
        } catch (Exception t) {
            throw ClientException.wrap(t);
        }
    }

    public void validateUserName(FacesContext context, UIComponent component,
            Object value) {
        if (!(value instanceof String)
                || !StringUtils.containsOnly((String) value, VALID_CHARS)) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.userManager.wrong.username"), null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
    }

    public void validatePassword(FacesContext context, UIComponent component,
            Object value) {
        Map<String, Object> attributes = component.getAttributes();
        String firstPasswordInputId = (String) attributes.get("firstPasswordInputId");
        String secondPasswordInputId = (String) attributes.get("secondPasswordInputId");
        if (firstPasswordInputId == null || secondPasswordInputId == null) {
            log.error("Cannot validate passwords: input id(s) not found");
            return;
        }

        UIInput firstPasswordComp = (UIInput) component.findComponent(firstPasswordInputId);
        UIInput secondPasswordComp = (UIInput) component.findComponent(secondPasswordInputId);
        if (firstPasswordComp == null || secondPasswordComp == null) {
            log.error("Cannot validate passwords: input(s) not found");
            return;
        }

        Object firstPassword = firstPasswordComp.getLocalValue();
        Object secondPassword = secondPasswordComp.getLocalValue();

        if (firstPassword == null || secondPassword == null) {
            log.error("Cannot validate passwords: value(s) not found");
            return;
        }

        if (!firstPassword.equals(secondPassword)) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.userManager.password.not.match"),
                    null);
            throw new ValidatorException(message);
        }

    }

    @Factory(value = "notReadOnly", scope = APPLICATION)
    public boolean isNotReadOnly() {
        return !"true".equals(Framework.getProperty(
                "org.nuxeo.ecm.webapp.readonly.mode", "false"));
    }

    public List<String> getUserVirtualGroups(String userId) throws Exception {
        NuxeoPrincipal principal = userManager.getPrincipal(userId);
        if (principal instanceof NuxeoPrincipalImpl) {
            NuxeoPrincipalImpl user = (NuxeoPrincipalImpl) principal;
            return user.getVirtualGroups();
        }
        return null;
    }

    public String viewUser() throws ClientException {
        if (selectedUser != null) {
            return viewUser(selectedUser.getId());
        } else {
            return null;
        }
    }

    public String viewUser(String userName) throws ClientException {
        webActions.setCurrentTabIds(MAIN_TAB_HOME + "," + USERS_TAB);
        setSelectedUser(userName);
        showUserOrGroup = true;
        // do not reset the state before actually viewing the user
        shouldResetStateOnTabChange = false;
        return VIEW_HOME;
    }

    /**
     * @since 5.5
     */
    public void setShowUser(String showUser) {
        showUserOrGroup = Boolean.valueOf(showUser);
    }

    protected void fireSeamEvent(String eventName) {
        Events evtManager = Events.instance();
        evtManager.raiseEvent(eventName);
    }

    @Factory(value = "anonymousUserDefined", scope = APPLICATION)
    public boolean anonymousUserDefined() throws ClientException {
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
            detailsMode = DETAILS_VIEW_MODE;
        }
    }

}
