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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventService;

/**
 * @author George Lefter
 * @author Florent Guillaume
 */
public class UserManagerImpl implements UserManager {

    private static final Log log = LogFactory.getLog(UserManagerImpl.class);

    public static final String USERMANAGER_TOPIC = "usermanager";

    public static final String USERCHANGED_EVENT_ID = "user_changed";

    public static final String GROUPCHANGED_EVENT_ID = "group_changed";

    public static final String DEFAULT_ANONYMOUS_USER_ID = "Anonymous";

    private final DirectoryService dirService;

    private String userDirectoryName;

    private String userSchemaName;

    private String userEmailField;

    private Map<String, MatchType> userSearchFields;

    private String groupDirectoryName;

    private String groupSchemaName;

    private String groupMembersField;

    private String groupSubGroupsField;

    private String groupParentGroupsField;

    private String defaultGroup;

    private String defaultRootLogin;

    private String userSortField;

    private String userListingMode;

    private String groupListingMode;

    private Pattern userPasswordPattern;

    private String anonymousUserId;

    private Map<String, Object> anonymousUserMap;

    public UserManagerImpl() {
        dirService = Framework.getLocalService(DirectoryService.class);
    }

    public void setUserDirectoryName(String userDirectoryName) {
        this.userDirectoryName = userDirectoryName;
        try {
            userSchemaName = dirService.getDirectorySchema(userDirectoryName);
        } catch (ClientException e) {
            throw new RuntimeException("Unkown user directory "
                    + userDirectoryName, e);
        }
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
        return Collections.unmodifiableSet(userSearchFields.keySet());
    }

