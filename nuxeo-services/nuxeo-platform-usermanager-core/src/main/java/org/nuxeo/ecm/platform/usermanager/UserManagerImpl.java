/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     George Lefter
 *     Florent Guillaume
 *     Anahide Tchertchian
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.platform.usermanager;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.nuxeo.ecm.platform.usermanager.UserConfig.GROUPS_COLUMN;

import java.io.Serializable;
import java.security.Principal;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.AdministratorGroupsProvider;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheManagement;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor.SubstringMatchType;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryExpressionEvaluator;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.InvalidPasswordException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;

/**
 * Standard implementation of the Nuxeo UserManager.
 */
public class UserManagerImpl implements UserManager, MultiTenantUserManager, AdministratorGroupsProvider {

    private static final String VALIDATE_PASSWORD_PARAM = "nuxeo.usermanager.check.password";

    /** @since 11.1 */
    protected static final String SEARCH_ESCAPE_COMPAT_PARAM = "nuxeo.usermanager.search.escape.compat";

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

    public static final String INVALIDATE_PRINCIPAL_EVENT_ID = "invalidatePrincipal";

    public static final String INVALIDATE_ALL_PRINCIPALS_EVENT_ID = "invalidateAllPrincipals";

    /**
     * Possible value for the {@link DocumentEventContext#CATEGORY_PROPERTY_KEY} key of a core event context.
     *
     * @since 9.2
     */
    public static final String USER_GROUP_CATEGORY = "userGroup";

    /**
     * Key for the id of a user or a group in a core event context.
     *
     * @since 9.2
     */
    public static final String ID_PROPERTY_KEY = "id";

    /**
     * Key for the ancestor group names of a group in a core event context.
     *
     * @since 9.2
     */
    public static final String ANCESTOR_GROUPS_PROPERTY_KEY = "ancestorGroups";

    protected final DirectoryService dirService;

    protected final CacheService cacheService;

    protected Cache principalCache = null;

    public UserMultiTenantManagement multiTenantManagement = new DefaultUserMultiTenantManagement();

    /**
     * A structure used to inject field name configuration of users schema into a NuxeoPrincipalImpl instance. TODO not
     * all fields inside are configurable for now - they will use default values
     */
    protected UserConfig userConfig;

    /**
     * @since 9.3
     */
    protected GroupConfig groupConfig;

    protected String userDirectoryName;

    protected String userSchemaName;

    protected String userIdField;

    protected String userEmailField;

    protected Map<String, MatchType> userSearchFields;

    protected String groupDirectoryName;

    protected String groupSchemaName;

    protected String groupIdField;

    protected String groupLabelField;

    protected String groupMembersField;

    protected String groupSubGroupsField;

    protected String groupParentGroupsField;

    protected String groupSortField;

