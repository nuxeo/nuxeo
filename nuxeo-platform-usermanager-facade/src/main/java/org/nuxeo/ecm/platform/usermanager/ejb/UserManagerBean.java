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

package org.nuxeo.ecm.platform.usermanager.ejb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Stateful;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
@Stateful
@SerializedConcurrentAccess
@Remote(UserManager.class)
@Local(UserManagerLocal.class)
public class UserManagerBean implements UserManager {

    private static final Log log = LogFactory.getLog(UserManagerBean.class);

    private final EJBExceptionHandler exceptionHandler = new EJBExceptionHandler();

    private String defaultGroup;

    private transient UserManager userManager;

    @PostActivate
    @PostConstruct
    public void initialize() {
        // UserService userService = (UserService) Framework.getRuntime()
        // .getComponent(UserService.NAME);
        // userManager = userService.getUserManager();
        getUserManager();
    }

    private UserManager getUserManager() {
        if (userManager == null) {
            userManager = Framework.getLocalService(UserManager.class);
        }

        return userManager;
    }

    @PrePassivate
    public void cleanup() {
        userManager = null;
    }

    public boolean checkUsernamePassword(String username, String password)
            throws ClientException {
        try {
            return getUserManager().checkUsernamePassword(username, password);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public boolean validatePassword(String password) {
        return getUserManager().validatePassword(password);
    }

    public List<NuxeoPrincipal> getAvailablePrincipals() throws ClientException {
        try {
            return getUserManager().getAvailablePrincipals();
        } catch (Throwable e) {
            log.error("getAvailablePrincipals failed", e);
            throw exceptionHandler.wrapException(e);
        }
    }

    public NuxeoPrincipal getPrincipal(String username) throws ClientException {
        try {
            return getUserManager().getPrincipal(username);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void remove() {
        // TODO Auto-generated method stub
    }

    public void createGroup(NuxeoGroup group) throws ClientException {
        try {
            getUserManager().createGroup(group);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void createPrincipal(NuxeoPrincipal principal)
            throws ClientException {
        try {
            getUserManager().createPrincipal(principal);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void updatePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        try {
            getUserManager().updatePrincipal(principal);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void deleteGroup(NuxeoGroup group) throws ClientException {
        try {
            getUserManager().deleteGroup(group);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void deletePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        try {
            getUserManager().deletePrincipal(principal);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public List<NuxeoGroup> getAvailableGroups() throws ClientException {
        try {
            return getUserManager().getAvailableGroups();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void updateGroup(NuxeoGroup group) throws ClientException {
        try {
            getUserManager().updateGroup(group);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public List<NuxeoPrincipal> searchPrincipals(String name)
            throws ClientException {
        try {
            return getUserManager().searchPrincipals(name);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public List<NuxeoPrincipal> searchByMap(Map<String, Object> filter,
            Set<String> pattern) throws ClientException {
        try {
            return getUserManager().searchByMap(filter, pattern);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public List<NuxeoGroup> searchGroups(String pattern) throws ClientException {
        try {
            return getUserManager().searchGroups(pattern);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public NuxeoGroup getGroup(String groupName) throws ClientException {
        try {
            return getUserManager().getGroup(groupName);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public DocumentModel getModelForUser(String name) throws ClientException {
        try {
            return getUserManager().getModelForUser(name);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public void setRootLogin(String defaultRootLogin) {
        getUserManager().setRootLogin(defaultRootLogin);
    }

    public void setUserSortField(String sortField) {
        getUserManager().setUserSortField(sortField);
    }

    public String getUserSortField() {
        return getUserManager().getUserSortField();
    }

    public void setGroupSortField(String sortField) {
        getUserManager().setGroupSortField(sortField);
    }

    public String getUserListingMode() {
        return getUserManager().getUserListingMode();
    }

    public void setUserListingMode(String userListingMode) {
        getUserManager().setUserListingMode(userListingMode);
    }

    public String getGroupListingMode() {
        return getUserManager().getGroupListingMode();
    }

    public void setGroupListingMode(String groupListingMode) {
        getUserManager().setGroupListingMode(groupListingMode);
    }

    public void setUserDirectoryName(String userDirectoryName) {
        getUserManager().setUserDirectoryName(userDirectoryName);
    }

    public String getUserDirectoryName() {
        return getUserManager().getUserDirectoryName();
    }

    public void setUserEmailField(String userEmailField) {
        getUserManager().setUserEmailField(userEmailField);
    }

    public String getUserEmailField() {
        return getUserManager().getUserEmailField();
    }

    public void setUserSearchFields(Set<String> userSearchFields) {
        getUserManager().setUserSearchFields(userSearchFields);
    }

    public Set<String> getUserSearchFields() {
        return getUserManager().getUserSearchFields();
    }

    public void setGroupDirectoryName(String groupDirectoryName) {
        getUserManager().setGroupDirectoryName(groupDirectoryName);
    }

    public String getGroupDirectoryName() {
        return getUserManager().getGroupDirectoryName();
    }

    public void setGroupMembersField(String groupMembersField) {
        getUserManager().setGroupMembersField(groupMembersField);
    }

    public String getGroupMembersField() {
        return getUserManager().getGroupMembersField();
    }

    public void setGroupSubGroupsField(String groupSubGroupsField) {
        getUserManager().setGroupSubGroupsField(groupSubGroupsField);
    }

    public String getGroupSubGroupsField() {
        return getUserManager().getGroupSubGroupsField();
    }

    public void setGroupParentGroupsField(String groupParentGroupsField) {
        getUserManager().setGroupParentGroupsField(groupParentGroupsField);
    }

    public String getGroupParentGroupsField() {
        return getUserManager().getGroupParentGroupsField();
    }

    public List<String> getGroupsInGroup(String parentId)
            throws ClientException {
        try {
            return getUserManager().getGroupsInGroup(parentId);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public List<String> getTopLevelGroups() throws ClientException {
        try {
            return getUserManager().getTopLevelGroups();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public List<String> getUsersInGroup(String groupId) throws ClientException {
        try {
            return getUserManager().getUsersInGroup(groupId);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public Boolean areGroupsReadOnly() {
        try {
            return getUserManager().areGroupsReadOnly();
        } catch (Throwable e) {
            log.debug("Error while getting userManager.areGroupsReadOnly()", e);
            return Boolean.FALSE;
        }
    }

    public Boolean areUsersReadOnly() {
        try {
            return getUserManager().areUsersReadOnly();
        } catch (Throwable e) {
            log.debug("Error while getting userManager.areUsersReadOnly()", e);
            return Boolean.FALSE;
        }
    }

    public Pattern getUserPasswordPattern() {
        return getUserManager().getUserPasswordPattern();
    }

    public void setUserPasswordPattern(Pattern userPasswordPattern) {
        getUserManager().setUserPasswordPattern(userPasswordPattern);
    }

    public void setAnonymousUser(Map<String, String> anonymousUser) {
        getUserManager().setAnonymousUser(anonymousUser);
    }

    public String getAnonymousUserId() {
        return getUserManager().getAnonymousUserId();
    }

}
