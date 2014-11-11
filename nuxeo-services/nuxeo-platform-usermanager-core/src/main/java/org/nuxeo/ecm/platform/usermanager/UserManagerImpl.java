/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     George Lefter
 *     Florent Guillaume
 *     Anahide Tchertchian
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Standard implementation of the Nuxeo UserManager.
 */
public class UserManagerImpl implements UserManager {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(UserManagerImpl.class);

    public static final String USERMANAGER_TOPIC = "usermanager";

    /** Used by JaasCacheFlusher. */
    public static final String USERCHANGED_EVENT_ID = "user_changed";

    public static final String USERCREATED_EVENT_ID = "user_created";

    public static final String USERDELETED_EVENT_ID = "user_deleted";

    public static final String USERMODIFIED_EVENT_ID = "user_modified";

    /** Used by JaasCacheFlusher. */
    public static final String GROUPCHANGED_EVENT_ID = "group_changed";

    public static final String GROUPCREATED_EVENT_ID = "group_created";

    public static final String GROUPDELETED_EVENT_ID = "group_deleted";

    public static final String GROUPMODIFIED_EVENT_ID = "group_modified";

    public static final String DEFAULT_ANONYMOUS_USER_ID = "Anonymous";

    public static final String VIRTUAL_FIELD_FILTER_PREFIX = "__";

    protected final DirectoryService dirService;

    /**
     * A structure used to inject field name configuration of users schema
     * into a NuxeoPrincipalImpl instance.
     * TODO not all fields inside are configurable for now -
     * they will use default values
     */
    protected UserConfig userConfig;

    protected String userDirectoryName;

    protected String userSchemaName;

    protected String userIdField;

    protected String userEmailField;

    protected Map<String, MatchType> userSearchFields;

    protected String groupDirectoryName;

    protected String groupSchemaName;

    protected String groupIdField;

    protected String groupMembersField;

    protected String groupSubGroupsField;

    protected String groupParentGroupsField;

    protected String groupSortField;

    protected String defaultGroup;

    protected List<String> administratorIds;

    protected List<String> administratorGroups;

    protected Boolean disableDefaultAdministratorsGroup;

    protected String userSortField;

    protected String userListingMode;

    protected String groupListingMode;

    protected Pattern userPasswordPattern;

    protected VirtualUser anonymousUser;

    protected String digestAuthDirectory;

    protected String digestAuthRealm;

    protected final Map<String, VirtualUserDescriptor> virtualUsers;

    public UserManagerImpl() {
        dirService = Framework.getLocalService(DirectoryService.class);
        virtualUsers = new HashMap<String, VirtualUserDescriptor>();
        userConfig = new UserConfig();
    }

    public void setConfiguration(UserManagerDescriptor descriptor) {
        defaultGroup = descriptor.defaultGroup;
        administratorIds = descriptor.defaultAdministratorIds;
        disableDefaultAdministratorsGroup = false;
        if (descriptor.disableDefaultAdministratorsGroup != null) {
            disableDefaultAdministratorsGroup = descriptor.disableDefaultAdministratorsGroup;
        }
        administratorGroups = new ArrayList<String>();
        if (!disableDefaultAdministratorsGroup) {
            administratorGroups.add(SecurityConstants.ADMINISTRATORS);
        }
        if (descriptor.administratorsGroups != null) {
            administratorGroups.addAll(descriptor.administratorsGroups);
        }
        if (administratorGroups.isEmpty()) {
            log.warn("No administrators group has been defined: at least one should be set"
                    + " to avoid lockups when blocking rights for instance");
        }
        userSortField = descriptor.userSortField;
        groupSortField = descriptor.groupSortField;
        userListingMode = descriptor.userListingMode;
        groupListingMode = descriptor.groupListingMode;
        userEmailField = descriptor.userEmailField;
        userSearchFields = descriptor.userSearchFields;
        userPasswordPattern = descriptor.userPasswordPattern;
        groupMembersField = descriptor.groupMembersField;
        groupSubGroupsField = descriptor.groupSubGroupsField;
        groupParentGroupsField = descriptor.groupParentGroupsField;
        anonymousUser = descriptor.anonymousUser;

        setUserDirectoryName(descriptor.userDirectoryName);
        setGroupDirectoryName(descriptor.groupDirectoryName);
        setVirtualUsers(descriptor.virtualUsers);

        digestAuthDirectory = descriptor.digestAuthDirectory;
        digestAuthRealm = descriptor.digestAuthRealm;

        userConfig = new UserConfig();
        userConfig.emailKey = userEmailField;
        userConfig.schemaName = userSchemaName;
        userConfig.nameKey = userIdField;
    }

