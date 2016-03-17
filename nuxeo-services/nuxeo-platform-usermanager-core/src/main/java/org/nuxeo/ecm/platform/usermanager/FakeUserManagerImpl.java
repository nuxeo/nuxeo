/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 *
 * $Id: FakeUserManagerImpl.java 28010 2007-12-07 19:23:44Z fguillaume $
 */

package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.services.event.Event;

/**
 * @author Florent Guillaume
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

    Map<String, MatchType> groupSearchFields;

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

    public Set<String> getGroupSearchFields() {
        return groupSearchFields.keySet();
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

    public Boolean areGroupsReadOnly() {
        throw new UnsupportedOperationException();
    }

    public Boolean areUsersReadOnly() {
        throw new UnsupportedOperationException();
    }

    public boolean checkUsernamePassword(String username, String password) {
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

    @Override
    public void updatePassword(String username, String oldPassword, String
            newPassword) {
        throw new UnsupportedOperationException();
    }

    public List<String> getGroupsInGroup(String parentId) {
        throw new UnsupportedOperationException();
    }

    public NuxeoPrincipal getPrincipal(String username) {
        NuxeoPrincipalImpl principal = new NuxeoPrincipalImpl(SecurityConstants.ADMINISTRATOR, false, true);
        principal.setGroups(Arrays.asList(SecurityConstants.ADMINISTRATORS));
        principal.setEmail("admin@example.com");
        return principal;
    }

    public List<String> getTopLevelGroups() {
        throw new UnsupportedOperationException();
    }

    public List<String> getUsersInGroup(String groupId) {
        throw new UnsupportedOperationException();
    }

    public List<String> getUsersInGroupAndSubGroups(String groupId) {
        throw new UnsupportedOperationException();
    }

    public DocumentModelList searchGroups(String pattern) {
        throw new UnsupportedOperationException();
    }

    public List<NuxeoPrincipal> searchPrincipals(String pattern) {
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

    public DocumentModel getBareUserModel() {
        throw new UnsupportedOperationException();
    }

    public DocumentModel createGroup(DocumentModel groupModel) {
        throw new UnsupportedOperationException();
    }

    public DocumentModel createUser(DocumentModel userModel) {
        throw new UnsupportedOperationException();
    }

    public void deleteGroup(DocumentModel groupModel) {
        throw new UnsupportedOperationException();
    }

    public void deleteGroup(String groupId) {
        throw new UnsupportedOperationException();
    }

    public void deleteUser(DocumentModel userModel) {
        throw new UnsupportedOperationException();
    }

    public void deleteUser(String userId) {
        throw new UnsupportedOperationException();
    }

    public DocumentModel getBareGroupModel() {
        throw new UnsupportedOperationException();
    }

    public NuxeoGroup getGroup(String groupName) {
        throw new UnsupportedOperationException();
    }

    public List<String> getGroupIds() {
        throw new UnsupportedOperationException();
    }

    public List<String> getUserIds() {
        throw new UnsupportedOperationException();
    }

    public DocumentModelList searchGroups(Map<String, Serializable> filter, Set<String> fulltext)
            {
        throw new UnsupportedOperationException();
    }

    public DocumentModelList searchUsers(Map<String, Serializable> filter, Set<String> fulltext) {
        throw new UnsupportedOperationException();
    }

    public DocumentModelList searchUsers(String pattern) {
        throw new UnsupportedOperationException();
    }

    public void updateGroup(DocumentModel groupModel) {
        throw new UnsupportedOperationException();
    }

    public void updateUser(DocumentModel userModel) {
        throw new UnsupportedOperationException();
    }

    public DocumentModel getGroupModel(String groupName) {
        throw new UnsupportedOperationException();
    }

    public DocumentModel getUserModel(String userName) {
        throw new UnsupportedOperationException();
    }

    public String getGroupIdField() {
        return "groupname";
    }

    public String getGroupLabelField() {
        return "grouplabel";
    }

    public String getGroupSchemaName() {
        return "group";
    }

    public String getUserIdField() {
        return "username";
    }

    public String getUserSchemaName() {
        return "user";
    }

    public void createGroup(NuxeoGroup group) {
        throw new UnsupportedOperationException();
    }

    public void createPrincipal(NuxeoPrincipal principal) {
        throw new UnsupportedOperationException();
    }

    public void deleteGroup(NuxeoGroup group) {
        throw new UnsupportedOperationException();
    }

    public void deletePrincipal(NuxeoPrincipal principal) {
        throw new UnsupportedOperationException();
    }

    public List<NuxeoGroup> getAvailableGroups() {
        throw new UnsupportedOperationException();
    }

    public List<NuxeoPrincipal> getAvailablePrincipals() {
        throw new UnsupportedOperationException();
    }

    public DocumentModel getModelForUser(String name) {
        throw new UnsupportedOperationException();
    }

    public List<NuxeoPrincipal> searchByMap(Map<String, Serializable> filter, Set<String> pattern)
            {
        throw new UnsupportedOperationException();
    }

    public void updateGroup(NuxeoGroup group) {
        throw new UnsupportedOperationException();
    }

    public void updatePrincipal(NuxeoPrincipal principal) {
        throw new UnsupportedOperationException();
    }

    public List<String> getAdministratorsGroups() {
        throw new UnsupportedOperationException();
    }

    public String[] getUsersForPermission(String perm, ACP acp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Principal authenticate(String name, String password) {
        return checkUsernamePassword(name, password) ? getPrincipal(name) : null;
    }

    @Override
    public boolean aboutToHandleEvent(Event event) {
        return false;
    }

    @Override
    public void handleEvent(Event event) {
    }
}