    protected Map<String, MatchType> groupSearchFields;

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
        dirService = Framework.getService(DirectoryService.class);
        cacheService = Framework.getService(CacheService.class);
        virtualUsers = new HashMap<>();
        userConfig = new UserConfig();
    }

    @Override
    public void setConfiguration(UserManagerDescriptor descriptor) {
        defaultGroup = descriptor.defaultGroup;
        administratorIds = descriptor.defaultAdministratorIds;
        disableDefaultAdministratorsGroup = false;
        if (descriptor.disableDefaultAdministratorsGroup != null) {
            disableDefaultAdministratorsGroup = descriptor.disableDefaultAdministratorsGroup;
        }
        administratorGroups = new ArrayList<>();
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
        groupLabelField = descriptor.groupLabelField;
        groupMembersField = descriptor.groupMembersField;
        groupSubGroupsField = descriptor.groupSubGroupsField;
        groupParentGroupsField = descriptor.groupParentGroupsField;
        groupSearchFields = descriptor.groupSearchFields;
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

        groupConfig = new GroupConfig();
        groupConfig.schemaName = groupSchemaName;
        groupConfig.idField = groupIdField;
        groupConfig.labelField = groupLabelField;
        groupConfig.membersField = groupMembersField;
        groupConfig.subGroupsField = groupSubGroupsField;
        groupConfig.parentGroupsField = groupParentGroupsField;

        if (cacheService != null && descriptor.userCacheName != null) {
            principalCache = cacheService.getCache(descriptor.userCacheName);
            invalidateAllPrincipals();
        }

    }

    protected void setUserDirectoryName(String userDirectoryName) {
        this.userDirectoryName = userDirectoryName;
        userSchemaName = dirService.getDirectorySchema(userDirectoryName);
        userIdField = dirService.getDirectoryIdField(userDirectoryName);
    }

    @Override
    public String getUserDirectoryName() {
        return userDirectoryName;
    }

    @Override
    public String getUserIdField() {
        return userIdField;
    }

    @Override
    public String getUserSchemaName() {
        return userSchemaName;
    }

    @Override
    public String getUserEmailField() {
        return userEmailField;
    }

    @Override
    public Set<String> getUserSearchFields() {
        return Collections.unmodifiableSet(userSearchFields.keySet());
    }

    @Override
    public Set<String> getGroupSearchFields() {
        return Collections.unmodifiableSet(groupSearchFields.keySet());
    }

    protected void setGroupDirectoryName(String groupDirectoryName) {
        this.groupDirectoryName = groupDirectoryName;
        groupSchemaName = dirService.getDirectorySchema(groupDirectoryName);
        groupIdField = dirService.getDirectoryIdField(groupDirectoryName);
    }

    @Override
    public String getGroupDirectoryName() {
        return groupDirectoryName;
    }

    @Override
    public String getGroupIdField() {
        return groupIdField;
    }

    @Override
    public String getGroupLabelField() {
        return groupLabelField;
    }

    @Override
    public String getGroupSchemaName() {
        return groupSchemaName;
    }

    @Override
    public String getGroupMembersField() {
        return groupMembersField;
    }

    @Override
    public String getGroupSubGroupsField() {
        return groupSubGroupsField;
    }

    @Override
    public String getGroupParentGroupsField() {
        return groupParentGroupsField;
    }

    @Override
    public String getUserListingMode() {
        return userListingMode;
    }

    @Override
    public String getGroupListingMode() {
        return groupListingMode;
    }

    @Override
    public String getDefaultGroup() {
        return defaultGroup;
    }

    @Override
    public Pattern getUserPasswordPattern() {
        return userPasswordPattern;
    }

    @Override
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

    protected void setVirtualUsers(Map<String, VirtualUserDescriptor> virtualUsers) {
        this.virtualUsers.clear();
        if (virtualUsers != null) {
            this.virtualUsers.putAll(virtualUsers);
        }
    }

    @Override
    public boolean checkUsernamePassword(String username, String password) {

        if (username == null || password == null) {
            log.warn("Trying to authenticate against null username or password");
            return false;
        }

        // deal with anonymous user
        String anonymousUserId = getAnonymousUserId();
        if (username.equals(anonymousUserId)) {
            log.warn(String.format("Trying to authenticate anonymous user (%s)", anonymousUserId));
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

        String userDirName;
        // BBB backward compat for userDirectory + userAuthentication
        if ("userDirectory".equals(userDirectoryName) && dirService.getDirectory("userAuthentication") != null) {
            userDirName = "userAuthentication";
        } else {
            userDirName = userDirectoryName;
        }
        try (Session userDir = dirService.open(userDirName)) {
            if (!userDir.isAuthenticating()) {
                log.error("Trying to authenticate against a non authenticating " + "directory: " + userDirName);
                return false;
            }

            boolean authenticated = userDir.authenticate(username, password);
            if (authenticated) {
                Framework.doPrivileged(() -> syncDigestAuthPassword(username, password));
            }
            return authenticated;
        }
    }

    protected void syncDigestAuthPassword(String username, String password) {
        if (StringUtils.isEmpty(digestAuthDirectory) || StringUtils.isEmpty(digestAuthRealm) || username == null
                || password == null) {
            return;
        }

        String ha1 = encodeDigestAuthPassword(username, digestAuthRealm, password);
        try (Session dir = dirService.open(digestAuthDirectory)) {
            dir.setReadAllColumns(true); // needed to read digest password
            String schema = dirService.getDirectorySchema(digestAuthDirectory);
            DocumentModel entry = dir.getEntry(username, true);
            if (entry == null) {
                entry = getDigestAuthModel();
                entry.setProperty(schema, dir.getIdField(), username);
                entry.setProperty(schema, dir.getPasswordField(), ha1);
                dir.createEntry(entry);
                log.debug("Created digest auth password for user:" + username);
            } else {
                String storedHa1 = (String) entry.getProperty(schema, dir.getPasswordField());
                if (!ha1.equals(storedHa1)) {
                    entry.setProperty(schema, dir.getPasswordField(), ha1);
                    dir.updateEntry(entry);
                    log.debug("Updated digest auth password for user:" + username);
                }
            }
        } catch (DirectoryException e) {
            log.warn("Digest auth password not synchronized, check your configuration", e);
        }
    }

    protected DocumentModel getDigestAuthModel() {
        String schema = dirService.getDirectorySchema(digestAuthDirectory);
        return BaseSession.createEntryModel(null, schema, null, null);
    }

    public static String encodeDigestAuthPassword(String username, String realm, String password) {
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

    @Override
    public boolean validatePassword(String password) {
        if (userPasswordPattern == null) {
            return true;
        } else {
            Matcher userPasswordMatcher = userPasswordPattern.matcher(password);
            return userPasswordMatcher.find();
        }
    }

    protected NuxeoPrincipal makeAnonymousPrincipal() {
        DocumentModel userEntry = makeVirtualUserEntry(getAnonymousUserId(), anonymousUser);
        // XXX: pass anonymous user groups, but they will be ignored
        return makePrincipal(userEntry, true, anonymousUser.getGroups());
    }

    protected NuxeoPrincipal makeVirtualPrincipal(VirtualUser user) {
        DocumentModel userEntry = makeVirtualUserEntry(user.getId(), user);
        return makePrincipal(userEntry, false, user.getGroups());
    }

    protected NuxeoPrincipal makeTransientPrincipal(String username) {
        DocumentModel userEntry = BaseSession.createEntryModel(null, userSchemaName, username, null);
        userEntry.setProperty(userSchemaName, userIdField, username);
        NuxeoPrincipal principal = makePrincipal(userEntry, false, true, null);
        String[] parts = username.split("/");
        String email = parts[1];
        principal.setFirstName(email);
        principal.setEmail(email);
        return principal;
    }

    protected DocumentModel makeVirtualUserEntry(String id, VirtualUser user) {
        final DocumentModel userEntry = BaseSession.createEntryModel(null, userSchemaName, id, null);
        // at least fill id field
        userEntry.setProperty(userSchemaName, userIdField, id);
        for (Entry<String, Serializable> prop : user.getProperties().entrySet()) {
            try {
                userEntry.setProperty(userSchemaName, prop.getKey(), prop.getValue());
            } catch (PropertyNotFoundException ce) {
                log.error("Property: " + prop.getKey() + " does not exists. Check your " + "UserService configuration.",
                        ce);
            }
        }
        return userEntry;
    }

    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry) {
        return makePrincipal(userEntry, false, null);
    }

    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry, boolean anonymous, List<String> groups) {
        return makePrincipal(userEntry, anonymous, false, groups);
    }

    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry, boolean anonymous, boolean isTransient,
            List<String> groups) {
        boolean admin = false;
        String username = userEntry.getId();

        List<String> virtualGroups = new LinkedList<>();
        // Add preconfigured groups: useful for LDAP, not for anonymous users
        if (defaultGroup != null && !anonymous && !isTransient) {
            virtualGroups.add(defaultGroup);
        }
        // Add additional groups: useful for virtual users
        if (groups != null && !isTransient) {
            virtualGroups.addAll(groups);
        }
        // Create a default admin if needed
        if (administratorIds != null && administratorIds.contains(username)) {
            admin = true;
            if (administratorGroups != null) {
                virtualGroups.addAll(administratorGroups);
            }
        }

        NuxeoPrincipalImpl principal = new NuxeoPrincipalImpl(username, anonymous, admin, false);
        principal.setConfig(userConfig);

        principal.setModel(userEntry, false);
        principal.setVirtualGroups(virtualGroups, true);

        // TODO: reenable roles initialization once we have a use case for
        // a role directory. In the mean time we only set the JBOSS role
        // that is required to login
        List<String> roles = Collections.singletonList("regular");
        principal.setRoles(roles);

        return principal;
    }

    protected boolean useCache() {
        return principalCache != null;
    }

    @Override
    public NuxeoPrincipal getPrincipal(String username) {
        return getPrincipal(username, true);
    }

    @Override
    public NuxeoPrincipal getPrincipal(String username, boolean fetchReferences) {
        // Caching is not needed for a principal without references because this object is only a reference on an
        // already cached User Model
        if (useCache() && fetchReferences) {
            return getPrincipalUsingCache(username);
        }
        return getPrincipal(username, null, fetchReferences);
    }

    protected NuxeoPrincipal getPrincipalUsingCache(String username) {
        NuxeoPrincipal ret = (NuxeoPrincipal) principalCache.get(username);
        if (ret == null) {
            ret = getPrincipal(username, null);
            if (ret == null) {
                return ret;
            }
            ((CacheManagement) principalCache).putLocal(username, ret);
        }
        return ((NuxeoPrincipalImpl) ret).cloneTransferable(); // should not return cached principal
    }

    @Override
    public DocumentModel getUserModel(String userName) {
        return getUserModel(userName, null);
    }

    @Override
    public DocumentModel getBareUserModel() {
        String schema = dirService.getDirectorySchema(userDirectoryName);
        return BaseSession.createEntryModel(null, schema, null, null);
    }

    @Override
    public NuxeoGroup getGroup(String groupName) {
        return getGroup(groupName, null);
    }

    protected NuxeoGroup getGroup(String groupName, DocumentModel context) {
        DocumentModel groupEntry = getGroupModel(groupName, context);
        if (groupEntry != null) {
            return makeGroup(groupEntry);
        }
        return null;

    }

    @Override
    public DocumentModel getGroupModel(String groupName) {
        return getGroupModel(groupName, null);
    }

    @SuppressWarnings("unchecked")
    protected NuxeoGroup makeGroup(DocumentModel groupEntry) {
        return new NuxeoGroupImpl(groupEntry, groupConfig);
    }

    @Override
    public List<String> getTopLevelGroups() {
        return getTopLevelGroups(null);
    }

    @Override
    public List<String> getGroupsInGroup(String parentId) {
        NuxeoGroup group = getGroup(parentId, null);
        if (group != null) {
            return group.getMemberGroups();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getUsersInGroup(String groupId) {
        return getGroup(groupId).getMemberUsers();
    }

    @Override
    public List<String> getUsersInGroupAndSubGroups(String groupId) {
        return getUsersInGroupAndSubGroups(groupId, null);
    }

    protected void appendSubgroups(String groupId, Set<String> groups, DocumentModel context) {
        List<String> groupsToAppend = getGroupsInGroup(groupId, context);
        groups.addAll(groupsToAppend);
        for (String subgroupId : groupsToAppend) {
            groups.add(subgroupId);
            // avoiding infinite loop
            if (!groups.contains(subgroupId)) {
                appendSubgroups(subgroupId, groups, context);
            }
        }

    }

    protected boolean isAnonymousMatching(Map<String, Serializable> filter, Set<String> fulltext) {
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
                    if (!value.toString().toLowerCase().startsWith(expected.toString().toLowerCase())) {
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

    protected boolean isAnonymousMatching(QueryBuilder queryBuilder, Directory dir) {
        String anonymousUserId = getAnonymousUserId();
        if (anonymousUserId == null) {
            return false;
        }
        MultiExpression expression = queryBuilder.predicate();
        if (expression.predicates.isEmpty()) {
            return true;
        }
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, Object> entry = (Map) anonymousUser.getProperties();
        entry.put(userIdField, anonymousUserId);
        return new MemoryDirectoryExpressionEvaluator(dir).matchesEntry(expression, entry);
    }

    @Override
    public List<NuxeoPrincipal> searchPrincipals(String pattern) {
        DocumentModelList entries = searchUsers(pattern);
        List<NuxeoPrincipal> principals = new ArrayList<>(entries.size());
        for (DocumentModel entry : entries) {
            principals.add(makePrincipal(entry));
        }
        return principals;
    }

    @Override
    public DocumentModelList searchGroups(String pattern) {
        return searchGroups(pattern, null);
    }

    @Override
    public String getUserSortField() {
        return userSortField;
    }

    protected Map<String, String> getUserSortMap() {
        return getDirectorySortMap(userSortField, userIdField);
    }

    protected OrderByExpr getUserOrderBy() {
        String sortField = StringUtils.defaultString(userSortField, userIdField);
        return OrderByExprs.asc(sortField);
    }

    protected Map<String, String> getGroupSortMap() {
        return getDirectorySortMap(groupSortField, groupIdField);
    }

    protected OrderByExpr getGroupOrderBy() {
        String sortField = StringUtils.defaultString(groupSortField, groupIdField);
        return OrderByExprs.asc(sortField);
    }

    protected Map<String, String> getDirectorySortMap(String descriptorSortField, String fallBackField) {
        String sortField = descriptorSortField != null ? descriptorSortField : fallBackField;
        Map<String, String> orderBy = new HashMap<>();
        orderBy.put(sortField, DocumentModelComparator.ORDER_ASC);
        return orderBy;
    }

    /**
     * @since 8.2
     */
    protected void notifyCore(String userOrGroupId, String eventId) {
        notifyCore(userOrGroupId, eventId, null);
    }

    /**
     * @since 9.2
     */
    protected void notifyCore(String userOrGroupId, String eventId, List<String> ancestorGroupIds) {
        Map<String, Serializable> eventProperties = new HashMap<>();
        eventProperties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, USER_GROUP_CATEGORY);
        eventProperties.put(ID_PROPERTY_KEY, userOrGroupId);
        if (ancestorGroupIds != null) {
            eventProperties.put(ANCESTOR_GROUPS_PROPERTY_KEY, (Serializable) ancestorGroupIds);
        }
        NuxeoPrincipal principal = ClientLoginModule.getCurrentPrincipal();
        UnboundEventContext envContext = new UnboundEventContext(principal, eventProperties);
        envContext.setProperties(eventProperties);
        EventProducer eventProducer = Framework.getService(EventProducer.class);
        eventProducer.fireEvent(envContext.newEvent(eventId));
    }

    protected void notifyRuntime(String userOrGroupName, String eventId) {
        EventService eventService = Framework.getService(EventService.class);
        eventService.sendEvent(new Event(USERMANAGER_TOPIC, eventId, this, userOrGroupName));
    }

    @Override
    public void notifyUserChanged(String userName, String eventId) {
        invalidatePrincipal(userName);
        notifyRuntime(userName, USERCHANGED_EVENT_ID);
        if (eventId != null) {
            notifyRuntime(userName, eventId);
            notifyCore(userName, eventId);
        }
    }

    protected void invalidatePrincipal(String userName) {
        if (useCache()) {
            principalCache.invalidate(userName);
        }
    }

    @Override
    public void notifyGroupChanged(String groupName, String eventId, List<String> ancestorGroupNames) {
        invalidateAllPrincipals();
        notifyRuntime(groupName, GROUPCHANGED_EVENT_ID);
        if (eventId != null) {
            notifyRuntime(groupName, eventId);
            notifyCore(groupName, eventId, ancestorGroupNames);
        }
    }

    protected void invalidateAllPrincipals() {
        if (useCache()) {
            principalCache.invalidateAll();
        }
    }

    @Override
    public Boolean areGroupsReadOnly() {
        try (Session groupDir = dirService.open(groupDirectoryName)) {
            return groupDir.isReadOnly();
        } catch (DirectoryException e) {
            log.error(e);
            return false;
        }
    }

    @Override
    public Boolean areUsersReadOnly() {
        try (Session userDir = dirService.open(userDirectoryName)) {
            return userDir.isReadOnly();
        } catch (DirectoryException e) {
            log.error(e);
            return false;
        }
    }

    protected void checkGrouId(DocumentModel groupModel) {
        // be sure the name does not contains trailing spaces
        Object groupIdValue = groupModel.getProperty(groupSchemaName, groupIdField);
        if (groupIdValue != null) {
            groupModel.setProperty(groupSchemaName, groupIdField, groupIdValue.toString().trim());
        }
    }

    protected String getGroupId(DocumentModel groupModel) {
        Object groupIdValue = groupModel.getProperty(groupSchemaName, groupIdField);
        if (groupIdValue != null && !(groupIdValue instanceof String)) {
            throw new NuxeoException("Invalid group id " + groupIdValue);
        }
        return (String) groupIdValue;
    }

    protected void checkUserId(DocumentModel userModel) {
        Object userIdValue = userModel.getProperty(userSchemaName, userIdField);
        if (userIdValue != null) {
            userModel.setProperty(userSchemaName, userIdField, userIdValue.toString().trim());
        }
    }

    protected String getUserId(DocumentModel userModel) {
        Object userIdValue = userModel.getProperty(userSchemaName, userIdField);
        if (userIdValue != null && !(userIdValue instanceof String)) {
            throw new NuxeoException("Invalid user id " + userIdValue);
        }
        return (String) userIdValue;
    }

    @Override
    public DocumentModel createGroup(DocumentModel groupModel) {
        return createGroup(groupModel, null);
    }

    @Override
    public DocumentModel createUser(DocumentModel userModel) {
        return createUser(userModel, null);
    }

    @Override
    public void deleteGroup(String groupId) {
        deleteGroup(groupId, null);
    }

    @Override
    public void deleteGroup(DocumentModel groupModel) {
        deleteGroup(groupModel, null);
    }

    @Override
    public void deleteUser(String userId) {
        deleteUser(userId, null);
    }

    @Override
    public void deleteUser(DocumentModel userModel) {
        String userId = getUserId(userModel);
        deleteUser(userId);
    }

    @Override
    public List<String> getGroupIds() {
        try (Session groupDir = dirService.open(groupDirectoryName)) {
            List<String> groupIds = groupDir.getProjection(Collections.<String, Serializable> emptyMap(),
                    groupDir.getIdField());
            Collections.sort(groupIds);
            return groupIds;
        }
    }

    @Override
    public List<String> getUserIds() {
        return getUserIds(null);
    }

    protected void removeVirtualFilters(Map<String, Serializable> filter) {
        if (filter == null) {
            return;
        }
        List<String> keys = new ArrayList<>(filter.keySet());
        for (String key : keys) {
            if (key.startsWith(VIRTUAL_FIELD_FILTER_PREFIX)) {
                filter.remove(key);
            }
        }
    }

    protected QueryBuilder getQueryForPattern(String pattern, String dirName, Map<String, MatchType> searchFields,
            OrderByExpr orderBy) {
        QueryBuilder queryBuilder = new QueryBuilder();
        if (!StringUtils.isBlank(pattern)) {
            // build query
            pattern = pattern.trim().toLowerCase();
            String likePattern;
            if (!useSearchEscapeCompat()) {
                // replace characters that are meaningful in a LIKE pattern
                likePattern = pattern.replace("\\", "\\\\");
                likePattern = likePattern.replace("%", "\\%");
                likePattern = likePattern.replace("_", "\\_");
            } else {
                // compat: don't do any escaping
                likePattern = pattern;
            }
            List<Predicate> predicates = new ArrayList<>();
            for (Entry<String, MatchType> fieldEntry : searchFields.entrySet()) {
                String key = fieldEntry.getKey();
                Predicate predicate;
                if (fieldEntry.getValue() == MatchType.SUBSTRING) {
                    Directory dir = dirService.getDirectory(dirName);
                    SubstringMatchType substringMatchType = dir.getDescriptor().getSubstringMatchType();
                    String value;
                    switch (substringMatchType) {
                    case subany:
                        value = '%' + likePattern + '%';
                        break;
                    case subinitial:
                        value = likePattern + '%';
                        break;
                    case subfinal:
                        value = '%' + likePattern;
                        break;
                    default:
                        throw new IllegalStateException(substringMatchType.toString());
                    }
                    predicate = Predicates.ilike(key, value);
                } else { // MatchType.EXACT
                    predicate = Predicates.eq(key, pattern);
                }
                predicates.add(predicate);
            }
            queryBuilder.filter(new MultiExpression(Operator.OR, predicates));
        }
        queryBuilder.order(orderBy);
        return queryBuilder;
    }

    @Override
    public DocumentModelList searchGroups(Map<String, Serializable> filter, Set<String> fulltext) {
        return searchGroups(filter, fulltext, null);
    }

    @Override
    public DocumentModelList searchGroups(QueryBuilder queryBuilder) {
        return searchGroups(queryBuilder, null);
    }

    @Override
    public DocumentModelList searchUsers(String pattern) {
        return searchUsers(pattern, null);
    }

    @Override
    public DocumentModelList searchUsers(Map<String, Serializable> filter, Set<String> fulltext) {
        return searchUsers(filter, fulltext, getUserSortMap(), null);
    }

    @Override
    public DocumentModelList searchUsers(QueryBuilder queryBuilder) {
        return searchUsers(queryBuilder, null);
    }

    @Override
    public void updateGroup(DocumentModel groupModel) {
        updateGroup(groupModel, null);
    }

    @Override
    public void updateUser(DocumentModel userModel) {
        updateUser(userModel, null);
    }

    @Override
    public DocumentModel getBareGroupModel() {
        String schema = dirService.getDirectorySchema(groupDirectoryName);
        return BaseSession.createEntryModel(null, schema, null, null);
    }

    @Override
    public List<String> getAdministratorsGroups() {
        return administratorGroups;
    }

    protected List<String> getLeafPermissions(String perm) {
        ArrayList<String> permissions = new ArrayList<>();
        PermissionProvider permissionProvider = Framework.getService(PermissionProvider.class);
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
        return getUsersForPermission(perm, acp, null);
    }

    @Override
    public Principal authenticate(String name, String password) {
        return checkUsernamePassword(name, password) ? getPrincipal(name) : null;
    }

    /*************** MULTI-TENANT-IMPLEMENTATION ************************/

    public DocumentModelList searchUsers(Map<String, Serializable> filter, Set<String> fulltext,
            Map<String, String> orderBy, DocumentModel context) {
        try (Session userDir = dirService.open(userDirectoryName, context)) {
            removeVirtualFilters(filter);

            // XXX: do not fetch references, can be costly
            DocumentModelList entries = userDir.query(filter, fulltext, null, false);
            if (isAnonymousMatching(filter, fulltext)) {
                entries.add(makeVirtualUserEntry(getAnonymousUserId(), anonymousUser));
            }

            // TODO: match searchable virtual users

            if (orderBy != null && !orderBy.isEmpty()) {
                // sort: cannot sort before virtual users are added
                entries.sort(new DocumentModelComparator(userSchemaName, orderBy));
            }

            return entries;
        }
    }

    @Override
    public List<String> getUsersInGroup(String groupId, DocumentModel context) {
        String storeGroupId = multiTenantManagement.groupnameTranformer(this, groupId, context);
        return getGroup(storeGroupId).getMemberUsers();
    }

    @Override
    public DocumentModelList searchUsers(String pattern, DocumentModel context) {
        QueryBuilder queryBuilder = getQueryForPattern(pattern, userDirectoryName, userSearchFields, getUserOrderBy());
        return searchUsers(queryBuilder, context);
    }

    @Override
    public DocumentModelList searchUsers(QueryBuilder queryBuilder, DocumentModel context) {
        Directory dir = dirService.getDirectory(userDirectoryName, context);
        try (Session session = dir.getSession()) {
            if (isAnonymousMatching(queryBuilder, dir)) {
                DocumentModel anonymousEntry = makeVirtualUserEntry(getAnonymousUserId(), anonymousUser);
                List<DocumentModel> virtualEntries = Collections.singletonList(anonymousEntry);
                return queryWithVirtualEntries(session, queryBuilder, virtualEntries);
            } else {
                return session.query(queryBuilder, false);
            }
        }
    }

    /**
     * Executes a query then adds virtual entries (already supposed to match the query). Then does
     * limit/offset/order/countTotal.
     *
     * @since 10.3
     */
    protected DocumentModelList queryWithVirtualEntries(Session session, QueryBuilder queryBuilder,
            List<DocumentModel> virtualEntries) {
        AbstractDirectory dir = (AbstractDirectory) ((BaseSession) session).getDirectory();

        // do the basic query
        DocumentModelList entries = session.query(queryBuilder, false);

        int limit = Math.max(0, (int) queryBuilder.limit());
        int offset = Math.max(0, (int) queryBuilder.offset());
        boolean countTotal = queryBuilder.countTotal();
        OrderByList orders = queryBuilder.orders();
        int size = entries.size();
        long totalSize = entries.totalSize();

        // if we have all the results or a page small enough that all virtual entries fit, just add them
        if (offset == 0 && (limit == 0 || size + virtualEntries.size() <= limit)) {
            // add virtual entries
            entries.addAll(virtualEntries);
            if (totalSize >= 0) {
                ((DocumentModelListImpl) entries).setTotalSize(totalSize + virtualEntries.size());
            }
            // re-sort
            dir.orderEntries(entries, AbstractDirectory.makeOrderBy(orders));
            return entries;
        }

        // else we have to do a manual paging/sorting...
        // re-do the full query with limit/offset
        queryBuilder = new QueryBuilder(queryBuilder).limit(0).offset(0).orders(Collections.emptyList());
        entries = session.query(queryBuilder, false);
        // add virtual entries
        entries.addAll(virtualEntries);
        // sort
        if (!orders.isEmpty()) {
            dir.orderEntries(entries, AbstractDirectory.makeOrderBy(orders));
        }
        // manual limit/offset
        entries = ((BaseSession) session).applyQueryLimits(entries, limit, offset);
        if (!countTotal) { // offset != 0 always here
            // compat with other directories
            ((DocumentModelListImpl) entries).setTotalSize(-2);
        }
        return entries;
    }

    @Override
    public DocumentModelList searchUsers(Map<String, Serializable> filter, Set<String> fulltext,
            DocumentModel context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getGroupIds(DocumentModel context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DocumentModelList searchGroups(Map<String, Serializable> filter, Set<String> fulltext,
            DocumentModel context) {
        filter = filter != null ? cloneMap(filter) : new HashMap<>();
        HashSet<String> fulltextClone = fulltext != null ? cloneSet(fulltext) : new HashSet<>();
        multiTenantManagement.queryTransformer(this, filter, fulltextClone, context);

        try (Session groupDir = dirService.open(groupDirectoryName, context)) {
            removeVirtualFilters(filter);
            return groupDir.query(filter, fulltextClone, getGroupSortMap(), false);
        }
    }

    @Override
    public DocumentModelList searchGroups(QueryBuilder queryBuilder, DocumentModel context) {
        queryBuilder = multiTenantManagement.groupQueryTransformer(this, queryBuilder, context);
        try (Session session = dirService.open(groupDirectoryName, context)) {
            return session.query(queryBuilder, false);
        }
    }

    @Override
    public DocumentModel createGroup(DocumentModel groupModel, DocumentModel context)
            throws GroupAlreadyExistsException {
        groupModel = multiTenantManagement.groupTransformer(this, groupModel, context);

        // be sure the name does not contains trailing spaces
        checkGrouId(groupModel);

        try (Session groupDir = dirService.open(groupDirectoryName, context)) {
            String groupId = getGroupId(groupModel);

            // check the group does not exist
            if (groupDir.hasEntry(groupId)) {
                throw new GroupAlreadyExistsException();
            }
            groupModel = groupDir.createEntry(groupModel);
            notifyGroupChanged(groupId, GROUPCREATED_EVENT_ID);
            return groupModel;

        }
    }

    @Override
    public DocumentModel getGroupModel(String groupIdValue, DocumentModel context) {
        String groupName = multiTenantManagement.groupnameTranformer(this, groupIdValue, context);
        if (groupName != null) {
            groupName = groupName.trim();
        }

        try (Session groupDir = dirService.open(groupDirectoryName, context)) {
            return groupDir.getEntry(groupName);
        }
    }

    @Override
    public DocumentModel getUserModel(String userName, DocumentModel context) {
        return getUserModel(userName, context, true);
    }

    protected DocumentModel getUserModel(String userName, DocumentModel context, boolean fetchReferences) {
        if (userName == null) {
            return null;
        }

        userName = userName.trim();
        // return anonymous model
        if (anonymousUser != null && userName.equals(anonymousUser.getId())) {
            return makeVirtualUserEntry(getAnonymousUserId(), anonymousUser);
        }

        try (Session userDir = dirService.open(userDirectoryName, context)) {
            return userDir.getEntry(userName, fetchReferences);
        }
    }

    protected Map<String, Serializable> cloneMap(Map<String, Serializable> map) {
        Map<String, Serializable> result = new HashMap<>();
        for (String key : map.keySet()) {
            result.put(key, map.get(key));
        }
        return result;
    }

    protected HashSet<String> cloneSet(Set<String> set) {
        HashSet<String> result = new HashSet<>();
        for (String key : set) {
            result.add(key);
        }
        return result;
    }

    @Override
    public NuxeoPrincipal getPrincipal(String username, DocumentModel context) {
        return getPrincipal(username, context, true);
    }

    protected NuxeoPrincipal getPrincipal(String username, DocumentModel context, boolean fetchReferences) {
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
        if (NuxeoPrincipal.isTransientUsername(username)) {
            return makeTransientPrincipal(username);
        }
        DocumentModel userModel = getUserModel(username, context, fetchReferences);
        if (userModel != null) {
            return makePrincipal(userModel);
        }
        return null;
    }

    @Override
    public DocumentModelList searchGroups(String pattern, DocumentModel context) {
        QueryBuilder queryBuilder = getQueryForPattern(pattern, groupDirectoryName, groupSearchFields,
                getGroupOrderBy());
        return searchGroups(queryBuilder, context);
    }

    @Override
    public List<String> getUserIds(DocumentModel context) {
        try (Session userDir = dirService.open(userDirectoryName, context)) {
            List<String> userIds = userDir.getProjection(Collections.<String, Serializable> emptyMap(),
                    userDir.getIdField());
            Collections.sort(userIds);
            return userIds;
        }
    }

    @Override
    public DocumentModel createUser(DocumentModel userModel, DocumentModel context) throws UserAlreadyExistsException {
        // be sure UserId does not contains any trailing spaces
        checkUserId(userModel);

        try (Session userDir = dirService.open(userDirectoryName, context)) {
            String userId = getUserId(userModel);

            // check the user does not exist
            if (userDir.hasEntry(userId)) {
                throw new UserAlreadyExistsException();
            }

            checkPasswordValidity(userModel);

            String schema = dirService.getDirectorySchema(userDirectoryName);
            // If trying to create the user with groups, check that the groups exist
            checkGroupsExistence(userModel, schema, context);

            String clearUsername = (String) userModel.getProperty(schema, userDir.getIdField());
            String clearPassword = (String) userModel.getProperty(schema, userDir.getPasswordField());

            userModel = userDir.createEntry(userModel);

            syncDigestAuthPassword(clearUsername, clearPassword);

            notifyUserChanged(userId, USERCREATED_EVENT_ID);
            return userModel;

        }
    }

    @SuppressWarnings("unchecked")
    protected void checkGroupsExistence(DocumentModel userModel, String schema, DocumentModel context) {
        List<String> groups = (List<String>) userModel.getProperty(schema, GROUPS_COLUMN);
        if (groups == null || groups.isEmpty()) {
            return;
        }
        try (Session groupDir = dirService.open(groupDirectoryName, context)) {
            for (String group : groups) {
                if (!groupDir.hasEntry(group)) {
                    throw new NuxeoException("group does not exist: " + group, SC_FORBIDDEN);
                }
            }
        }
    }

    protected void checkPasswordValidity(DocumentModel userModel) throws InvalidPasswordException {
        if (!mustCheckPasswordValidity()) {
            return;
        }
        String schema = dirService.getDirectorySchema(userDirectoryName);
        String passwordField = dirService.getDirectory(userDirectoryName).getPasswordField();

        Property passwordProperty = userModel.getPropertyObject(schema, passwordField);

        if (passwordProperty.isDirty()) {
            String clearPassword = (String) passwordProperty.getValue();
            if (StringUtils.isNotBlank(clearPassword) && !validatePassword(clearPassword)) {
                throw new InvalidPasswordException();
            }
        }
    }

    @Override
    public void updateUser(DocumentModel userModel, DocumentModel context) {
        try (Session userDir = dirService.open(userDirectoryName, context)) {
            String userId = getUserId(userModel);

            if (!userDir.hasEntry(userId)) {
                throw new DirectoryException("user does not exist: " + userId);
            }

            String schema = dirService.getDirectorySchema(userDirectoryName);
            // If trying to update the user with groups, check that the groups exist
            checkGroupsExistence(userModel, schema, context);

            checkPasswordValidity(userModel);

            String clearUsername = (String) userModel.getProperty(schema, userDir.getIdField());
            String clearPassword = (String) userModel.getProperty(schema, userDir.getPasswordField());

            userDir.updateEntry(userModel);

            syncDigestAuthPassword(clearUsername, clearPassword);

            notifyUserChanged(userId, USERMODIFIED_EVENT_ID);
        }
    }

    private boolean mustCheckPasswordValidity() {
        return Framework.getService(ConfigurationService.class).isBooleanPropertyTrue(VALIDATE_PASSWORD_PARAM);
    }

    protected boolean useSearchEscapeCompat() {
        return Framework.getService(ConfigurationService.class).isBooleanTrue(SEARCH_ESCAPE_COMPAT_PARAM);
    }

    @Override
    public void deleteUser(DocumentModel userModel, DocumentModel context) {
        String userId = getUserId(userModel);
        deleteUser(userId, context);
    }

    @Override
    public void deleteUser(String userId, DocumentModel context) {
        try (Session userDir = dirService.open(userDirectoryName, context)) {
            if (!userDir.hasEntry(userId)) {
                throw new DirectoryException("User does not exist: " + userId);
            }
            userDir.deleteEntry(userId);
            notifyUserChanged(userId, USERDELETED_EVENT_ID);

        } finally {
            notifyUserChanged(userId, null);
        }
    }

    @Override
    public void updateGroup(DocumentModel groupModel, DocumentModel context) {
        try (Session groupDir = dirService.open(groupDirectoryName, context)) {
            String groupId = getGroupId(groupModel);

            if (!groupDir.hasEntry(groupId)) {
                throw new DirectoryException("group does not exist: " + groupId);
            }
            groupDir.updateEntry(groupModel);
            notifyGroupChanged(groupId, GROUPMODIFIED_EVENT_ID);
        }
    }

    @Override
    public void deleteGroup(DocumentModel groupModel, DocumentModel context) {
        String groupId = getGroupId(groupModel);
        deleteGroup(groupId, context);
    }

    @Override
    public void deleteGroup(String groupId, DocumentModel context) {
        try (Session groupDir = dirService.open(groupDirectoryName, context)) {
            if (!groupDir.hasEntry(groupId)) {
                throw new DirectoryException("Group does not exist: " + groupId);
            }
            // Get ancestor group names before deletion to pass them as a property of the core event
            List<String> ancestorGroupNames = getAncestorGroups(groupId);
            groupDir.deleteEntry(groupId);
            notifyGroupChanged(groupId, GROUPDELETED_EVENT_ID, ancestorGroupNames);
        }
    }

    @Override
    public List<String> getGroupsInGroup(String parentId, DocumentModel context) {
        return getGroup(parentId, null).getMemberGroups();
    }

    @Override
    public List<String> getTopLevelGroups(DocumentModel context) {
        try (Session groupDir = dirService.open(groupDirectoryName, context)) {
            List<String> topLevelGroups = new LinkedList<>();
            // XXX retrieve all entries with references, can be costly.
            DocumentModelList groups = groupDir.query(Collections.<String, Serializable> emptyMap(), null, null, true);
            for (DocumentModel group : groups) {
                @SuppressWarnings("unchecked")
                List<String> parents = (List<String>) group.getProperty(groupSchemaName, groupParentGroupsField);

                if (parents == null || parents.isEmpty()) {
                    topLevelGroups.add(group.getId());
                }
            }
            return topLevelGroups;
        }
    }

    @Override
    public List<String> getUsersInGroupAndSubGroups(String groupId, DocumentModel context) {
        Set<String> groups = new HashSet<>();
        groups.add(groupId);
        appendSubgroups(groupId, groups, context);

        Set<String> users = new HashSet<>();
        for (String groupid : groups) {
            users.addAll(getGroup(groupid, context).getMemberUsers());
        }

        return new ArrayList<>(users);
    }

    @Override
    public String[] getUsersForPermission(String perm, ACP acp, DocumentModel context) {
        PermissionProvider permissionProvider = Framework.getService(PermissionProvider.class);
        // using a hashset to avoid duplicates
        HashSet<String> usernames = new HashSet<>();

        ACL merged = acp.getMergedACLs("merged");
        // The list of permission that is has "perm" as its (compound)
        // permission
        ArrayList<ACE> filteredACEbyPerm = new ArrayList<>();

        List<String> currentPermissions = getLeafPermissions(perm);

        for (ACE ace : merged.getACEs()) {
            // Checking if the permission contains the permission we want to
            // check (we use the security service method for coumpound
            // permissions)
            List<String> acePermissions = getLeafPermissions(ace.getPermission());

            // Everything is a special permission (not compound)
            if (SecurityConstants.EVERYTHING.equals(ace.getPermission())) {
                acePermissions = Arrays.asList(permissionProvider.getPermissions());
            }

            if (acePermissions.containsAll(currentPermissions)) {
                // special case: everybody perm grant false, don't take in
                // account the previous ace
                if (SecurityConstants.EVERYONE.equals(ace.getUsername()) && !ace.isGranted()) {
                    break;
                }
                filteredACEbyPerm.add(ace);
            }
        }

        for (ACE ace : filteredACEbyPerm) {
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
                group = getGroup(aceUsername, context);
                if (group != null) {
                    users = getUsersInGroupAndSubGroups(aceUsername, context);
                }

            }
            // otherwise, add the user
            if (users == null) {
                users = new ArrayList<>();
                users.add(aceUsername);
            }
            if (ace.isGranted()) {
                usernames.addAll(users);
            } else {
                usernames.removeAll(users);
            }
        }
        return usernames.toArray(new String[usernames.size()]);
    }

    @Override
    public List<String> getAncestorGroups(String groupId) {
        List<String> ancestorGroups = new ArrayList<>();
        populateAncestorGroups(groupId, ancestorGroups);
        return ancestorGroups;
    }

    protected void populateAncestorGroups(String groupId, List<String> ancestorGroups) {
        NuxeoGroup group = getGroup(groupId);
        if (group != null) {
            List<String> parentGroups = group.getParentGroups();
            // Avoid infinite loop in case a group has one of its parents as a subgroup
            parentGroups.stream().filter(parentGroup -> !ancestorGroups.contains(parentGroup)).forEach(parentGroup -> {
                ancestorGroups.add(parentGroup);
                populateAncestorGroups(parentGroup, ancestorGroups);
            });
        }
    }

    @Override
    public GroupConfig getGroupConfig() {
        return groupConfig;
    }

    @Override
    public List<String> getDescendantGroups(String groupId){
        List<String> descendantGroups = new ArrayList<>();
        populateDescendantGroups(groupId, descendantGroups);
        return descendantGroups;
    }

    protected void populateDescendantGroups(String groupId, List<String> descendantGroups) {
        NuxeoGroup group = getGroup(groupId);
        if (group != null) {
            List<String> subGroups = group.getMemberGroups();
            subGroups.stream().filter(subGroup -> !descendantGroups.contains(subGroup)).forEach(subGroup -> {
                descendantGroups.add(subGroup);
                populateDescendantGroups(subGroup, descendantGroups);
            });
        }
    }

    @Override
    public void handleEvent(Event event) {
        String id = event.getId();
        if (INVALIDATE_PRINCIPAL_EVENT_ID.equals(id)) {
            invalidatePrincipal((String) event.getData());
        } else if (INVALIDATE_ALL_PRINCIPALS_EVENT_ID.equals(id)) {
            invalidateAllPrincipals();
        }
    }

}
