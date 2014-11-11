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
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
@Stateless
@Remote(UserManager.class)
@Local(UserManagerLocal.class)
public class UserManagerBean implements UserManager {

    private static final Log log = LogFactory.getLog(UserManagerBean.class);

    private static final EJBExceptionHandler exceptionHandler = new EJBExceptionHandler();

    private transient UserManager userManager;

    @PostConstruct
    public void initialize() {
        getUserManager();
    }

    private UserManager getUserManager() {
        if (userManager == null) {
            userManager = Framework.getLocalService(UserManager.class);
        }
        return userManager;
    }

    public void cleanup() {}

    public boolean checkUsernamePassword(String username, String password)
            throws ClientException {
        try {
            return getUserManager().checkUsernamePassword(username, password);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public boolean validatePassword(String password) throws ClientException {
        try {
            return getUserManager().validatePassword(password);
        } catch (Throwable e) {
            log.error("getAvailablePrincipals failed", e);
            throw exceptionHandler.wrapException(e);
        }
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

    public void remove() {}

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
        return getUserManager().getDefaultGroup();
    }

    public void setDefaultGroup(String defaultGroup) {

        getUserManager().setDefaultGroup(defaultGroup);
    }

    public void setRootLogin(String defaultRootLogin) throws ClientException {
        try {
            getUserManager().setRootLogin(defaultRootLogin);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setUserSortField(String sortField) throws ClientException {
        try {
            getUserManager().setUserSortField(sortField);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public String getUserSortField() throws ClientException {
        try {
            return getUserManager().getUserSortField();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setGroupSortField(String sortField) throws ClientException {
        try {
            getUserManager().setGroupSortField(sortField);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public String getUserListingMode() throws ClientException {
        try {
            return getUserManager().getUserListingMode();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setUserListingMode(String userListingMode)
            throws ClientException {
        try {
            getUserManager().setUserListingMode(userListingMode);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public String getGroupListingMode() throws ClientException {
        try {
            return getUserManager().getGroupListingMode();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setGroupListingMode(String groupListingMode)
            throws ClientException {
        try {
            getUserManager().setGroupListingMode(groupListingMode);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setUserDirectoryName(String userDirectoryName)
            throws ClientException {
        try {
            getUserManager().setUserDirectoryName(userDirectoryName);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public String getUserDirectoryName() throws ClientException {
        try {
            return getUserManager().getUserDirectoryName();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setUserEmailField(String userEmailField) throws ClientException {
        try {
            getUserManager().setUserEmailField(userEmailField);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public String getUserEmailField() throws ClientException {
        try {
            return getUserManager().getUserEmailField();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setUserSearchFields(Set<String> userSearchFields)
            throws ClientException {
        try {
            getUserManager().setUserSearchFields(userSearchFields);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setUserSearchFields(Map<String, MatchType> userSearchFields)
            throws ClientException {
        try {
            getUserManager().setUserSearchFields(userSearchFields);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public Set<String> getUserSearchFields() throws ClientException {
        try {
            return getUserManager().getUserSearchFields();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setGroupDirectoryName(String groupDirectoryName)
            throws ClientException {
        try {
            getUserManager().setGroupDirectoryName(groupDirectoryName);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public String getGroupDirectoryName() throws ClientException {
        try {
            return getUserManager().getGroupDirectoryName();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setGroupMembersField(String groupMembersField)
            throws ClientException {
        try {
            getUserManager().setGroupMembersField(groupMembersField);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public String getGroupMembersField() throws ClientException {
        try {
            return getUserManager().getGroupMembersField();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setGroupSubGroupsField(String groupSubGroupsField)
            throws ClientException {
        try {
            getUserManager().setGroupSubGroupsField(groupSubGroupsField);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public String getGroupSubGroupsField() throws ClientException {
        try {
            return getUserManager().getGroupSubGroupsField();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setGroupParentGroupsField(String groupParentGroupsField)
            throws ClientException {
        try {
            getUserManager().setGroupParentGroupsField(groupParentGroupsField);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public String getGroupParentGroupsField() throws ClientException {
        try {
            return getUserManager().getGroupParentGroupsField();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
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

    public Boolean areGroupsReadOnly() throws ClientException {
        try {
            return getUserManager().areGroupsReadOnly();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public Boolean areUsersReadOnly() throws ClientException {
        try {
            return getUserManager().areUsersReadOnly();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public Pattern getUserPasswordPattern() throws ClientException {
        try {
            return getUserManager().getUserPasswordPattern();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setUserPasswordPattern(Pattern userPasswordPattern) throws ClientException {
        try {
            getUserManager().setUserPasswordPattern(userPasswordPattern);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public void setAnonymousUser(Map<String, String> anonymousUser) throws ClientException {
        try {
            getUserManager().setAnonymousUser(anonymousUser);
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

    public String getAnonymousUserId() throws ClientException {
        try {
            return getUserManager().getAnonymousUserId();
        } catch (Throwable e) {
            throw exceptionHandler.wrapException(e);
        }
    }

}
