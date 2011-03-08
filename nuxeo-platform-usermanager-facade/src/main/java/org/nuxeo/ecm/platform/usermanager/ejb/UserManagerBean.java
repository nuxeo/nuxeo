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

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.UserManagerDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
@Stateless
@Remote(UserManager.class)
@Local(UserManagerLocal.class)
public class UserManagerBean implements UserManagerLocal {

    private static final long serialVersionUID = 1L;

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

    public void cleanup() {
    }

    public boolean checkUsernamePassword(String username, String password)
            throws ClientException {
        try {
            return getUserManager().checkUsernamePassword(username, password);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public boolean validatePassword(String password) throws ClientException {
        try {
            return getUserManager().validatePassword(password);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public NuxeoPrincipal getPrincipal(String username) throws ClientException {
        try {
            return getUserManager().getPrincipal(username);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<NuxeoPrincipal> searchPrincipals(String name)
            throws ClientException {
        try {
            return getUserManager().searchPrincipals(name);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<NuxeoGroup> searchGroups(String pattern) throws ClientException {
        try {
            return getUserManager().searchGroups(pattern);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public NuxeoGroup getGroup(String groupName) throws ClientException {
        try {
            return getUserManager().getGroup(groupName);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getDefaultGroup() {
        return getUserManager().getDefaultGroup();
    }

    public String getUserSortField() throws ClientException {
        try {
            return getUserManager().getUserSortField();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getUserListingMode() throws ClientException {
        try {
            return getUserManager().getUserListingMode();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getGroupListingMode() throws ClientException {
        try {
            return getUserManager().getGroupListingMode();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getUserDirectoryName() throws ClientException {
        try {
            return getUserManager().getUserDirectoryName();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getUserEmailField() throws ClientException {
        try {
            return getUserManager().getUserEmailField();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public Set<String> getUserSearchFields() throws ClientException {
        try {
            return getUserManager().getUserSearchFields();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getGroupDirectoryName() throws ClientException {
        try {
            return getUserManager().getGroupDirectoryName();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getGroupMembersField() throws ClientException {
        try {
            return getUserManager().getGroupMembersField();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getGroupSubGroupsField() throws ClientException {
        try {
            return getUserManager().getGroupSubGroupsField();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getGroupParentGroupsField() throws ClientException {
        try {
            return getUserManager().getGroupParentGroupsField();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<String> getGroupsInGroup(String parentId)
            throws ClientException {
        try {
            return getUserManager().getGroupsInGroup(parentId);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<String> getTopLevelGroups() throws ClientException {
        try {
            return getUserManager().getTopLevelGroups();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<String> getUsersInGroup(String groupId) throws ClientException {
        try {
            return getUserManager().getUsersInGroup(groupId);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<String> getUsersInGroupAndSubGroups(String groupId)
            throws ClientException {
        try {
            return getUserManager().getUsersInGroupAndSubGroups(groupId);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public Boolean areGroupsReadOnly() throws ClientException {
        try {
            return getUserManager().areGroupsReadOnly();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public Boolean areUsersReadOnly() throws ClientException {
        try {
            return getUserManager().areUsersReadOnly();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public Pattern getUserPasswordPattern() throws ClientException {
        try {
            return getUserManager().getUserPasswordPattern();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getAnonymousUserId() throws ClientException {
        try {
            return getUserManager().getAnonymousUserId();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    @Override
    public String getDigestAuthDirectory() throws ClientException {
        try {
            return getUserManager().getDigestAuthDirectory();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    @Override
    public String getDigestAuthRealm() throws ClientException {
        try {
            return getUserManager().getDigestAuthRealm();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void setConfiguration(UserManagerDescriptor descriptor)
            throws ClientException {
        try {
            getUserManager().setConfiguration(descriptor);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public DocumentModel getBareUserModel() throws ClientException {
        try {
            return getUserManager().getBareUserModel();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public DocumentModel createGroup(DocumentModel groupModel)
            throws ClientException {
        try {
            return getUserManager().createGroup(groupModel);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public DocumentModel createUser(DocumentModel userModel)
            throws ClientException {
        try {
            return getUserManager().createUser(userModel);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void deleteGroup(DocumentModel groupModel) throws ClientException {
        try {
            getUserManager().deleteGroup(groupModel);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void deleteUser(DocumentModel userModel) throws ClientException {
        try {
            getUserManager().deleteUser(userModel);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public DocumentModel getBareGroupModel() throws ClientException {
        try {
            return getUserManager().getBareGroupModel();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<String> getGroupIds() throws ClientException {
        try {
            return getUserManager().getGroupIds();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<String> getUserIds() throws ClientException {
        try {
            return getUserManager().getUserIds();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public DocumentModelList searchGroups(Map<String, Serializable> filter,
            HashSet<String> fulltext) throws ClientException {
        try {
            return getUserManager().searchGroups(filter, fulltext);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public DocumentModelList searchUsers(Map<String, Serializable> filter,
            Set<String> fulltext) throws ClientException {
        try {
            return getUserManager().searchUsers(filter, fulltext);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public DocumentModelList searchUsers(String pattern) throws ClientException {
        try {
            return getUserManager().searchUsers(pattern);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void updateGroup(DocumentModel groupModel) throws ClientException {
        try {
            getUserManager().updateGroup(groupModel);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void updateUser(DocumentModel userModel) throws ClientException {
        try {
            getUserManager().updateUser(userModel);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void deleteGroup(String groupId) throws ClientException {
        try {
            getUserManager().deleteGroup(groupId);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void deleteUser(String userId) throws ClientException {
        try {
            getUserManager().deleteUser(userId);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public DocumentModel getGroupModel(String groupName) throws ClientException {
        try {
            return getUserManager().getGroupModel(groupName);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public DocumentModel getUserModel(String userName) throws ClientException {
        try {
            return getUserManager().getUserModel(userName);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getGroupIdField() throws ClientException {
        try {
            return getUserManager().getGroupIdField();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getGroupSchemaName() throws ClientException {
        try {
            return getUserManager().getGroupSchemaName();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getUserIdField() throws ClientException {
        try {
            return getUserManager().getUserIdField();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public String getUserSchemaName() throws ClientException {
        try {
            return getUserManager().getUserSchemaName();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void createGroup(NuxeoGroup group) throws ClientException {
        try {
            getUserManager().createGroup(group);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void createPrincipal(NuxeoPrincipal principal)
            throws ClientException {
        try {
            getUserManager().createPrincipal(principal);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void deleteGroup(NuxeoGroup group) throws ClientException {
        try {
            getUserManager().deleteGroup(group);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void deletePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        try {
            getUserManager().deletePrincipal(principal);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<NuxeoGroup> getAvailableGroups() throws ClientException {
        try {
            return getUserManager().getAvailableGroups();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<NuxeoPrincipal> getAvailablePrincipals() throws ClientException {
        try {
            return getUserManager().getAvailablePrincipals();
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public DocumentModel getModelForUser(String name) throws ClientException {
        try {
            return getUserManager().getModelForUser(name);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<NuxeoPrincipal> searchByMap(Map<String, Serializable> filter,
            Set<String> pattern) throws ClientException {
        try {
            return getUserManager().searchByMap(filter, pattern);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void updateGroup(NuxeoGroup group) throws ClientException {
        try {
            getUserManager().updateGroup(group);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public void updatePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        try {
            getUserManager().updatePrincipal(principal);
        } catch (Throwable e) {
            throw ClientException.wrap(e);
        }
    }

    public List<String> getAdministratorsGroups() {
        return getUserManager().getAdministratorsGroups();
    }

    public String[] getUsersForPermission(String perm, ACP acp) {
        return getUserManager().getUsersForPermission(perm, acp);
    }

    @Override
    public boolean authenticate(String name, String password) {
        return getUserManager().authenticate(name, password);
    }

}