    public void setGroupDirectoryName(String groupDirectoryName) {
        this.groupDirectoryName = groupDirectoryName;
        try {
            groupSchemaName = dirService.getDirectorySchema(groupDirectoryName);
        } catch (ClientException e) {
            throw new RuntimeException("Unkown group directory "
                    + groupDirectoryName, e);
        }
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

    public Pattern getUserPasswordPattern() {
        return userPasswordPattern;
    }

    public void setUserPasswordPattern(Pattern userPasswordPattern) {
        this.userPasswordPattern = userPasswordPattern;
    }

    public void setAnonymousUser(Map<String, String> anonymousUser) {
        if (anonymousUser == null) {
            anonymousUserId = null;
            anonymousUserMap = null;
        } else {
            Map<String, Object> map = new HashMap<String, Object>(anonymousUser);
            anonymousUserId = (String) map.remove(ANONYMOUS_USER_ID_KEY);
            if (anonymousUserId == null) {
                anonymousUserId = DEFAULT_ANONYMOUS_USER_ID;
            }
            map.put(NuxeoPrincipalImpl.USERNAME_COLUMN, anonymousUserId);
            anonymousUserMap = Collections.unmodifiableMap(map);
        }
    }

    public String getAnonymousUserId() {
        return anonymousUserId;
    }

    public void remove() throws ClientException {
    }

    public boolean checkUsernamePassword(String username, String password)
            throws ClientException {

        if (username == null || password == null) {
            log.warn("Trying to authenticate against null username or password");
            return false;
        }
        if (username.equals(anonymousUserId)) {
            log.warn(String.format(
                    "Trying to authenticate anonymous user (%s)",
                    anonymousUserId));
            return false;
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
            return userDir.authenticate(username, password);
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
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
        final DocumentModelImpl userEntry = new DocumentModelImpl(null,
                userSchemaName, anonymousUserId, null, null, null,
                new String[] { userSchemaName }, null);
        userEntry.addDataModel(new DataModelImpl(userSchemaName,
                anonymousUserMap));
        return makePrincipal(userEntry, true);
    }

    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry)
            throws ClientException {
        return makePrincipal(userEntry, false);
    }

    protected NuxeoPrincipal makePrincipal(DocumentModel userEntry,
            boolean anonymous) throws ClientException {
        NuxeoPrincipalImpl principal = new NuxeoPrincipalImpl(
                userEntry.getId(), anonymous);

        principal.setModel(userEntry);

        if (!anonymous) {
            List<String> virtualGroups = new LinkedList<String>();

            // Add preconfigured groups : useful for LDAP
            if (defaultGroup != null) {
                virtualGroups.add(defaultGroup);
            }
            // Create a default admin if needed
            if (defaultRootLogin != null
                    && defaultRootLogin.equals(principal.getName())) {
                virtualGroups.add("administrators");
            }
            principal.setVirtualGroups(virtualGroups);
        }

        // TODO: renable roles initialization once we have a use case for
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
        if (username.equals(anonymousUserId)) {
            return makeAnonymousPrincipal();
        }
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            DocumentModel userEntry = userDir.getEntry(username);
            if (userEntry == null) {
                return null;
            }
            return makePrincipal(userEntry);
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    @Deprecated
    public DocumentModel getModelForUser(String name) throws ClientException {
        Session userDir = null;
        try {

            if (name == null) {
                return null;
            }

            // check if the user exists
            userDir = dirService.open(userDirectoryName);
            DocumentModel docModel = userDir.getEntry(name);

            if (docModel == null) {
                userDir.close();
                return null;
            }
            return docModel;
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public void updatePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        Session userDir = null;
        String principalName = principal.getName();
        try {
            userDir = dirService.open(userDirectoryName);

            // check if the user exists
            NuxeoPrincipal oldPrincipal = getPrincipal(principalName);
            if (oldPrincipal == null) {
                throw new DirectoryException("No entry found for username: "
                        + principalName);
            }

            userDir.updateEntry(principal.getModel());
            userDir.commit();

            // update the roles for the user (YAGNI?)
            List<String> newRoles = principal.getRoles();
            List<String> oldRoles = oldPrincipal.getRoles();
            List<String> addedRoles = new ArrayList<String>(newRoles);
            addedRoles.removeAll(oldRoles);
            List<String> removedRoles = new ArrayList<String>(oldRoles);
            removedRoles.removeAll(newRoles);

            for (String roleName : addedRoles) {
                addPrincipalToRole(principalName, roleName);
            }
            for (String roleName : removedRoles) {
                removePrincipalFromRole(principalName, roleName);
            }
            notifyUserChanged(principal);

        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public void updateGroup(NuxeoGroup group) throws ClientException {
        String groupName = group.getName();
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            DocumentModel groupEntry = groupDir.getEntry(groupName);
            // TODO: move hardcoded names to an extension point
            groupEntry.setProperty(groupSchemaName, groupMembersField,
                    group.getMemberUsers());
            groupEntry.setProperty(groupSchemaName, groupSubGroupsField,
                    group.getMemberGroups());
            groupDir.updateEntry(groupEntry);
            groupDir.commit();
            notifyGroupChanged(group);

        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public NuxeoGroup getGroup(String groupName) throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);

            DocumentModel groupEntry = groupDir.getEntry(groupName);
            if (groupEntry == null) {
                return null;
            }
            return getGroup(groupEntry);
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public NuxeoGroup getGroup(DocumentModel groupEntry) {
        NuxeoGroup group = new NuxeoGroupImpl(groupEntry.getId());
        List<String> list = (List<String>) groupEntry.getProperty(
                groupSchemaName, groupMembersField);
        if (list != null) {
            group.setMemberUsers(list);
        }
        list = (List<String>) groupEntry.getProperty(groupSchemaName,
                groupSubGroupsField);
        if (list != null) {
            group.setMemberGroups(list);
        }
        list = (List<String>) groupEntry.getProperty(groupSchemaName,
                groupParentGroupsField);
        if (list != null) {
            group.setParentGroups(list);
        }
        return group;
    }

    public void createGroup(NuxeoGroup group) throws ClientException {
        Session groupDir = null;
        try {
            String groupId = group.getName();
            groupDir = dirService.open(groupDirectoryName);

            // check the group does not exist
            NuxeoGroup oldGroup = getGroup(groupId);
            if (oldGroup != null) {
                throw new GroupAlreadyExistsException();
            }

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(groupDir.getIdField(), groupId);
            map.put(groupMembersField, group.getMemberUsers());
            map.put(groupSubGroupsField, group.getMemberGroups());
            groupDir.createEntry(map);

            groupDir.commit();
            notifyGroupChanged(group);

        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public void createPrincipal(NuxeoPrincipal principal)
            throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            String principalName = principal.getName();

            // check the user does not exist
            // TODO: use hasEntry
            DocumentModel user = userDir.getEntry(principalName);
            if (user != null) {
                throw new UserAlreadyExistsException();
            }

            // TODO transform the model into a map
            // XXX hack, principals have only one model
            DataModel dm = principal.getModel().getDataModels().values().iterator().next();
            userDir.createEntry(dm.getMap());

            for (String roleName : principal.getRoles()) {
                addPrincipalToRole(principalName, roleName);
            }
            userDir.commit();
            notifyUserChanged(principal);

        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public void deleteGroup(NuxeoGroup group) throws ClientException {
        Session groupDir = null;

        try {
            String groupId = group.getName();
            groupDir = dirService.open(groupDirectoryName);

            DocumentModel docModel = groupDir.getEntry(groupId);
            if (docModel == null) {
                throw new DirectoryException("group does not exist: " + groupId);
            }
            groupDir.deleteEntry(docModel);
            groupDir.commit();
            notifyGroupChanged(group);

        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    public void deletePrincipal(NuxeoPrincipal principal)
            throws ClientException {
        Session userDir = null;

        try {
            userDir = dirService.open(userDirectoryName);

            String userId = principal.getName();

            // remove user
            DocumentModel entry = userDir.getEntry(userId);
            if (entry == null) {
                throw new DirectoryException("principal does not exist: "
                        + userId);
            }
            userDir.deleteEntry(entry);
            userDir.commit();
            notifyUserChanged(principal);

        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public List<NuxeoGroup> getAvailableGroups() throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);

            DocumentModelList groupEntries = groupDir.getEntries();
            List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>(
                    groupEntries.size());
            for (DocumentModel entry : groupEntries) {
                groups.add(getGroup(entry.getId()));
            }
            sortGroups(groups);
            return groups;
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getTopLevelGroups() throws ClientException {
        Session groupDir = null;
        try {
            List<String> topLevelGroups = new LinkedList<String>();
            groupDir = dirService.open(groupDirectoryName);
            DocumentModelList groups = groupDir.getEntries();
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

    private void addPrincipalToRole(String principalName, String roleName)
            throws ClientException {
        // XXX: YAGNI
    }

    private void removePrincipalFromRole(String principalId, String roleId)
            throws ClientException {
        // XXX: YAGNI
    }

    // TODO: if this code has to be generalized, then it would be simpler
    // to use a MemoryDirectory to hold the anonymous user and just do
    // the normal query on it.
    /**
     * Specific matching for Anonymous, which is not in a directory. (Matches
     * using fulltext rules.)
     */
    protected boolean isAnonymousMatching(String pattern) {
        if (anonymousUserId == null) {
            return false;
        }
        pattern = pattern.toLowerCase();
        for (Object value : anonymousUserMap.values()) {
            if (value.toString().toLowerCase().startsWith(pattern)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isAnonymousMatching(Map<String, Object> filter,
            Set<String> fulltext) {
        if (anonymousUserId == null) {
            return false;
        }
        for (Entry<String, Object> e : filter.entrySet()) {
            String fieldName = e.getKey();
            Object expected = e.getValue();
            Object value = anonymousUserMap.get(fieldName);
            if (value == null) {
                if (expected != null) {
                    return false;
                }
            } else {
                if (fulltext.contains(fieldName)) {
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
        if (pattern == null) {
            throw new IllegalArgumentException("pattern cannot be null");
        }

        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            Map<String, DocumentModel> uniqueEntries = new HashMap<String, DocumentModel>();

            for (Map.Entry<String, MatchType> fieldEntry : userSearchFields.entrySet()) {
                Map<String, Object> filter = new HashMap<String, Object>();
                filter.put(fieldEntry.getKey(), pattern);
                DocumentModelList fetchedEntries;
                if (fieldEntry.getValue() == MatchType.SUBSTRING) {
                    fetchedEntries = userDir.query(filter, filter.keySet());
                } else {
                    fetchedEntries = userDir.query(filter);
                }
                for (DocumentModel entry : fetchedEntries) {
                    uniqueEntries.put(entry.getId(), entry);
                }
            }

            log.debug(String.format("found %d unique entries",
                    uniqueEntries.size()));

            List<NuxeoPrincipal> principals = new ArrayList<NuxeoPrincipal>(
                    uniqueEntries.size());
            for (DocumentModel entry : uniqueEntries.values()) {
                principals.add(makePrincipal(entry));
            }

            if (isAnonymousMatching(pattern)) {
                principals.add(makeAnonymousPrincipal());
            }

            sortPrincipals(principals, userDir.getIdField());
            return principals;
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    private void sortPrincipals(List<NuxeoPrincipal> principals,
            String defaultSortField) {
        String sortField = userSortField != null ? userSortField
                : defaultSortField;
        Collections.sort(principals, new PrincipalComparator(sortField));
    }

    /**
     * Comparator for two principals, uses case- and accent-insensitive
     * comparison on a specific field.
     *
     * @author Florent Guillaume
     */
    static class PrincipalComparator implements Comparator<NuxeoPrincipal>,
            Serializable {

        private static final long serialVersionUID = -8369584979443796622L;

        static final Collator collator = Collator.getInstance(); // use
        // locale?

        static {
            collator.setStrength(Collator.PRIMARY); // case+accent independent
        }

        private final String fieldName;

        PrincipalComparator(String fieldName) {
            this.fieldName = fieldName;
        }

        public int compare(NuxeoPrincipal p1, NuxeoPrincipal p2) {
            // XXX hack, principals have only one model
            DataModel m1 = p1.getModel().getDataModels().values().iterator().next();
            DataModel m2 = p1.getModel().getDataModels().values().iterator().next();
            String s1 = (String) m1.getData(fieldName);
            String s2 = (String) m2.getData(fieldName);
            if (s1 == null && s2 != null) {
                return -1;
            } else if (s1 != null && s2 == null) {
                return 1;
            } else if (s1 != null && s2 != null) {
                int cmp = collator.compare(s1, s2);
                if (cmp != 0) {
                    return cmp;
                }
            }
            // strings are equal, provide consistent ordering
            if (p1.hashCode() == p2.hashCode()) {
                return 0;
            } else if (p1.hashCode() < p2.hashCode()) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    protected static final Comparator<NuxeoGroup> groupComparator = new Comparator<NuxeoGroup>() {
        public int compare(NuxeoGroup g1, NuxeoGroup g2) {
            return g1.getName().compareTo(g2.getName());
        }
    };

    protected static void sortGroups(List<NuxeoGroup> groups) {
        Collections.sort(groups, groupComparator);
    }

    public List<NuxeoPrincipal> searchByMap(Map<String, Object> filter,
            Set<String> pattern) throws ClientException {

        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);

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

    public List<NuxeoGroup> searchGroups(String pattern) throws ClientException {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern cannot be null");
        }

        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);

            Map<String, Object> filter = new HashMap<String, Object>();
            filter.put(groupDir.getIdField(), pattern);
            DocumentModelList groupEntries = groupDir.query(filter,
                    filter.keySet());

            List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>(
                    groupEntries.size());
            for (DocumentModel groupEntry : groupEntries) {
                groups.add(getGroup(groupEntry));
            }
            sortGroups(groups);
            return groups;
        } finally {
            if (groupDir != null) {
                groupDir.close();
            }
        }
    }

    // XXX: this is potentially a performance killing method
    @Deprecated
    public List<NuxeoPrincipal> getAvailablePrincipals() throws ClientException {
        Session userDir = null;
        try {
            userDir = dirService.open(userDirectoryName);
            DocumentModelList entries = userDir.getEntries();

            List<NuxeoPrincipal> principalList = new ArrayList<NuxeoPrincipal>(
                    entries.size());
            for (DocumentModel entry : entries) {
                principalList.add(makePrincipal(entry));
            }
            sortPrincipals(principalList, userDir.getIdField());
            return principalList;
        } finally {
            if (userDir != null) {
                userDir.close();
            }
        }
    }

    public void setRootLogin(String defaultRootLogin) {
        this.defaultRootLogin = defaultRootLogin;
    }

    public void setUserSortField(String sortField) {
        userSortField = sortField;
    }

    public String getUserSortField() {
        return userSortField;
    }

    public void setGroupSortField(String sortField) {
    }

    /**
     * Notifies user has changed so that the JaasCacheFlusher listener can make
     * sure principals cache is reset.
     */
    private void notifyUserChanged(NuxeoPrincipal principal) {
        EventService eventService = (EventService) Framework.getRuntime().getComponent(
                EventService.NAME);
        eventService.sendEvent(new Event(USERMANAGER_TOPIC,
                USERCHANGED_EVENT_ID, this, principal.getName()));
    }

    /**
     * Notifies group has changed so that the JaasCacheFlusher listener can make
     * sure principals cache is reset.
     */
    private void notifyGroupChanged(NuxeoGroup group) {
        EventService eventService = (EventService) Framework.getRuntime().getComponent(
                EventService.NAME);
        eventService.sendEvent(new Event(USERMANAGER_TOPIC,
                GROUPCHANGED_EVENT_ID, this, group.getName()));
    }

    public Boolean areGroupsReadOnly() throws ClientException {
        Session groupDir = null;
        try {
            groupDir = dirService.open(groupDirectoryName);
            return groupDir.isReadOnly();
        } catch (DirectoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            // TODO Auto-generated catch block
            e.printStackTrace();
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

}
