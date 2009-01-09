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

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

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
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.web.Session;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

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
    private transient UserManager userManager;

    @In(create = true)
    protected Principal currentUser;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    private DocumentModelList allUsers;

    /**
     * Map with first letter as key and users list as value
     */
    private Map<String, DocumentModelList> userCatalog;

    /**
     * Current viewable users (on the selected letter tab)
     */
    @DataModel("userList")
    private DocumentModelList users;

    @DataModelSelection("userList")
    private DocumentModel selectedUser;

    private String userListingMode;

    private DocumentModel newUserModel;

    private DocumentModel searchUserModel;

    private String searchString = "";

    private boolean doSearch = false;

    @RequestParameter("newSelectedLetter")
    private String newSelectedLetter;

    private String selectedLetter;

    private boolean searchOverflow = false;

    public String getUserListingMode() throws ClientException {
        if (userListingMode == null) {
            userListingMode = userManager.getUserListingMode();
        }
        return userListingMode;
    }

    @Factory("userList")
    public void getUsers() throws ClientException {
        if (SEARCH_ONLY.equals(getUserListingMode())) {
            allUsers = new DocumentModelListImpl();
            users = new DocumentModelListImpl();
        } else {
            try {
                allUsers = userManager.searchUsers(null);
                updateUserCatalog();
            } catch (SizeLimitExceededException e) {
                allUsers = new DocumentModelListImpl();
                users = new DocumentModelListImpl();
                searchOverflow = true;
            } catch (Exception t) {
                throw ClientException.wrap(t);
            }
        }
    }

    private void updateUserCatalog() throws ClientException {
        if (allUsers == null) {
            allUsers = userManager.searchUsers(searchString);
        }

        if (StringUtils.isEmpty(searchString)
                && TABBED.equals(getUserListingMode())) {
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
                if (displayName == null) {
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

            if (StringUtils.isEmpty(selectedLetter)
                    || !userCatalog.containsKey(selectedLetter)) {
                selectedLetter = getCatalogLetters().iterator().next();
            }

            users = userCatalog.get(selectedLetter);
            if (users == null) {
                users = new DocumentModelListImpl();
            }

        } else {
            userCatalog = null;
            users = new DocumentModelListImpl(allUsers);
        }
    }

    public String viewUser(String userName) throws ClientException {
        final NuxeoPrincipal principal = userManager.getPrincipal(userName);
        if (principal == null) {
            log.error("No principal for username: " + userName);
            return null;
        } else {
            return viewUser(principal.getModel());
        }
    }

    public DocumentModel getSelectedUser() throws ClientException {
        return selectedUser;
    }

    public String viewUser() throws ClientException {
        refreshUser(selectedUser);
        return viewUser(selectedUser);
    }

    protected String viewUser(DocumentModel user) throws ClientException {
        selectedUser = refreshUser(user);
        if (user != null) {
            return "view_user";
        } else {
            return null;
        }
    }

    protected DocumentModel refreshUser(DocumentModel user)
            throws ClientException {
        return userManager.getUserModel(user.getId());
    }

    public String editUser() throws ClientException {
        selectedUser = refreshUser(selectedUser);
        return "edit_user";
    }

    public String deleteUser() throws ClientException {
        try {
            userManager.deleteUser(selectedUser);
            if (allUsers != null) {
                allUsers.remove(selectedUser);
            }
            if (users != null) {
                users.remove(selectedUser);
            }

            // XXX: Why?
            Events.instance().raiseEvent(
                    EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED);

            return viewUsers();
        } catch (Exception t) {
            throw ClientException.wrap(t);
        }
    }

    public String searchUsers() throws ClientException {
        searchOverflow = false;
        try {
            if (searchString.compareTo("*") == 0) {
                allUsers = userManager.searchUsers(null);
            } else {
                allUsers = userManager.searchUsers(searchString);
            }
        } catch (SizeLimitExceededException e) {
            searchOverflow = true;
            allUsers = new DocumentModelListImpl();
            users = new DocumentModelListImpl();
            return "view_users";
        }
        doSearch = true;
        return viewUsers();
    }

    public String updateUser() throws ClientException {
        try {
            userManager.updateUser(selectedUser);
            return viewUser(selectedUser.getName());
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
            ((EditableValueHolder) component).setValid(false);
            context.addMessage(component.getClientId(context), message);
            // also add global message
            context.addMessage(null, message);
        }
    }

    @RequestParameter
    protected String firstPasswordInputId;

    @RequestParameter
    protected String secondPasswordInputId;

    public void validatePassword(FacesContext context, UIComponent component,
            Object value) {
        Map<String, Object> attributes = component.getAttributes();
        firstPasswordInputId = (String) attributes.get("firstPasswordInputId");
        secondPasswordInputId = (String) attributes.get("secondPasswordInputId");
        UIInput firstPasswordComp = (UIInput) component.findComponent(firstPasswordInputId);
        UIInput secondPasswordComp = (UIInput) component.findComponent(secondPasswordInputId);
        String firstPassword = (String) firstPasswordComp.getLocalValue();
        String secondPassword = (String) secondPasswordComp.getLocalValue();

        if (!firstPassword.equals(secondPassword)) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, ComponentUtils.translate(
                            context, "label.userManager.password.not.match"),
                    null);
            secondPasswordComp.setValid(false);
            context.addMessage(secondPasswordComp.getClientId(context), message);
        }
    }

    public String createUser() throws ClientException,
            UserAlreadyExistsException {
        try {
            newUserModel = userManager.createUser(newUserModel);
            selectedUser = newUserModel;
            newUserModel = null;
            return viewUser();
        } catch (UserAlreadyExistsException e) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "error.userManager.userAlreadyExists"));
            return null;
        }
    }

    public DocumentModel getNewUserModel() throws ClientException {
        if (newUserModel == null) {
            newUserModel = userManager.getBareUserModel();
        }
        return newUserModel;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public Collection<String> getCatalogLetters() {
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
        if (newSelectedLetter != null) {
            selectedLetter = newSelectedLetter;
        }

        try {
            updateUserCatalog();
        } catch (SizeLimitExceededException e) {
            allUsers = new DocumentModelListImpl();
            users = new DocumentModelListImpl();
            searchOverflow = true;
            return "view_users";
        }

        if (userCatalog != null) {
            // "TABBED"
            return "view_many_users";
        } else {
            // "ALL"
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
                if (pal.getName().equals(selectedUser.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean getAllowEditUser() throws ClientException {
        return getCanEditUsers(true);
    }

    public boolean getAllowChangePassword() throws ClientException {
        return getCanEditUsers(true);
    }

    public boolean getAllowCreateUser() throws ClientException {
        return getCanEditUsers(false);
    }

    public boolean getAllowDeleteUser() throws ClientException {
        return getCanEditUsers(false);
    }

    public String clearSearch() throws ClientException {
        searchUserModel = null;
        doSearch = false;
        return searchUsers();
    }

    public DocumentModel getSearchUserModel() throws ClientException {
        if (searchUserModel == null) {
            searchUserModel = userManager.getBareUserModel();
        }
        return searchUserModel;
    }

    public String searchUsersAdvanced() throws ClientException {
        searchOverflow = false;
        try {
            // XXX hack, directory entries have only one model
            org.nuxeo.ecm.core.api.DataModel dm = searchUserModel.getDataModels().values().iterator().next();
            Map<String, Object> filter = dm.getMap();
            // create a new set because a HashMap.KeySet is not serializable
            allUsers = userManager.searchUsers(filter, new HashSet<String>(
                    filter.keySet()));
        } catch (SizeLimitExceededException e) {
            searchOverflow = true;
            allUsers = new DocumentModelListImpl();
            users = new DocumentModelListImpl();
            return "view_users";
        }

        doSearch = true;
        return viewUsers();
    }

    public String clearSearchAdvanced() throws ClientException {
        searchUserModel = null;
        doSearch = false;
        return viewUsers();
    }

    public boolean getDoSearch() {
        return doSearch;
    }

    public void setDoSearch(boolean doSearch) {
        this.doSearch = doSearch;
    }

    public boolean isSearchOverflow() {
        return searchOverflow;
    }

    public void setSearchOverflow(boolean searchOverflow) {
        this.searchOverflow = searchOverflow;
    }

    public String changePassword() throws ClientException {
        updateUser();

        String message = resourcesAccessor.getMessages().get(
                "label.userManager.password.changed");
        facesMessages.add(FacesMessage.SEVERITY_INFO, message);

        if (selectedUser.getName().equals(currentUser.getName())) {
            // If user changed HIS password, reset session
            Session.instance().invalidate();
            return navigationContext.goHome();
        } else {
            return "view_user";
        }
    }
}