    protected void setUserDirectoryName(String userDirectoryName) {
        this.userDirectoryName = userDirectoryName;
        try {
            userSchemaName = dirService.getDirectorySchema(userDirectoryName);
            userIdField = dirService.getDirectoryIdField(userDirectoryName);
        } catch (ClientException e) {
            throw new RuntimeException("Unkown user directory "
                    + userDirectoryName, e);
        }
    }

    public String getUserDirectoryName() {
        return userDirectoryName;
    }

    public String getUserIdField() throws ClientException {
        return userIdField;
    }

    public String getUserSchemaName() throws ClientException {
        return userSchemaName;
    }

    public String getUserEmailField() {
        return userEmailField;
    }

    public Set<String> getUserSearchFields() {
        return Collections.unmodifiableSet(userSearchFields.keySet());
    }

    protected void setGroupDirectoryName(String groupDirectoryName) {
        this.groupDirectoryName = groupDirectoryName;
        try {
            groupSchemaName = dirService.getDirectorySchema(groupDirectoryName);
            groupIdField = dirService.getDirectoryIdField(groupDirectoryName);
        } catch (ClientException e) {
            throw new RuntimeException("Unkown group directory "
                    + groupDirectoryName, e);
        }
    }

    public String getGroupDirectoryName() {
        return groupDirectoryName;
    }

    public String getGroupIdField() throws ClientException {
        return groupIdField;
    }

    public String getGroupSchemaName() throws ClientException {
        return groupSchemaName;
    }

    public String getGroupMembersField() {
        return groupMembersField;
    }

    public String getGroupSubGroupsField() {
        return groupSubGroupsField;
    }

    public String getGroupParentGroupsField() {
        return groupParentGroupsField;
    }

    public String getUserListingMode() {
        return userListingMode;
    }

