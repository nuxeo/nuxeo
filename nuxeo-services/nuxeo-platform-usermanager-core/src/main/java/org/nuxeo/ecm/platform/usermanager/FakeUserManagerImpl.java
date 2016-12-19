/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
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
        virtualUsers = new HashMap<>();
    }

    @Override
    public String getUserListingMode() {
        return userListingMode;
    }

    public void setUserListingMode(String userListingMode) {
        this.userListingMode = userListingMode;
    }

    @Override
    public String getGroupListingMode() {
        return groupListingMode;
    }

    public void setGroupListingMode(String groupListingMode) {
        this.groupListingMode = groupListingMode;
    }

    @Override
    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    @Override
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

    @Override
    public String getUserDirectoryName() {
        return userDirectoryName;
    }

    public void setUserEmailField(String userEmailField) {
        this.userEmailField = userEmailField;
    }

    @Override
    public String getUserEmailField() {
        return userEmailField;
    }

    public void setUserSearchFields(Set<String> userSearchFields) {
        this.userSearchFields = new LinkedHashMap<>();
        for (String searchField : userSearchFields) {
            this.userSearchFields.put(searchField, MatchType.SUBSTRING);
        }
    }

    public void setUserSearchFields(Map<String, MatchType> userSearchFields) {
        this.userSearchFields = userSearchFields;
    }

    @Override
    public Set<String> getUserSearchFields() {
        return userSearchFields.keySet();
    }

    @Override
    public Set<String> getGroupSearchFields() {
        return groupSearchFields.keySet();
    }

    public void setGroupDirectoryName(String groupDirectoryName) {
        this.groupDirectoryName = groupDirectoryName;
    }

    @Override
    public String getGroupDirectoryName() {
        return groupDirectoryName;
    }

    public void setGroupMembersField(String groupMembersField) {
        this.groupMembersField = groupMembersField;
    }

    @Override
    public String getGroupMembersField() {
        return groupMembersField;
    }

    public void setGroupSubGroupsField(String groupSubGroupsField) {
        this.groupSubGroupsField = groupSubGroupsField;
    }

    @Override
    public String getGroupSubGroupsField() {
        return groupSubGroupsField;
    }

    public void setGroupParentGroupsField(String groupParentGroupsField) {
        this.groupParentGroupsField = groupParentGroupsField;
    }

    @Override
    public String getGroupParentGroupsField() {
        return groupParentGroupsField;
    }

    @Override
    public Boolean areGroupsReadOnly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean areUsersReadOnly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkUsernamePassword(String username, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean validatePassword(String password) {
        if (userPasswordPattern == null) {
            return true;
        } else {
            Matcher userPasswordMatcher = userPasswordPattern.matcher(password);
            return userPasswordMatcher.find();
        }
    }

    @Override
    public List<String> getGroupsInGroup(String parentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NuxeoPrincipal getPrincipal(String username) {
        NuxeoPrincipalImpl principal = new NuxeoPrincipalImpl(SecurityConstants.ADMINISTRATOR, false, true);
        principal.setGroups(Collections.singletonList(SecurityConstants.ADMINISTRATORS));
        principal.setEmail("admin@example.com");
        return principal;
    }

    @Override
    public List<String> getTopLevelGroups() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getUsersInGroup(String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getUsersInGroupAndSubGroups(String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModelList searchGroups(String pattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NuxeoPrincipal> searchPrincipals(String pattern) {
        throw new UnsupportedOperationException();
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public DocumentModel getBareUserModel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createGroup(DocumentModel groupModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel createUser(DocumentModel userModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteGroup(DocumentModel groupModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteGroup(String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteUser(DocumentModel userModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteUser(String userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel getBareGroupModel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NuxeoGroup getGroup(String groupName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getGroupIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getUserIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModelList searchGroups(Map<String, Serializable> filter, Set<String> fulltext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModelList searchUsers(Map<String, Serializable> filter, Set<String> fulltext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModelList searchUsers(String pattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateGroup(DocumentModel groupModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateUser(DocumentModel userModel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel getGroupModel(String groupName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModel getUserModel(String userName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getGroupIdField() {
        return "groupname";
    }

    @Override
    public String getGroupLabelField() {
        return "grouplabel";
    }

    @Override
    public String getGroupSchemaName() {
        return "group";
    }

    @Override
    public String getUserIdField() {
        return "username";
    }

    @Override
    public String getUserSchemaName() {
        return "user";
    }

    @Override
    public List<String> getAdministratorsGroups() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getUsersForPermission(String perm, ACP acp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Principal authenticate(String name, String password) {
        return checkUsernamePassword(name, password) ? getPrincipal(name) : null;
    }

    @Override
    public void handleEvent(Event event) {
    }

}
