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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Seam;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.directory.SizeLimitExceededException;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Name("userManagerActions")
@Scope(CONVERSATION)
public class UserManagerActionsBean extends InputController implements
        UserManagerActions, Serializable {

    private static final long serialVersionUID = 2160735474991874750L;
    private static final Log log = LogFactory.getLog(UserManagerActionsBean.class);

    private static final String ALL = "all";

    private static final String TABBED = "tabbed";

    private static final String SEARCH_ONLY = "search_only";

    public static final String VALID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-0123456789";

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected String searchString = "";
    protected String searchUsername = "";
    protected String searchLastname = "";
    protected String searchFirstname = "";
    protected String searchCompany = "";
    protected String searchEmail = "";

    protected boolean doSearch = false;

    @In
    protected transient Context sessionContext;

    // curent viewable users (on the selected letter tab)
    @DataModel("userList")
    protected List<NuxeoPrincipal> users;

    private List<NuxeoPrincipal> allUsers;

    // hash that maps the firstLetter to user lists
    private Map<String, List<NuxeoPrincipal>> userCatalog;

    private NuxeoPrincipal principal;

    private String changed_password;
    private String changed_password_verify;

    //@In(required = false)
    @DataModelSelection("userList")
    protected NuxeoPrincipal selectedUser;

    @In(required = false)
    protected NuxeoPrincipal newUser;

    private String retypedPassword;

    @RequestParameter("usernameParam")
    protected String usernameParam;

    @RequestParameter("newSelectedLetter")
    protected String newSelectedLetter;

    private String selectedLetter;

    private boolean searchOverflow = false;

    protected String userListingMode;


    @Create
    public void initialize() throws ClientException {
        log.info("Initializing...");
        principal = (NuxeoPrincipal) FacesContext.getCurrentInstance()
                .getExternalContext().getUserPrincipal();
        //principalIsAdmin = principal.isAdministrator();
        userListingMode = userManager.getUserListingMode();
    }

    public void destroy() {
        log.debug("Removing SEAM action listener...");
    }

    @Factory("userList")
    public void getUsers() throws ClientException {
        if (SEARCH_ONLY.equals(userListingMode)) {
            allUsers = Collections.emptyList();
            users = Collections.emptyList();
        } else {
            try {
                allUsers = userManager.getAvailablePrincipals();
                updateUserCatalog();
            } catch (SizeLimitExceededException e) {
                allUsers = Collections.emptyList();
                users = Collections.emptyList();
                searchOverflow = true;
            } catch (Exception t) {
                throw EJBExceptionHandler.wrapException(t);
            }
        }
    }

    public String viewUser(String userName) throws ClientException {
        // try to display the requested user info
        final NuxeoPrincipal nxprinc = userManager.getPrincipal(userName);
        if (nxprinc != null) {
            selectedUser = nxprinc;
        } else {
            log.error("No principal for username: " + usernameParam);
            return null;
        }

        try {
            sessionContext.set("selectedUser", selectedUser);
            return "view_user";
        } catch (Exception t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    public String viewUser() throws ClientException {
        if (usernameParam != null) {
            return viewUser(usernameParam);
        }
        try {
            refreshPrincipal(selectedUser);
            sessionContext.set("selectedUser", selectedUser);
            return "view_user";
        } catch (Exception t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    /**
     * Gets the general type of User from the xml definition.
     *
     * @return
     */
    public Type getChangeableUserType() {
        return typeManager.getType("User");
    }

    public Type getChangeableUserCreateType() {
        return typeManager.getType("UserCreate");
    }

    public void refreshPrincipal(NuxeoPrincipal principal)
            throws ClientException {
        NuxeoPrincipal freshPrincipal = userManager.getPrincipal(
                principal.getName());
        principal.setGroups(freshPrincipal.getGroups());
        principal.setRoles(freshPrincipal.getRoles());
        principal.setModel(freshPrincipal.getModel());
    }

    public String editUser() throws ClientException {
        try {
            refreshPrincipal(selectedUser);
            sessionContext.set("selectedUser", selectedUser);
            return "edit_user";
        } catch (Exception t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    public String deleteUser() throws ClientException {
        try {
            userManager.deletePrincipal(selectedUser);
            if (allUsers != null) {
                allUsers.remove(selectedUser);
            }
            if (users != null) {
                users.remove(selectedUser);
            }


            Events.instance().raiseEvent(EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED);

            return viewUsers();
        } catch (Exception t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    public List<SelectItem> getAvailableGroups() throws ClientException {
        List<SelectItem> selectItemList = new ArrayList<SelectItem>();
        for (NuxeoGroup group : userManager.getAvailableGroups()) {
            String groupName = group.getName();
            selectItemList.add(new SelectItem(groupName));
        }
        return selectItemList;
    }

    public String searchUsers() throws ClientException {
        searchOverflow = false;
        try {
            if (searchString.compareTo("*") == 0) {
                allUsers = userManager.getAvailablePrincipals();
            } else {
                allUsers = userManager.searchPrincipals(searchString);
            }
        } catch (SizeLimitExceededException e) {
            searchOverflow = true;
            allUsers = Collections.emptyList();
            users = Collections.emptyList();
            return "view_users";
        }
        doSearch = true;
        return viewUsers();
    }

    private void updateUserCatalog() throws ClientException {
        if (allUsers == null) {
            allUsers = userManager.searchPrincipals(searchString);
        }

        if (StringUtils.isEmpty(searchString) && TABBED.equals(userListingMode)) {
            userCatalog = new HashMap<String, List<NuxeoPrincipal>>();
            String userSortField = userManager.getUserSortField();
            for (NuxeoPrincipal principal : allUsers) {
                // FIXME: this should use a "display name" dedicated API
                String displayName = null;
                if (userSortField != null) {
                    // XXX hack, principals have only one model
                    org.nuxeo.ecm.core.api.DataModel dm = principal.getModel().getDataModels().values().iterator().next();
                    displayName = (String) dm.getData(userSortField);
                }
                if (displayName == null) {
                    displayName = principal.getName();
                }
                String firstLetter = displayName.substring(0, 1).toUpperCase();
                List<NuxeoPrincipal> list = userCatalog.get(firstLetter);
                if (list == null) {
                    list = new ArrayList<NuxeoPrincipal>();
                    userCatalog.put(firstLetter, list);
                }
                list.add(principal);
            }

            if (StringUtils.isEmpty(selectedLetter)
                    || !userCatalog.containsKey(selectedLetter)) {
                selectedLetter = getCatalogLetters().iterator().next();
            }

            users = userCatalog.get(selectedLetter);
            if (users == null) {
                users = Collections.emptyList();
            }

        } else {
            userCatalog = null;
            users = new ArrayList<NuxeoPrincipal>(allUsers);
        }
    }

    public String updateUser() throws ClientException {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
/*            if (selectedUser.getPassword() != null) {
                if (!selectedUser.getPassword().equals(retypedPassword)) {
                    String message = ComponentUtils.translate(context,
                            "error.userManager.passwordMismatch");
                    FacesMessages.instance().add(message);
                    return null;
                }
            }
*/
            if ("".equals(selectedUser.getPassword())) {
                selectedUser.setPassword(null);
            }
            userManager.updatePrincipal(selectedUser);
            return viewUser(selectedUser.getName());
        } catch (Exception t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    public String saveUser() throws ClientException {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            // XXX hack, principals have only one model
            org.nuxeo.ecm.core.api.DataModel dm = newUser.getModel().getDataModels().values().iterator().next();

            String username = (String) dm.getData(NuxeoPrincipalImpl.USERNAME_COLUMN);
            if (!StringUtils.containsOnly(username, VALID_CHARS)) {
                String message = ComponentUtils.translate(context,
                        "label.userManager.wrong.username");

                FacesMessages.instance().add(FacesMessage.SEVERITY_ERROR, message, (Object[]) null);
                return null;
            }

            String newUserPassword = (String) dm.getData(NuxeoPrincipalImpl.PASSWORD_COLUMN);
            if (!newUserPassword.equals(changed_password_verify)) {
                String message = ComponentUtils.translate(context,
                        "label.userManager.password.not.match");

                FacesMessages.instance().add("h_inputText_passwordCreate2",
                        FacesMessage.SEVERITY_ERROR, message, (Object[]) null);

                return null;
            }

            userManager.createPrincipal(newUser);

            selectedUser = newUser;

            return viewUser();

        } catch (UserAlreadyExistsException ex) {
            String message = ComponentUtils.translate(context,
                    "error.userManager.userAlreadyExists");
            facesMessages.add(FacesMessage.SEVERITY_WARN, message);
            return null;

        } catch (Exception t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    // this is called directly by JSF code when switching tabs
    // DOH!
    public String createUser() throws ClientException {
        try {
            newUser = new NuxeoPrincipalImpl("");
            // We need to put an appropriate datamodel in the user.
            // Find the schema name from the layout (which is
            // what the UI will use to fill it in).
            Type userType = getChangeableUserCreateType();
            String schemaName = userType.getLayout()[0].getSchemaName();
            DataModelImpl dm = new DataModelImpl(schemaName);
            DocumentModelImpl entry = new DocumentModelImpl(null, userType.getId(), "",
                    null, null, null, new String[] { schemaName }, null);
            entry.addDataModel(dm);
            newUser.setModel(entry);
            newUser.getRoles().add("regular");
            sessionContext.set("newUser", newUser);
            return "create_user";
        } catch (Exception t) {
            throw EJBExceptionHandler.wrapException(t);
        }
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

    public boolean getAllowCreateUser() throws ClientException {
        return principal.isAdministrator() && !userManager.areUsersReadOnly();
    }

    public boolean getAllowDeleteUser() throws ClientException {
        return principal.isAdministrator() && !userManager.areUsersReadOnly();
    }

    public String viewUsers() throws ClientException {
        if (newSelectedLetter != null) {
            selectedLetter = newSelectedLetter;
        }

        if (SEARCH_ONLY.equals(userListingMode)) {
            if (StringUtils.isEmpty(searchString)
                    && StringUtils.isEmpty(searchUsername)
                    && StringUtils.isEmpty(searchFirstname)
                    && StringUtils.isEmpty(searchLastname)
                    && StringUtils.isEmpty(searchEmail)
                    && StringUtils.isEmpty(searchCompany)) {
                allUsers = Collections.emptyList();
                users = Collections.emptyList();
                return "view_users";
            }
        }

        try {
            updateUserCatalog();
        } catch (SizeLimitExceededException e) {
            allUsers = Collections.emptyList();
            users = Collections.emptyList();
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

    public boolean getAllowEditUser() {
        return principal.isAdministrator()
                || principal.getName().equals(selectedUser.getName());
    }

    public boolean getAllowChangePassword() throws ClientException {
        if (userManager.areUsersReadOnly()) {
            return false;
        }

        return principal.isAdministrator()
                || principal.getName().equals(selectedUser.getName());
    }

    public String getRetypedPassword() {
        return retypedPassword;
    }

    public void setRetypedPassword(String retypedPassword) {
        this.retypedPassword = retypedPassword;
    }

    public String clearSearch() throws ClientException {
        searchString = "";
        searchEmail = "";
        searchFirstname = "";
        searchLastname = "";
        searchUsername = "";
        doSearch = false;

        return searchUsers();
    }

    @PrePassivate
    public void saveState() {
        log.info("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.info("PostActivate");
    }

    public String getSearchEmail() {
        return searchEmail;
    }

    public void setSearchEmail(String email) {
        searchEmail = email;
    }

    public String getSearchFirstname() {
        return searchFirstname;
    }

    public void setSearchFirstname(String firstName) {
        searchFirstname = firstName;
    }

    public String getSearchLastname() {
        return searchLastname;
    }

    public void setSearchLastname(String lastName) {
        searchLastname = lastName;
    }

    public String getSearchUsername() {
        return searchUsername;
    }

    public void setSearchUsername(String username) {
        searchUsername = username;
    }

    public String getSearchCompany() {
        return searchCompany;
    }

    public void setSearchCompany(String company) {
        searchCompany = company;
    }

    public String searchUsersAdvanced() throws ClientException {
        searchOverflow = false;
        try {
            Map<String, Object> filter = new HashMap<String, Object>();
            if ((searchUsername + searchLastname + searchFirstname + searchEmail + searchCompany).trim()
                    .compareTo("*") == 0) {
                allUsers = userManager.getAvailablePrincipals();

            } else {
                if (searchUsername != null && !"".equals(searchUsername)) {
                    filter.put(NuxeoPrincipalImpl.USERNAME_COLUMN, searchUsername);
                }
                if (searchLastname != null && !"".equals(searchLastname)) {
                    filter.put(NuxeoPrincipalImpl.LASTNAME_COLUMN, searchLastname);
                }
                if (searchFirstname != null && !"".equals(searchFirstname)) {
                    filter.put(NuxeoPrincipalImpl.FIRSTNAME_COLUMN, searchFirstname);
                }
                if (searchEmail != null && !"".equals(searchEmail)) {
                    filter.put(NuxeoPrincipalImpl.EMAIL_COLUMN, searchEmail);
                }
                if (searchCompany != null && !"".equals(searchCompany)) {
                    filter.put(NuxeoPrincipalImpl.COMPANY_COLUMN, searchCompany);
                }

                // create a new set because a HashMap.KeySet is not serializable
                allUsers = userManager.searchByMap(filter, new HashSet<String>(
                        filter.keySet()));
            }
        } catch (SizeLimitExceededException e) {
            searchOverflow = true;
            allUsers = Collections.emptyList();
            users = Collections.emptyList();
            return "view_users";
        }

        doSearch = true;
        return viewUsers();
    }

    public String clearSearchAdvanced() throws ClientException {
        searchEmail = "";
        searchFirstname = "";
        searchLastname = "";
        searchUsername = "";
        doSearch = false;

        return viewUsers();
    }

    public boolean getDoSearch() {
        return doSearch;
    }

    public void setDoSearch(boolean doSearch) {
        this.doSearch = doSearch;
    }

    public void setChanged_password(String changed_password) {
        this.changed_password = changed_password;
    }

    public void setChanged_password_verify(String changed_password_verify) {
        this.changed_password_verify = changed_password_verify;
    }

    public String getChanged_password() {
        return "";
    }

    public String getChanged_password_verify() {
        return "";
    }

    public boolean isSearchOverflow() {
        return searchOverflow;
    }

    public void setSearchOverflow(boolean searchOverflow) {
        this.searchOverflow = searchOverflow;
    }

    public String changePassword() throws ClientException {
        FacesContext context = FacesContext.getCurrentInstance();

        if (changed_password.equals(changed_password_verify)
                && changed_password.length() > 0) {
            selectedUser.setPassword(changed_password);
            userManager.updatePrincipal(selectedUser);

            String message = ComponentUtils.translate(context,
                    "label.userManager.password.changed");

            facesMessages.add(FacesMessage.SEVERITY_INFO, message);

            if (selectedUser.getName().equals(principal.getName())) {
                // If user chaged HIS password reset session
                // HttpServletRequest httpRequest = (HttpServletRequest)
                // context.getExternalContext().getRequest();
                // httpRequest.getSession().invalidate();
                Seam.invalidateSession();
                return "home";
            } else {
                return "view_user";
            }
        }

        String message = ComponentUtils.translate(context,
                "label.userManager.password.not.match");

        FacesMessages.instance().add("h_inputText_password1",
                FacesMessage.SEVERITY_ERROR, message, (Object[]) null);

        return null;
    }

}