    public String getGroupListingMode() {
        return groupListingMode;
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public Pattern getUserPasswordPattern() {
        return userPasswordPattern;
    }

    public String getAnonymousUserId() {
        if (anonymousUser == null) {
            return null;
        }
        String anonymousUserId = anonymousUser.getId();
        if (anonymousUserId == null) {
            return DEFAULT_ANONYMOUS_USER_ID;
        }
        return anonymousUserId;
    }

    protected void setVirtualUsers(
            Map<String, VirtualUserDescriptor> virtualUsers) {
        this.virtualUsers.clear();
        if (virtualUsers != null) {
            this.virtualUsers.putAll(virtualUsers);
        }
    }

    public boolean checkUsernamePassword(String username, String password)
            throws ClientException {

        if (username == null || password == null) {
            log.warn("Trying to authenticate against null username or password");
            return false;
        }

        // deal with anonymous user
        String anonymousUserId = getAnonymousUserId();
        if (username.equals(anonymousUserId)) {
            log.warn(String.format(
                    "Trying to authenticate anonymous user (%s)",
                    anonymousUserId));
            return false;
        }

        // deal with virtual users
        if (virtualUsers.containsKey(username)) {
            VirtualUser user = virtualUsers.get(username);
            String expected = user.getPassword();
            if (expected == null) {
                return false;
            }
            return expected.equals(password);
        }

        Session userDir = null;
        try {
            String userDirName;
            // BBB backward compat for userDirectory + userAuthentication
            if ("userDirectory".equals(userDirectoryName)
                    && dirService.getDirectory("userAuthentication") != null) {
                userDirName = "userAuthentication";
            } else {
                userDirName = userDirectoryName;
            }

            userDir = dirService.open(userDirName);
            if (!userDir.isAuthenticating()) {
                log.error("Trying to authenticate against a non authenticating "
                        + "directory: " + userDirName);
                return false;
            }

            boolean authenticated = userDir.authenticate(username, password);
            if (authenticated) {
                syncDigestAuthPassword(username, password);
            }
            return authenticated;
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    protected void syncDigestAuthPassword(String username, String password)
            throws ClientException {
        if (StringUtils.isEmpty(digestAuthDirectory)
                || StringUtils.isEmpty(digestAuthRealm) || username == null
                || password == null) {
            return;
        }

        String ha1 = encodeDigestAuthPassword(username, digestAuthRealm,
                password);
        Session dir = dirService.open(digestAuthDirectory);
        try {
            String schema = dirService.getDirectorySchema(digestAuthDirectory);
            DocumentModel entry = dir.getEntry(username, true);
            if (entry == null) {
                entry = getDigestAuthModel();
                entry.setProperty(schema, dir.getIdField(), username);
                entry.setProperty(schema, dir.getPasswordField(), ha1);
                dir.createEntry(entry);
                dir.commit();
                log.debug("Created digest auth password for user:" + username);
            } else {
                String storedHa1 = (String) entry.getProperty(schema,
                        dir.getPasswordField());
                if (!ha1.equals(storedHa1)) {
                    entry.setProperty(schema, dir.getPasswordField(), ha1);
                    dir.updateEntry(entry);
                    dir.commit();
                    log.debug("Updated digest auth password for user:"
                            + username);
                }
            }
        } catch (DirectoryException e) {
            log.error("Digest auth password not synchronized", e);
        } finally {
            dir.close();
        }
    }

    protected DocumentModel getDigestAuthModel() throws ClientException {
        String schema = dirService.getDirectorySchema(digestAuthDirectory);
        return BaseSession.createEntryModel(null, schema, null, null);
    }

    public static String encodeDigestAuthPassword(String username,
            String realm, String password) {
        String a1 = username + ":" + realm + ":" + password;
        return DigestUtils.md5Hex(a1);
    }

    @Override
    public String getDigestAuthDirectory() {
        return digestAuthDirectory;
    }

    @Override
    public String getDigestAuthRealm() {
        return digestAuthRealm;
    }

    public boolean validatePassword(String password) {
        if (userPasswordPattern == null) {
            return true;
        } else {
            Matcher userPasswordMatcher = userPasswordPattern.matcher(password);
            return userPasswordMatcher.find();
        }
    }

    protected NuxeoPrincipal makeAnonymousPrincipal() throws ClientException {
        DocumentModel userEntry = makeVirtualUserEntry(getAnonymousUserId(),
                anonymousUser);
        // XXX: pass anonymous user groups, but they will be ignored
        return makePrincipal(userEntry, true, anonymousUser.getGroups());
    }

    protected NuxeoPrincipal makeVirtualPrincipal(VirtualUser user)
            throws ClientException {
        DocumentModel userEntry = makeVirtualUserEntry(user.getId(), user);
        return makePrincipal(userEntry, false, user.getGroups());
    }

    protected DocumentModel makeVirtualUserEntry(String id, VirtualUser user)
            throws ClientException {
        final DocumentModel userEntry = BaseSession.createEntryModel(null,
                userSchemaName, id, null);
        // at least fill id field
        userEntry.setProperty(userSchemaName, userIdField, id);
        for (Entry<String, Serializable> prop : user.getProperties().entrySet()) {
            try {
                userEntry.setProperty(userSchemaName, prop.getKey(),
                        prop.getValue());
            } catch (ClientException ce) {
                log.error("Property: " + prop.getKey()
                        + " does not exists. Check your "
                        + "UserService configuration.", ce);
            }
        }
        return userEntry;
    }

    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry)
            throws ClientException {
        return makePrincipal(userEntry, false, null);
    }

    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry,
            boolean anonymous, List<String> groups) throws ClientException {
        boolean admin = false;
        String username = userEntry.getId();

        // XXX why not set groups to anonymous user?
        List<String> virtualGroups = new LinkedList<String>();
        if (!anonymous) {
            // Add preconfigured groups: useful for LDAP
            if (defaultGroup != null) {
                virtualGroups.add(defaultGroup);
            }
            // Add additional groups: useful for virtual users
            if (groups != null) {
                virtualGroups.addAll(groups);
            }
            // Create a default admin if needed
            if (administratorIds != null && administratorIds.contains(username)) {
                admin = true;
                if (administratorGroups != null) {
                    virtualGroups.addAll(administratorGroups);
                }
            }
        }

        NuxeoPrincipalImpl principal = new NuxeoPrincipalImpl(username,
                anonymous, admin, false);
        principal.setConfig(userConfig);

        principal.setModel(userEntry, false);
        principal.setVirtualGroups(virtualGroups, true);

        // TODO: reenable roles initialization once we have a use case for
        // a role directory. In the mean time we only set the JBOSS role
        // that is required to login
        List<String> roles = Arrays.asList("regular");
        principal.setRoles(roles);

        return principal;
    }

    public NuxeoPrincipal getPrincipal(String username) throws ClientException {
        if (username == null) {
            return null;
        }
        String anonymousUserId = getAnonymousUserId();
        if (username.equals(anonymousUserId)) {
            return makeAnonymousPrincipal();
        }
        if (virtualUsers.containsKey(username)) {
            return makeVirtualPrincipal(virtualUsers.get(username));
        }
        DocumentModel userModel = getUserModel(username);
        if (userModel != null) {
            return makePrincipal(userModel);
        }
        return null;
    }

    public DocumentModel getUserModel(String userName) throws ClientException {
        if (userName == null) {
            return null;
        }
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            return userDir.getEntry(userName);
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public DocumentModel getBareUserModel() throws ClientException {
        String schema = dirService.getDirectorySchema(userDirectoryName);
        return BaseSession.createEntryModel(null, schema, null, null);
    }

    public NuxeoGroup getGroup(String groupName) throws ClientException {
        DocumentModel groupEntry = getGroupModel(groupName);
        if (groupEntry != null) {
            return makeGroup(groupEntry);
        }
        return null;
    }

    public DocumentModel getGroupModel(String groupName) throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            return groupDir.getEntry(groupName);
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected NuxeoGroup makeGroup(DocumentModel groupEntry) {
        NuxeoGroup group = new NuxeoGroupImpl(groupEntry.getId());
        List<String> list;
        try {
            list = (List<String>) groupEntry.getProperty(groupSchemaName,
                    groupMembersField);
        } catch (ClientException e) {
            list = null;
        }
        if (list != null) {
            group.setMemberUsers(list);
        }
        try {
            list = (List<String>) groupEntry.getProperty(groupSchemaName,
                    groupSubGroupsField);
        } catch (ClientException e) {
            list = null;
        }
        if (list != null) {
            group.setMemberGroups(list);
        }
        try {
            list = (List<String>) groupEntry.getProperty(groupSchemaName,
                    groupParentGroupsField);
        } catch (ClientException e) {
            list = null;
        }
        if (list != null) {
            group.setParentGroups(list);
        }
        return group;
    }

    @SuppressWarnings("unchecked")
    public List<String> getTopLevelGroups() throws ClientException {
        Session groupDir = null;
        try {
            List<String> topLevelGroups = new LinkedList<String>();
            groupDir = dirService.open(groupDirectoryName);
            // XXX retrieve all entries with references, can be costly.
            DocumentModelList groups = groupDir.query(
                    Collections.<String, Serializable> emptyMap(), null, null,
                    true);
            for (DocumentModel group : groups) {
                List<String> parents = (List<String>) group.getProperty(
                        groupSchemaName, groupParentGroupsField);

                if (parents == null || parents.isEmpty()) {
                    topLevelGroups.add(group.getId());
                }
            }
            return topLevelGroups;
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public List<String> getGroupsInGroup(String parentId)
            throws ClientException {
        return getGroup(parentId).getMemberGroups();
    }

    public List<String> getUsersInGroup(String groupId) throws ClientException {
        return getGroup(groupId).getMemberUsers();
    }

    public List<String> getUsersInGroupAndSubGroups(String groupId)
            throws ClientException {
        Set<String> groups = new HashSet<String>();
        groups.add(groupId);
        appendSubgroups(groupId, groups);

        Set<String> users = new HashSet<String>();
        for (String groupid : groups) {
            users.addAll(getGroup(groupid).getMemberUsers());
        }

        return new ArrayList<String>(users);
    }

    protected void appendSubgroups(String groupId, Set<String> groups)
            throws ClientException {
        List<String> groupsToAppend = getGroupsInGroup(groupId);
        groups.addAll(groupsToAppend);
        for (String subgroupId : groupsToAppend) {
            groups.add(subgroupId);
            // avoiding infinite loop
            if (!groups.contains(subgroupId)) {
                appendSubgroups(subgroupId, groups);
            }
        }

    }

    protected boolean isAnonymousMatching(Map<String, Serializable> filter,
            Set<String> fulltext) {
        String anonymousUserId = getAnonymousUserId();
        if (anonymousUserId == null) {
            return false;
        }
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        Map<String, Serializable> anonymousUserMap = anonymousUser.getProperties();
        anonymousUserMap.put(userIdField, anonymousUserId);
        for (Entry<String, Serializable> e : filter.entrySet()) {
            String fieldName = e.getKey();
            Object expected = e.getValue();
            Object value = anonymousUserMap.get(fieldName);
            if (value == null) {
                if (expected != null) {
                    return false;
                }
            } else {
                if (fulltext != null && fulltext.contains(fieldName)) {
                    if (!value.toString().toLowerCase().startsWith(
                            expected.toString().toLowerCase())) {
                        return false;
                    }
                } else {
                    if (!value.equals(expected)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public List<NuxeoPrincipal> searchPrincipals(String pattern)
            throws ClientException {
        DocumentModelList entries = searchUsers(pattern);
        List<NuxeoPrincipal> principals = new ArrayList<NuxeoPrincipal>(
                entries.size());
        for (DocumentModel entry : entries) {
            principals.add(makePrincipal(entry));
        }
        return principals;
    }

    public List<NuxeoGroup> searchGroups(String pattern) throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            if (pattern != null && pattern != "") {
                filter.put(groupDir.getIdField(), pattern);
            }
            DocumentModelList groupEntries = searchGroups(filter,
                    new HashSet<String>(filter.keySet()));

            List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>(
                    groupEntries.size());
            for (DocumentModel groupEntry : groupEntries) {
                groups.add(makeGroup(groupEntry));
            }
            return groups;

        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public String getUserSortField() {
        return userSortField;
    }

    protected Map<String, String> getUserSortMap() {
        String sortField = userSortField != null ? userSortField : userIdField;
        Map<String, String> orderBy = new HashMap<String, String>();
        orderBy.put(sortField, DocumentModelComparator.ORDER_ASC);
        return orderBy;
    }

    protected void notify(String userOrGroupName, String eventId)
            throws ClientException {
        try {
            EventService eventService = Framework.getService(EventService.class);
            eventService.sendEvent(new Event(USERMANAGER_TOPIC, eventId, this,
                    userOrGroupName));
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    /**
     * Notifies user has changed so that the JaasCacheFlusher listener can make
     * sure principals cache is reset.
     */
    protected void notifyUserChanged(String userName) throws ClientException {
        notify(userName, USERCHANGED_EVENT_ID);
    }

    /**
     * Notifies group has changed so that the JaasCacheFlusher listener can make
     * sure principals cache is reset.
     */
    protected void notifyGroupChanged(String groupName) throws ClientException {
        notify(groupName, GROUPCHANGED_EVENT_ID);
    }

    public Boolean areGroupsReadOnly() throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            return groupDir.isReadOnly();
        } catch (DirectoryException e) {
            log.error(e);
            return false;
        } finally {
            try {
                if (groupDir != null) {
                    groupDir.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public Boolean areUsersReadOnly() throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            return userDir.isReadOnly();
        } catch (DirectoryException e) {
            log.error(e);
            return false;
        } finally {
            try {
                if (userDir != null) {
                    userDir.close();
                }
            } catch (Exception e) {
            }
        }
    }

    protected String getGroupId(DocumentModel groupModel)
            throws ClientException {
        Object groupIdValue = groupModel.getProperty(groupSchemaName,
                groupIdField);
        if (groupIdValue != null && !(groupIdValue instanceof String)) {
            throw new ClientException("Invalid group id " + groupIdValue);
        }
        return (String) groupIdValue;
    }

    protected String getUserId(DocumentModel userModel) throws ClientException {
        Object userIdValue = userModel.getProperty(userSchemaName, userIdField);
        if (userIdValue != null && !(userIdValue instanceof String)) {
            throw new ClientException("Invalid user id " + userIdValue);
        }
        return (String) userIdValue;
    }

    public DocumentModel createGroup(DocumentModel groupModel)
            throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            String groupId = getGroupId(groupModel);

            // check the group does not exist
            if (groupDir.hasEntry(groupId)) {
                throw new GroupAlreadyExistsException();
            }
            groupModel = groupDir.createEntry(groupModel);
            groupDir.commit();
            notifyGroupChanged(groupId);
            notify(groupId, GROUPCREATED_EVENT_ID);
            return groupModel;

        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public DocumentModel createUser(DocumentModel userModel)
            throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            String userId = getUserId(userModel);

            // check the user does not exist
            if (userDir.hasEntry(userId)) {
                throw new UserAlreadyExistsException();
            }

            String schema = dirService.getDirectorySchema(userDirectoryName);
            String clearUsername = (String) userModel.getProperty(schema,
                    userDir.getIdField());
            String clearPassword = (String) userModel.getProperty(schema,
                    userDir.getPasswordField());

            userModel = userDir.createEntry(userModel);
            userDir.commit();

            syncDigestAuthPassword(clearUsername, clearPassword);

            notifyUserChanged(userId);
            notify(userId, USERCREATED_EVENT_ID);
            return userModel;

        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public void deleteGroup(String groupId) throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            if (!groupDir.hasEntry(groupId)) {
                throw new DirectoryException("Group does not exist: " + groupId);
            }
            groupDir.deleteEntry(groupId);
            groupDir.commit();
            notifyGroupChanged(groupId);
            notify(groupId, GROUPDELETED_EVENT_ID);
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public void deleteGroup(DocumentModel groupModel) throws ClientException {
        String groupId = getGroupId(groupModel);
        deleteGroup(groupId);
    }

    public void deleteUser(String userId) throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            if (!userDir.hasEntry(userId)) {
                throw new DirectoryException("User does not exist: " + userId);
            }
            userDir.deleteEntry(userId);
            userDir.commit();
            notifyUserChanged(userId);
            notify(userId, USERDELETED_EVENT_ID);

        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public void deleteUser(DocumentModel userModel) throws ClientException {
        String userId = getUserId(userModel);
        deleteUser(userId);
    }

    public List<String> getGroupIds() throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            List<String> groupIds = groupDir.getProjection(
                    Collections.<String, Serializable> emptyMap(),
                    groupDir.getIdField());
            Collections.sort(groupIds);
            return groupIds;
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public List<String> getUserIds() throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            List<String> userIds = userDir.getProjection(
                    Collections.<String, Serializable> emptyMap(),
                    userDir.getIdField());
            Collections.sort(userIds);
            return userIds;
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    protected void removeVirtualFilters(Map<String, Serializable> filter) {
        if (filter == null) {
            return;
        }
        List<String> keys = new ArrayList<String>(filter.keySet());
        for (String key : keys) {
            if (key.startsWith(VIRTUAL_FIELD_FILTER_PREFIX)) {
                filter.remove(key);
            }
        }
    }

    public DocumentModelList searchGroups(Map<String, Serializable> filter,
            HashSet<String> fulltext) throws ClientException {
        Session groupDir = null;
        try {

            removeVirtualFilters(filter);
            groupDir = dirService.open(groupDirectoryName);

            String sortField = groupSortField != null ? groupSortField
                    : groupDir.getIdField();
            Map<String, String> orderBy = new HashMap<String, String>();
            orderBy.put(sortField, DocumentModelComparator.ORDER_ASC);
            // XXX: do not fetch references, can be costly
            DocumentModelList entries = groupDir.query(filter, fulltext,
                    orderBy, false);

            return entries;
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    protected DocumentModelList searchUsers(Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy)
            throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);

            removeVirtualFilters(filter);

            // XXX: do not fetch references, can be costly
            DocumentModelList entries = userDir.query(filter, fulltext, null,
                    false);
            if (isAnonymousMatching(filter, fulltext)) {
                entries.add(makeVirtualUserEntry(getAnonymousUserId(),
                        anonymousUser));
            }

            // TODO: match searchable virtual users

            if (orderBy != null && !orderBy.isEmpty()) {
                // sort: cannot sort before virtual users are added
                Collections.sort(entries, new DocumentModelComparator(
                        userSchemaName, orderBy));
            }

            return entries;
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public DocumentModelList searchUsers(String pattern) throws ClientException {
        DocumentModelList entries = new DocumentModelListImpl();
        if (pattern == null || pattern.length() == 0) {
            entries = searchUsers(
                    Collections.<String, Serializable> emptyMap(), null);
        } else {
            Map<String, DocumentModel> uniqueEntries = new HashMap<String, DocumentModel>();

            for (Entry<String, MatchType> fieldEntry : userSearchFields.entrySet()) {
                Map<String, Serializable> filter = new HashMap<String, Serializable>();
                filter.put(fieldEntry.getKey(), pattern);
                DocumentModelList fetchedEntries;
                if (fieldEntry.getValue() == MatchType.SUBSTRING) {
                    fetchedEntries = searchUsers(filter, filter.keySet(), null);
                } else {
                    fetchedEntries = searchUsers(filter, null, null);
                }
                for (DocumentModel entry : fetchedEntries) {
                    uniqueEntries.put(entry.getId(), entry);
                }
            }
            log.debug(String.format("found %d unique entries",
                    uniqueEntries.size()));
            entries.addAll(uniqueEntries.values());
        }
        // sort
        Collections.sort(entries, new DocumentModelComparator(userSchemaName,
                getUserSortMap()));

        return entries;
    }

    public DocumentModelList searchUsers(Map<String, Serializable> filter,
            Set<String> fulltext) throws ClientException {
        return searchUsers(filter, fulltext, getUserSortMap());
    }

    public void updateGroup(DocumentModel groupModel) throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            String groupId = getGroupId(groupModel);

            if (!groupDir.hasEntry(groupId)) {
                throw new DirectoryException("group does not exist: " + groupId);
            }
            groupDir.updateEntry(groupModel);
            groupDir.commit();
            notifyGroupChanged(groupId);
            notify(groupId, GROUPMODIFIED_EVENT_ID);
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public void updateUser(DocumentModel userModel) throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            String userId = getUserId(userModel);

            if (!userDir.hasEntry(userId)) {
                throw new DirectoryException("user does not exist: " + userId);
            }

            String schema = dirService.getDirectorySchema(userDirectoryName);
            String clearUsername = (String) userModel.getProperty(schema,
                    userDir.getIdField());
            String clearPassword = (String) userModel.getProperty(schema,
                    userDir.getPasswordField());

            userDir.updateEntry(userModel);
            userDir.commit();

            syncDigestAuthPassword(clearUsername, clearPassword);

            notifyUserChanged(userId);
            notify(userId, USERMODIFIED_EVENT_ID);
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public DocumentModel getBareGroupModel() throws ClientException {
        String schema = dirService.getDirectorySchema(groupDirectoryName);
        return BaseSession.createEntryModel(null, schema, null, null);
    }

    public void createGroup(NuxeoGroup group) throws ClientException {
        DocumentModel newGroupModel = getBareGroupModel();
        newGroupModel.setProperty(groupSchemaName, groupIdField,
                group.getName());
        newGroupModel.setProperty(groupSchemaName, groupMembersField,
                group.getMemberUsers());
        newGroupModel.setProperty(groupSchemaName, groupSubGroupsField,
                group.getMemberGroups());
        createGroup(newGroupModel);
    }

    public void createPrincipal(NuxeoPrincipal principal)
            throws ClientException {
        createUser(principal.getModel());
    }

    public void deleteGroup(NuxeoGroup group) throws ClientException {
        deleteGroup(group.getName());
    }

    public void deletePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        deleteUser(principal.getName());
    }

    public List<NuxeoGroup> getAvailableGroups() throws ClientException {
        DocumentModelList groupModels = searchGroups(
                Collections.<String, Serializable> emptyMap(), null);
        List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>(groupModels.size());
        for (DocumentModel groupModel : groupModels) {
            groups.add(makeGroup(groupModel));
        }
        return groups;
    }

    public List<NuxeoPrincipal> getAvailablePrincipals() throws ClientException {
        DocumentModelList userModels = searchUsers(
                Collections.<String, Serializable> emptyMap(), null);
        List<NuxeoPrincipal> users = new ArrayList<NuxeoPrincipal>(
                userModels.size());
        for (DocumentModel userModel : userModels) {
            users.add(makePrincipal(userModel));
        }
        return users;
    }

    public DocumentModel getModelForUser(String name) throws ClientException {
        return getUserModel(name);
    }

    public List<NuxeoPrincipal> searchByMap(Map<String, Serializable> filter,
            Set<String> pattern) throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            removeVirtualFilters(filter);

            DocumentModelList entries = userDir.query(filter, pattern);
            List<NuxeoPrincipal> principals = new ArrayList<NuxeoPrincipal>(
                    entries.size());
            for (DocumentModel entry : entries) {
                principals.add(makePrincipal(entry));
            }
            if (isAnonymousMatching(filter, pattern)) {
                principals.add(makeAnonymousPrincipal());
            }
            return principals;
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public void updateGroup(NuxeoGroup group) throws ClientException {
        // XXX: need to refetch it for tests to pass, i don't get why (session
        // id is used maybe?)
        DocumentModel newGroupModel = getGroupModel(group.getName());
        newGroupModel.setProperty(groupSchemaName, groupIdField,
                group.getName());
        newGroupModel.setProperty(groupSchemaName, groupMembersField,
                group.getMemberUsers());
        newGroupModel.setProperty(groupSchemaName, groupSubGroupsField,
                group.getMemberGroups());
        updateGroup(newGroupModel);
    }

    public void updatePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        updateUser(principal.getModel());
    }

    public List<String> getAdministratorsGroups() {
        return administratorGroups;
    }

    protected List<String> getLeafPermissions(String perm)
            throws ClientException {
        ArrayList<String> permissions = new ArrayList<String>();
        PermissionProvider permissionProvider;
        try {
            permissionProvider = Framework.getService(PermissionProvider.class);
        } catch (Exception e) {
            throw new Error("An unexpected error occured", e);
        }
        String[] subpermissions = permissionProvider.getSubPermissions(perm);
        if (subpermissions == null || subpermissions.length <= 0) {
            // it's a leaf
            permissions.add(perm);
            return permissions;
        }
        for (String subperm : subpermissions) {
            permissions.addAll(getLeafPermissions(subperm));
        }
        return permissions;
    }

    @Override
    public String[] getUsersForPermission(String perm, ACP acp) {
        // using a hashset to avoid duplicates
        HashSet<String> usernames = new HashSet<String>();

        ACL merged = acp.getMergedACLs("merged");
        // The list of permission that is has "perm" as its (compound)
        // permission
        ArrayList<ACE> filteredACEbyPerm = new ArrayList<ACE>();

        List<String> currentPermissions = null;

        try {
            currentPermissions = getLeafPermissions(perm);

            for (ACE ace : merged.getACEs()) {
                // Checking if the permission contains the permission we want to
                // check (we use the security service method for coumpound
                // permissions)
                List<String> acePermissions;

                acePermissions = getLeafPermissions(ace.getPermission());
                // Everything is a special permission (not compound)
                if (SecurityConstants.EVERYTHING.equals(ace.getPermission())) {
                    try {
                        acePermissions = Arrays.asList(Framework.getService(
                                PermissionProvider.class).getPermissions());
                    } catch (Exception e) {
                        throw new Error("An unexpected error occured", e);
                    }
                }

                if (acePermissions.containsAll(currentPermissions)) {
                    // special case: everybody perm grant false, don't take in
                    // account the previous ace
                    if (SecurityConstants.EVERYONE.equals(ace.getUsername())
                            && !ace.isGranted()) {
                        filteredACEbyPerm.clear();
                    } else {
                        filteredACEbyPerm.add(ace);
                    }
                }
            }
        } catch (ClientException e2) {
            throw new Error("An unexpected error occured", e2);
        }

        for (ACE ace : filteredACEbyPerm) {
            try {
                String aceUsername = ace.getUsername();
                List<String> users = null;
                // If everyone, add/remove all the users
                if (SecurityConstants.EVERYONE.equals(aceUsername)) {
                    users = getUserIds();
                }
                // if a group, add/remove all the user from the group (and
                // subgroups)
                if (users == null) {
                    NuxeoGroup group;
                    group = getGroup(aceUsername);
                    if (group != null) {
                        users = getUsersInGroupAndSubGroups(aceUsername);
                    }

                }
                // otherwise, add the user
                if (users == null) {
                    users = new ArrayList<String>();
                    users.add(aceUsername);
                }
                if (ace.isGranted()) {
                    usernames.addAll(users);
                } else {
                    usernames.removeAll(users);
                }
            } catch (ClientException e) {
                // Unexpected: throwing a runtime exception
                throw new Error(
                        "An unexpected error occured while getting user ids", e);
            }

        }
        return usernames.toArray(new String[usernames.size()]);
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
