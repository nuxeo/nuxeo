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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.login.mockedServices;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.UserManagerDescriptor;

public class MockUserManager implements UserManager {

    private static final long serialVersionUID = 1L;

    public Boolean areGroupsReadOnly() throws ClientException {
        return null;
    }

    public Boolean areUsersReadOnly() throws ClientException {
        return null;
    }

    public boolean checkUsernamePassword(String username, String password)
            throws ClientException {
        return false;
    }

    public DocumentModel createGroup(DocumentModel groupModel)
            throws ClientException {
        return null;
    }

    public void createGroup(NuxeoGroup group) throws ClientException {
    }

    public void createPrincipal(NuxeoPrincipal principal)
            throws ClientException {
    }

    public DocumentModel createUser(DocumentModel userModel)
            throws ClientException {
        return null;
    }

    public void deleteGroup(DocumentModel groupModel) throws ClientException {
    }

    public void deleteGroup(String groupId) throws ClientException {
    }

    public void deleteGroup(NuxeoGroup group) throws ClientException {
    }

    public void deletePrincipal(NuxeoPrincipal principal)
            throws ClientException {
    }

    public void deleteUser(DocumentModel userModel) throws ClientException {
    }

    public void deleteUser(String userId) throws ClientException {
    }

    public String getAnonymousUserId() throws ClientException {
        return "Anonymous";
    }

    public List<NuxeoGroup> getAvailableGroups() throws ClientException {
        return null;
    }

    public List<NuxeoPrincipal> getAvailablePrincipals() throws ClientException {
        return null;
    }

    public DocumentModel getBareGroupModel() throws ClientException {
        return null;
    }

    public DocumentModel getBareUserModel() throws ClientException {
        return null;
    }

    public String getDefaultGroup() {
        return null;
    }

    public NuxeoGroup getGroup(String groupName) throws ClientException {
        return null;
    }

    public String getGroupDirectoryName() throws ClientException {
        return null;
    }

    public String getGroupIdField() throws ClientException {
        return null;
    }

    public List<String> getGroupIds() throws ClientException {
        return null;
    }

    public String getGroupListingMode() throws ClientException {
        return null;
    }

    public String getGroupMembersField() throws ClientException {
        return null;
    }

    public DocumentModel getGroupModel(String groupName) throws ClientException {
        return null;
    }

    public String getGroupParentGroupsField() throws ClientException {
        return null;
    }

    public String getGroupSchemaName() throws ClientException {
        return null;
    }

    public String getGroupSubGroupsField() throws ClientException {
        return null;
    }

    public List<String> getGroupsInGroup(String parentId)
            throws ClientException {
        return null;
    }

    public DocumentModel getModelForUser(String name) throws ClientException {
        return null;
    }

    public NuxeoPrincipal getPrincipal(String username) throws ClientException {
        return null;
    }

    public List<String> getTopLevelGroups() throws ClientException {
        return null;
    }

    public String getUserDirectoryName() throws ClientException {
        return null;
    }

    public String getUserEmailField() throws ClientException {
        return null;
    }

    public String getUserIdField() throws ClientException {
        return null;
    }

    public List<String> getUserIds() throws ClientException {
        return null;
    }

    public String getUserListingMode() throws ClientException {
        return null;
    }

    public DocumentModel getUserModel(String userName) throws ClientException {
        return null;
    }

    public Pattern getUserPasswordPattern() throws ClientException {
        return null;
    }

    public String getUserSchemaName() throws ClientException {
        return null;
    }

    public Set<String> getUserSearchFields() throws ClientException {
        return null;
    }

    public String getUserSortField() throws ClientException {
        return null;
    }

    public List<String> getUsersInGroup(String groupId) throws ClientException {
        return null;
    }

    public List<String> getUsersInGroupAndSubGroups(String groupId)
            throws ClientException {
        return null;
    }

    public List<NuxeoPrincipal> searchByMap(Map<String, Serializable> filter,
            Set<String> pattern) throws ClientException {
        return null;
    }

    public List<NuxeoGroup> searchGroups(String pattern) throws ClientException {
        return null;
    }

    public DocumentModelList searchGroups(Map<String, Serializable> filter,
            HashSet<String> fulltext) throws ClientException {
        return null;
    }

    public List<NuxeoPrincipal> searchPrincipals(String pattern)
            throws ClientException {
        return null;
    }

    public DocumentModelList searchUsers(String pattern) throws ClientException {
        return null;
    }

    public DocumentModelList searchUsers(Map<String, Serializable> filter,
            Set<String> fulltext) throws ClientException {
        return null;
    }

    public void setConfiguration(UserManagerDescriptor descriptor)
            throws ClientException {
    }

    public void updateGroup(DocumentModel groupModel) throws ClientException {
    }

    public void updateGroup(NuxeoGroup group) throws ClientException {
    }

    public void updatePrincipal(NuxeoPrincipal principal)
            throws ClientException {
    }

    public void updateUser(DocumentModel userModel) throws ClientException {
    }

    public boolean validatePassword(String password) throws ClientException {
        return false;
    }

    public List<String> getAdministratorsGroups() {
        throw new UnsupportedOperationException();
    }

}
