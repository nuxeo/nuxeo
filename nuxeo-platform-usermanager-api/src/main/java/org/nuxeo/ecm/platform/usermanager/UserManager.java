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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

public interface UserManager {

    boolean checkUsernamePassword(String username, String password)
            throws ClientException;

    boolean validatePassword(String password) throws ClientException;

    List<NuxeoPrincipal> getAvailablePrincipals() throws ClientException;

    /**
     * Retrieves the principal with the given username.
     *
     * @param username the username.
     * @return the principal, or null if doesn't exist.
     * @throws ClientException
     */
    NuxeoPrincipal getPrincipal(String username) throws ClientException;

    void createPrincipal(NuxeoPrincipal principal) throws ClientException;

    void updatePrincipal(NuxeoPrincipal principal) throws ClientException;

    void deletePrincipal(NuxeoPrincipal principal) throws ClientException;

    List<NuxeoPrincipal> searchPrincipals(String name) throws ClientException;

    List<NuxeoPrincipal> searchByMap(Map<String, Object> filter,
            Set<String> pattern) throws ClientException;

    List<NuxeoGroup> searchGroups(String pattern) throws ClientException;

    List<NuxeoGroup> getAvailableGroups() throws ClientException;

    NuxeoGroup getGroup(String groupName) throws ClientException;

    void createGroup(NuxeoGroup group) throws ClientException;

    void deleteGroup(NuxeoGroup group) throws ClientException;

    void updateGroup(NuxeoGroup group) throws ClientException;

    void remove() throws ClientException;

    String getDefaultGroup();

    void setDefaultGroup(String defaultGroup);

    void setRootLogin(String defaultRootLogin) throws ClientException;

    void setUserSortField(String sortField) throws ClientException;

    String getUserListingMode() throws ClientException;

    void setUserListingMode(String userListingMode) throws ClientException;

    String getUserSortField() throws ClientException;

    Pattern getUserPasswordPattern() throws ClientException;

    void setUserPasswordPattern(Pattern userPasswordPattern) throws ClientException;

    String getGroupListingMode() throws ClientException;

    void setGroupListingMode(String groupListingMode) throws ClientException;

    void setGroupSortField(String sortField) throws ClientException;

    DocumentModel getModelForUser(String name) throws ClientException;

    /**
     * Returns the list of groups that belong to this group.
     *
     * @param parentId the name of the parent group.
     * @return
     * @throws ClientException
     */
    List<String> getGroupsInGroup(String parentId) throws ClientException;

    /**
     * Returns the list of groups that are not members of other groups.
     *
     * @return
     * @throws ClientException
     */
    List<String> getTopLevelGroups() throws ClientException;

    /**
     * Returns the list of users that belong to this group.
     *
     * @param groupId ID of the group
     * @return
     */
    List<String> getUsersInGroup(String groupId) throws ClientException;

    /**
     * Returns true is users referential is read only (ie : LDAP) -> can not add
     * users -> can not delete users.
     *
     * @return
     */
    Boolean areGroupsReadOnly() throws ClientException;

    /**
     * Returns true is groups referential is read only (ie : LDAP) -> can not
     * add groups -> can not delete groups.
     *
     * @return
     * @throws ClientException
     */
    Boolean areUsersReadOnly() throws ClientException;

    /**
     * Sets the user directory name.
     *
     * @param userDirectoryName the user directory name.
     * @throws ClientException 
     */
    void setUserDirectoryName(String userDirectoryName) throws ClientException;

    /**
     * Gets the user directory name.
     *
     * @return the user directory name.
     * @throws ClientException 
     */
    String getUserDirectoryName() throws ClientException;

    /**
     * Sets the user email field.
     *
     * @param userEmailField the email field.
     * @throws ClientException 
     */
    void setUserEmailField(String userEmailField) throws ClientException;

    /**
     * Gets the user email field.
     *
     * @return the user email field.
     * @throws ClientException 
     */
    String getUserEmailField() throws ClientException;

    /**
     * Sets the user search fields, the fields to use when a fulltext search is
     * done.
     *
     * @param userSearchFields the search fields.
     * @throws ClientException 
     */
    void setUserSearchFields(Set<String> userSearchFields) throws ClientException;

    /**
     * Gets the user search fields, the fields to use when a fulltext search is
     * done.
     *
     * @return the search fields.
     * @throws ClientException 
     */
    Set<String> getUserSearchFields() throws ClientException;

    /**
     * Sets the group directory name.
     *
     * @param groupDirectoryName the user directory name.
     * @throws ClientException 
     */
    void setGroupDirectoryName(String groupDirectoryName) throws ClientException;

    /**
     * Gets the group directory name.
     *
     * @return the group directory name.
     * @throws ClientException 
     */
    String getGroupDirectoryName() throws ClientException;

    /**
     * Sets the group members field.
     *
     * @param groupMembersField the members field.
     * @throws ClientException 
     */
    void setGroupMembersField(String groupMembersField) throws ClientException;

    /**
     * Gets the group members field.
     *
     * @return the group members field.
     * @throws ClientException 
     */
    String getGroupMembersField() throws ClientException;

    /**
     * Sets the group sub-groups field.
     *
     * @param groupSubGroupsField the sub-groups field.
     * @throws ClientException 
     */
    void setGroupSubGroupsField(String groupSubGroupsField) throws ClientException;

    /**
     * Gets the group sub-groups field.
     *
     * @return the sub-groups field.
     * @throws ClientException 
     */
    String getGroupSubGroupsField() throws ClientException;

    /**
     * Sets the group parentGroups field.
     *
     * @param groupParentGroupsField the parentGroups field.
     * @throws ClientException 
     */
    void setGroupParentGroupsField(String groupParentGroupsField) throws ClientException;

    /**
     * Gets the group parent-groups field.
     *
     * @return the parent-groups field.
     * @throws ClientException 
     */
    String getGroupParentGroupsField() throws ClientException;

    /**
     * Sets the anonymous user properties.
     *
     * @param anonymousUser the anonymous user properties.
     * @throws ClientException 
     */
    void setAnonymousUser(Map<String, String> anonymousUser) throws ClientException;

    /**
     * Gets the anonymous user id.
     *
     * @return the anonymous user id, or null if none is defined.
     * @throws ClientException 
     */
    String getAnonymousUserId() throws ClientException;

    /**
     * The key in the anonymous user map that hold its id.
     */
    String ANONYMOUS_USER_ID_KEY = "__id__";
}
