/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.APPLICATION;
import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.web.Session;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Name("userManagerActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class UserManagerActionsBean implements UserManagerActions {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UserManagerActionsBean.class);

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected Principal currentUser;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    /**
     * Map with first letter as key and users list as value
     */
    protected Map<String, DocumentModelList> userCatalog;

    /**
     * Current viewable users (on the selected letter tab)
     */
    @DataModel(value="userList")
    protected DocumentModelList users;

    @DataModelSelection("userList")
    protected DocumentModel selectedUser;

    protected String userListingMode;

    protected DocumentModel newUser;

    protected DocumentModel searchUserModel;

    protected String searchString = "";

    @RequestParameter("newSelectedLetter")
    protected String newSelectedLetter;

    protected String selectedLetter;

    protected boolean searchOverflow = false;

    public String getUserListingMode() throws ClientException {
        if (userListingMode == null) {
            userListingMode = userManager.getUserListingMode();
        }
        return userListingMode;
    }

    @Factory(value = "userList")
    public DocumentModelList getUsers() throws ClientException {
        if (users == null) {
            searchOverflow = false;
            try {
                String userListingMode = getUserListingMode();
                if (SEARCH_ONLY.equals(userListingMode)
                        || !StringUtils.isEmpty(getTrimmedSearchString())) {
                    if ("*".equals(getTrimmedSearchString())) {
                        users = userManager.searchUsers(null);
                    } else if (!StringUtils.isEmpty(getTrimmedSearchString())) {
                        users = userManager.searchUsers(getTrimmedSearchString());
                    }
                } else if (TABBED.equals(userListingMode)) {
                    if (userCatalog == null) {
                        updateUserCatalog();
                    }
                    if (StringUtils.isEmpty(selectedLetter)
                            || !userCatalog.containsKey(selectedLetter)) {
                        Collection<String> catalogLetters = getCatalogLetters();
                        if (!catalogLetters.isEmpty()) {
                            selectedLetter = catalogLetters.iterator().next();
                        }
                    }
                    users = userCatalog.get(selectedLetter);
                }
            } catch (SizeLimitExceededException e) {
                searchOverflow = true;
            } catch (Exception t) {
                throw ClientException.wrap(t);
            }
        }
        if (users == null) {
            users = new DocumentModelListImpl();
        }
        return users;
    }

    public void resetUsers() {
        users = null;
        // FIXME: update only the new letter, added following the creation
        if (TABBED.equals(userListingMode)) {
            userCatalog = null;
        }
    }

    protected void updateUserCatalog() throws ClientException {
        // XXX: should filter all users on searchString?
        DocumentModelList allUsers = userManager.searchUsers(null);
        userCatalog = new HashMap<String, DocumentModelList>();
        String userSortField = userManager.getUserSortField();
        for (DocumentModel user : allUsers) {
            // FIXME: this should use a "display name" dedicated API
            String displayName = null;
            if (userSortField != null) {
                // XXX hack, principals have only one model
                org.nuxeo.ecm.core.api.DataModel dm = user.getDataModels().values().iterator().next();
                displayName = (String) dm.getData(userSortField);
            }
            if (StringUtils.isEmpty(displayName)) {
                displayName = user.getId();
            }
            String firstLetter = displayName.substring(0, 1).toUpperCase();
            DocumentModelList list = userCatalog.get(firstLetter);
            if (list == null) {
                list = new DocumentModelListImpl();
                userCatalog.put(firstLetter, list);
            }
            list.add(user);
        }
    }

    public DocumentModel getSelectedUser() {
        return selectedUser;
    }

    // refresh to get references
    protected DocumentModel refreshUser(String userName) throws ClientException {
        return userManager.getUserModel(userName);
    }

    protected String viewUser(DocumentModel user, boolean refresh)
            throws ClientException {
        if (user != null) {
            selectedUser = user;
            if (refresh) {
                selectedUser = refreshUser(user.getId());
            }
            if (selectedUser != null) {
                return "view_user";
            }
        }
        return null;

    }

    public String viewUser() throws ClientException {
        return viewUser(selectedUser, true);
    }

    public String viewUser(String userName) throws ClientException {
        return viewUser(userManager.getUserModel(userName), false);
    }

    public String searchUsers() throws ClientException {
        searchOverflow = false;
        // just reset users so that users list is refreshed
        resetUsers();
        return viewUsers();
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

    public String editUser() throws ClientException {
        selectedUser = refreshUser(selectedUser.getId());
        return "edit_user";
    }

    public String deleteUser() throws ClientException {
        try {
            userManager.deleteUser(selectedUser);
            // refresh users and groups list
            resetUsers();
            return viewUsers();
        } catch (Exception t) {
            throw ClientException.wrap(t);
        }
    }

    public String updateUser() throws ClientException {
        try {
            userManager.updateUser(selectedUser);
            // refresh users and groups list
            resetUsers();
            return viewUser(selectedUser.getId());
        } catch (Exception t) {
            throw ClientException.wrap(t);
        }
    }

    public String changePassword() throws ClientException {
        updateUser();

        String message = resourcesAccessor.getMessages().get(
                "label.userManager.password.changed");
        facesMessages.add(FacesMessage.SEVERITY_INFO, message);

        if (selectedUser.getId().equals(currentUser.getName())) {
            // If user changed HIS password, reset session
            Session.instance().invalidate();
            return navigationContext.goHome();
        } else {
            return "view_user";
        }
    }

    public String createUser() throws ClientException,
            UserAlreadyExistsException {
        try {
            selectedUser = userManager.createUser(newUser);
            newUser = null;
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("info.userManager.userCreated"));
            resetUsers();
            return viewUser();
        } catch (UserAlreadyExistsException e) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "error.userManager.userAlreadyExists"));
            return null;
        }

    }

    public DocumentModel getNewUser() throws ClientException {
        if (newUser == null) {
            newUser = userManager.getBareUserModel();
        }
        return newUser;
    }

    public String getSearchString() {
        return searchString;
    }

    protected String getTrimmedSearchString() {
        if (searchString == null) {
            return null;
        }
        return searchString.trim();
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public Collection<String> getCatalogLetters() {
        if (userCatalog == null) {
            try {
                updateUserCatalog();
            } catch (ClientException e) {
                log.error("Unable to update user catalog", e);
                return Collections.emptyList();
            }
        }
        List<String> list = new ArrayList<String>(userCatalog.keySet());
        Collections.sort(list);
        return list;
    }

    public void setSelectedLetter(String selectedLetter) {
        this.selectedLetter = selectedLetter;
    }

    public String getSelectedLetter() {
        return selectedLetter;
    }

    public String viewUsers() throws ClientException {
        if (newSelectedLetter != null
                && !newSelectedLetter.equals(selectedLetter)) {
            selectedLetter = newSelectedLetter;
            // just reset users so that users list is refreshed
            resetUsers();
        }
        String userListingMode = getUserListingMode();
        if (TABBED.equals(userListingMode)) {
            return "view_many_users";
        } else {
            return "view_users";
        }
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

    public boolean getAllowEditUser() throws ClientException {
        return getCanEditUsers(true) && !BaseSession.isReadOnlyEntry(selectedUser);
    }

    public boolean getAllowChangePassword() throws ClientException {
        return getCanEditUsers(true) && !BaseSession.isReadOnlyEntry(selectedUser);
    }

    public boolean getAllowCreateUser() throws ClientException {
        return getCanEditUsers(false);
    }

    public boolean getAllowDeleteUser() throws ClientException {
        return getCanEditUsers(false) && !BaseSession.isReadOnlyEntry(selectedUser);
    }

    public String clearSearch() throws ClientException {
        searchString = null;
        searchUserModel = null;
        return searchUsers();
    }

    public boolean isSearchOverflow() {
        return searchOverflow;
    }

    // XXX: never used, not tested
    public DocumentModel getSearchUserModel() throws ClientException {
        if (searchUserModel == null) {
            searchUserModel = userManager.getBareUserModel();
        }
        return searchUserModel;
    }

    // XXX: never used, not tested
    public String searchUsersAdvanced() throws ClientException {
        searchOverflow = false;
        try {
            // XXX hack, directory entries have only one model
            org.nuxeo.ecm.core.api.DataModel dm = searchUserModel.getDataModels().values().iterator().next();
            Map<String, Serializable> filter = mkSerializableMap(dm.getMap());
            // create a new set because a HashMap.KeySet is not serializable
            users = userManager.searchUsers(filter, new HashSet<String>(
                    filter.keySet()));
        } catch (SizeLimitExceededException e) {
            searchOverflow = true;
            users = new DocumentModelListImpl();
            return "view_users";
        }
        return viewUsers();
    }

    // XXX: never used, not tested
    public String clearSearchAdvanced() throws ClientException {
        searchUserModel = null;
        return viewUsers();
    }

    protected static Map<String, Serializable> mkSerializableMap(Map<String, Object> map) {
        Map<String, Serializable> serializableMap = null;
        if (map != null) {
            serializableMap = new HashMap<String, Serializable>();
            for (String key : map.keySet()) {
                serializableMap.put(key, (Serializable) map.get(key));
            }
        }
        return serializableMap;
    }

    @Factory(value = "notReadOnly", scope = APPLICATION)
    public boolean isNotReadOnly() {
        return !"true".equals(Framework.getProperty("org.nuxeo.ecm.webapp.readonly.mode", "false"));
    }

    public List<String> getUserVirtualGroups(String userId) throws Exception {

        NuxeoPrincipal principal = userManager.getPrincipal(userId);
        if (principal instanceof NuxeoPrincipalImpl) {
            NuxeoPrincipalImpl user = (NuxeoPrincipalImpl) principal;
            return user.getVirtualGroups();
        }
        return null;
    }

    /*
     * ----- Methods for AJAX calls, do not return anything to avoid redirect -----
     */

    public void setSelectedUser(DocumentModel user) throws ClientException {
        selectedUser = refreshUser(user.getId());
    }

    public void deleteUserNoRedirect() throws ClientException {
        deleteUser();
        resetUsers();
    }

    public void createUserNoRedirect() throws ClientException {
        createUser();
        resetUsers();
    }

    public void updateUserNoRedirect() throws ClientException {
        updateUser();
        resetUsers();
    }

}
