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
 *     Florent Guillaume
 *
 * $Id: FakeUserManagerImpl.java 28010 2007-12-07 19:23:44Z fguillaume $
 */

package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * @author Florent Guillaume
 *
 */
public class FakeUserManagerImpl implements UserManager {

    private static final long serialVersionUID = 1L;

    String userListingMode;

    String groupListingMode;

    List<String> defaultAdministratorIds;

    List<String> administratorsGroups;

    String defaultGroup;

    String userSortField;

    String groupSortField;

    String userDirectoryName;

    String userEmailField;

    Map<String, MatchType> userSearchFields;

    Pattern userPasswordPattern;

    String groupDirectoryName;

    String groupMembersField;

    String groupSubGroupsField;

    String groupParentGroupsField;

    VirtualUser anonymousUser;

    final Map<String, VirtualUserDescriptor> virtualUsers;

    public FakeUserManagerImpl() {
        virtualUsers = new HashMap<String, VirtualUserDescriptor>();
    }

    public String getUserListingMode() {
        return userListingMode;
    }

    public void setUserListingMode(String userListingMode) {
        this.userListingMode = userListingMode;
    }

    public String getGroupListingMode() {
        return groupListingMode;
    }

    public void setGroupListingMode(String groupListingMode) {
        this.groupListingMode = groupListingMode;
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public String getUserSortField() {
        return userSortField;
    }

    public void setUserSortField(String sortField) {
        userSortField = sortField;
    }

    public void setGroupSortField(String sortField) {
        groupSortField = sortField;
    }

    public void setUserDirectoryName(String userDirectoryName) {
        this.userDirectoryName = userDirectoryName;
    }

    public String getUserDirectoryName() {
        return userDirectoryName;
    }

    public void setUserEmailField(String userEmailField) {
        this.userEmailField = userEmailField;
    }

    public String getUserEmailField() {
        return userEmailField;
    }

    public void setUserSearchFields(Set<String> userSearchFields) {
        this.userSearchFields = new LinkedHashMap<String, MatchType>();
        for (String searchField : userSearchFields) {
            this.userSearchFields.put(searchField, MatchType.SUBSTRING);
        }
    }

    public void setUserSearchFields(Map<String, MatchType> userSearchFields) {
        this.userSearchFields = userSearchFields;
    }

    public Set<String> getUserSearchFields() {
        return userSearchFields.keySet();
    }

    public void setGroupDirectoryName(String groupDirectoryName) {
        this.groupDirectoryName = groupDirectoryName;
    }

    public String getGroupDirectoryName() {
        return groupDirectoryName;
    }

    public void setGroupMembersField(String groupMembersField) {
        this.groupMembersField = groupMembersField;
    }

    public String getGroupMembersField() {
        return groupMembersField;
    }

    public void setGroupSubGroupsField(String groupSubGroupsField) {
        this.groupSubGroupsField = groupSubGroupsField;
    }

    public String getGroupSubGroupsField() {
        return groupSubGroupsField;
    }

    public void setGroupParentGroupsField(String groupParentGroupsField) {
        this.groupParentGroupsField = groupParentGroupsField;
    }

    public String getGroupParentGroupsField() {
        return groupParentGroupsField;
    }

    public Boolean areGroupsReadOnly() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public Boolean areUsersReadOnly() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public boolean checkUsernamePassword(String username, String password)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public boolean validatePassword(String password) {
        if (userPasswordPattern == null) {
            return true;
        } else {
            Matcher userPasswordMatcher = userPasswordPattern.matcher(password);
            return userPasswordMatcher.find();
        }
    }

    public List<String> getGroupsInGroup(String parentId)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public NuxeoPrincipal getPrincipal(String username) throws ClientException {
        NuxeoPrincipalImpl principal = new NuxeoPrincipalImpl(
                SecurityConstants.ADMINISTRATOR, false, true);
        principal.setGroups(Arrays.asList(new String[] { SecurityConstants.ADMINISTRATORS }));
        principal.setEmail("admin@example.com");
        return principal;
    }

    public List<String> getTopLevelGroups() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public List<String> getUsersInGroup(String groupId) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public List<String> getUsersInGroupAndSubGroups(String groupId)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public List<NuxeoGroup> searchGroups(String pattern) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public List<NuxeoPrincipal> searchPrincipals(String pattern)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public Pattern getUserPasswordPattern() {
        return userPasswordPattern;
    }

    public void setUserPasswordPattern(Pattern userPasswordPattern) {
        this.userPasswordPattern = userPasswordPattern;
    }

    public void setAnonymousUser(VirtualUser anonymousUser) {
        this.anonymousUser = anonymousUser;
    }

    public void setVirtualUsers(Map<String, VirtualUserDescriptor> virtualUsers) {
        this.virtualUsers.clear();
        if (virtualUsers != null) {
            this.virtualUsers.putAll(virtualUsers);
        }
    }

    public String getAnonymousUserId() {
        if (anonymousUser == null) {
            return null;
        }
        return anonymousUser.getId();
    }

    @Override
    public String getDigestAuthDirectory() {
        return null;
    }

    @Override
    public String getDigestAuthRealm() {
        return null;
    }

    public void setConfiguration(UserManagerDescriptor descriptor) {
        setDefaultGroup(descriptor.defaultGroup);
        defaultAdministratorIds = descriptor.defaultAdministratorIds;
        administratorsGroups = descriptor.administratorsGroups;
        setUserSortField(descriptor.userSortField);
        setGroupSortField(descriptor.groupSortField);
        setUserListingMode(descriptor.userListingMode);
        setGroupListingMode(descriptor.groupListingMode);
        setUserDirectoryName(descriptor.userDirectoryName);
        setUserEmailField(descriptor.userEmailField);
        setUserSearchFields(descriptor.userSearchFields);
        setUserPasswordPattern(descriptor.userPasswordPattern);
        setGroupDirectoryName(descriptor.groupDirectoryName);
        setGroupMembersField(descriptor.groupMembersField);
        setGroupSubGroupsField(descriptor.groupSubGroupsField);
        setGroupParentGroupsField(descriptor.groupParentGroupsField);
        setAnonymousUser(descriptor.anonymousUser);
        setVirtualUsers(descriptor.virtualUsers);
    }

    public DocumentModel getBareUserModel() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DocumentModel createGroup(DocumentModel groupModel)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DocumentModel createUser(DocumentModel userModel)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void deleteGroup(DocumentModel groupModel) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void deleteGroup(String groupId) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void deleteUser(DocumentModel userModel) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void deleteUser(String userId) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DocumentModel getBareGroupModel() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public NuxeoGroup getGroup(String groupName) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public List<String> getGroupIds() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public List<String> getUserIds() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DocumentModelList searchGroups(Map<String, Serializable> filter,
            HashSet<String> fulltext) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DocumentModelList searchUsers(Map<String, Serializable> filter,
            Set<String> fulltext) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DocumentModelList searchUsers(String pattern) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void updateGroup(DocumentModel groupModel) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void updateUser(DocumentModel userModel) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DocumentModel getGroupModel(String groupName) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DocumentModel getUserModel(String userName) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public String getGroupIdField() throws ClientException {
        return "groupname";
    }

    public String getGroupSchemaName() throws ClientException {
        return "group";
    }

    public String getUserIdField() throws ClientException {
        return "username";
    }

    public String getUserSchemaName() throws ClientException {
        return "user";
    }

    public void createGroup(NuxeoGroup group) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void createPrincipal(NuxeoPrincipal principal)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void deleteGroup(NuxeoGroup group) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void deletePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public List<NuxeoGroup> getAvailableGroups() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public List<NuxeoPrincipal> getAvailablePrincipals() throws ClientException {
        throw new UnsupportedOperationException();
    }

    public DocumentModel getModelForUser(String name) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public List<NuxeoPrincipal> searchByMap(Map<String, Serializable> filter,
            Set<String> pattern) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void updateGroup(NuxeoGroup group) throws ClientException {
        throw new UnsupportedOperationException();
    }

    public void updatePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    public List<String> getAdministratorsGroups() {
        throw new UnsupportedOperationException();
    }

    public String[] getUsersForPermission(String perm, ACP acp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean authenticate(String name, String password) {
        try {
            return checkUsernamePassword(name, password);
        } catch (ClientException e) {
            throw new ClientRuntimeException("Username / password authentication failed", e);
        }
    }
}
