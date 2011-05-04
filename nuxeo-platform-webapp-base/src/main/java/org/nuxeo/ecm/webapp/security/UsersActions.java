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

import java.io.Serializable;
import java.security.Principal;
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
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.web.Session;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * Handles users management related web actions.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 * @since 5.4.2
 */
@Name("usersActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class UsersActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UsersActions.class);

    public static final String VALID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-0123456789.@";

    public static final String DEFAULT_USER_LISTING_MODE = "search_only";

    public static final String USERS_LISTING_CHANGED = "usersListingChanged";

    public static final String USERS_SEARCH_CHANGED = "usersSearchChanged";

    public static final String SELECTED_LETTER_CHANGED = "selectedLetterChanged";

    public static final String USER_DETAILS_VIEW_MODE = "view";

    @In(create = true)
    protected Principal currentUser;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected ContentViewActions contentViewActions;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    protected DocumentModel selectedUser;

    protected String searchString = "";

    protected String selectedLetter = "";

    protected DocumentModel newUser;

    protected String userListingMode;

    protected String userDetailsMode;

    protected boolean showCreateUser;

    protected boolean showUser;

    public DocumentModel getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(String userName) throws ClientException {
        this.selectedUser = refreshUser(userName);
    }

    // refresh to get references
    protected DocumentModel refreshUser(String userName) throws ClientException {
        return userManager.getUserModel(userName);
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
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

    public String getUserListingMode() throws ClientException {
        if (userListingMode == null) {
            userListingMode = userManager.getUserListingMode();
            if (userListingMode == null || userListingMode.trim().isEmpty()) {
                userListingMode = DEFAULT_USER_LISTING_MODE;
            }
        }
        return userListingMode;
    }

    public void setUserListingMode(String userListingMode) {
        this.userListingMode = userListingMode;
    }

    public String getUserDetailsMode() {
        if (userDetailsMode == null) {
            userDetailsMode = USER_DETAILS_VIEW_MODE;
        }
        return userDetailsMode;
    }

    public void setUserDetailsMode(String mode) {
        userDetailsMode = mode;
    }

    public boolean isShowCreateUser() {
        return showCreateUser;
    }

    public void toggleShowCreateUser() {
        showCreateUser = !showCreateUser;
    }

    public boolean isShowUser() {
        return showUser;
    }

    public void toggleShowUser() {
        showUser = !showUser;
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
        if (currentUser instanceof NuxeoPrincipal) {
            NuxeoPrincipal pal = (NuxeoPrincipal) currentUser;
            if (pal.isAdministrator()) {
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
        try {
            selectedUser = userManager.createUser(newUser);
            newUser = null;
            facesMessages.add(
                    StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get(
                            "info.userManager.userCreated"));
            showCreateUser = false;
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
            userDetailsMode = USER_DETAILS_VIEW_MODE;
            fireSeamEvent(USERS_LISTING_CHANGED);
        } catch (Exception t) {
            throw ClientException.wrap(t);
        }
    }

    public String changePassword() throws ClientException {
        updateUser();
        userDetailsMode = USER_DETAILS_VIEW_MODE;

        String message = resourcesAccessor.getMessages().get(
                "label.userManager.password.changed");
        facesMessages.add(FacesMessage.SEVERITY_INFO, message);
        fireSeamEvent(USERS_LISTING_CHANGED);

        if (selectedUser.getId().equals(currentUser.getName())) {
            // If user changed HIS password, reset session
            Session.instance().invalidate();
            return navigationContext.goHome();
        } else {
            return null;
        }
    }

    public void deleteUser() throws ClientException {
        try {
            userManager.deleteUser(selectedUser);
            selectedUser = null;
            showUser = false;
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

    public String viewUser(String userName) throws ClientException {
        setSelectedUser(userName);
        showUser = true;
        // set the right tabs
        return "user_center";
    }

    protected void fireSeamEvent(String eventName) {
        Events evtManager = Events.instance();
        evtManager.raiseEvent(eventName);
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

}
