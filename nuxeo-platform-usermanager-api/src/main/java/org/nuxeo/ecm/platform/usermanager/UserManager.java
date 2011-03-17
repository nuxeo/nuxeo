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
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.login.Authenticator;

/**
 * @author Anahide Tchertchian
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public interface UserManager extends Authenticator, Serializable {

    enum MatchType {
        EXACT, SUBSTRING
    }

    boolean checkUsernamePassword(String username, String password)
            throws ClientException;

    boolean validatePassword(String password) throws ClientException;

    /**
     * Retrieves the principal with the given username or null if it does not
     * exist.
     * <p>
     * Can build principals for anonymous and virtual users as well as for users
     * defined in the users directory.
     *
     * @throws ClientException
     */
    NuxeoPrincipal getPrincipal(String username) throws ClientException;

    /**
     * Returns the nuxeo group with given name or null if it does not exist.
     *
     * @throws ClientException
     */
    NuxeoGroup getGroup(String groupName) throws ClientException;

    /**
     * @deprecated see {@link #searchUsers(String)}
     */
    @Deprecated
    List<NuxeoPrincipal> searchPrincipals(String pattern)
            throws ClientException;

    /**
     * @deprecated see {@link #searchGroups(String)}
     */
    @Deprecated
    List<NuxeoGroup> searchGroups(String pattern) throws ClientException;

    /**
     * Returns the list of all user ids.
     *
     * @since 5.2M4
     * @throws ClientException
     */
    List<String> getUserIds() throws ClientException;

    /**
     * Creates user from given model.
     *
     * @since 5.2M4
     * @throws ClientException
     * @throws UserAlreadyExistsException
     */
    DocumentModel createUser(DocumentModel userModel) throws ClientException,
            UserAlreadyExistsException;

    /**
     * Updates user represented by given model.
     *
     * @param userModel
     * @since 5.2M4
     * @throws ClientException
     */
    void updateUser(DocumentModel userModel) throws ClientException;

    /**
     * Deletes user represented by given model.
     *
     * @since 5.2M4
     * @throws DirectoryException if given entry does not exist
     * @throws ClientException
     */
    void deleteUser(DocumentModel userModel) throws ClientException;

    /**
     * Deletes user with given id.
     *
     * @since 5.2M4
     * @throws DirectoryException if given entry does not exist
     * @throws ClientException
     */
    void deleteUser(String userId) throws ClientException;

    /**
     * Returns a bare user model.
     * <p>
     * Can be used for user creation/search screens.
     *
     * @since 5.2M4
     * @throws ClientException
     */
    DocumentModel getBareUserModel() throws ClientException;

    /**
     * Returns the document model representing user with given id or null if it
     * does not exist.
     *
     * @since 5.2M4
     * @throws ClientException
     */
    DocumentModel getUserModel(String userName) throws ClientException;

    /**
     * Returns users matching given pattern
     * <p>
     * Pattern is used to fill a filter and fulltext map according to users
     * search fields configuration. Search is performed on each of these fields
     * (OR).
     *
     * @since 5.2M4
     * @throws ClientException
     */
    DocumentModelList searchUsers(String pattern) throws ClientException;

    /**
     * Returns users matching given criteria.
     *
     * @param filter filter with field names as keys
     * @param fulltext field names used for fulltext match
     * @since 5.2M4
     * @throws ClientException
     */
    DocumentModelList searchUsers(Map<String, Serializable> filter,
            Set<String> fulltext) throws ClientException;

    String getUserListingMode() throws ClientException;

    String getUserSortField() throws ClientException;

    Pattern getUserPasswordPattern() throws ClientException;

    /**
     * Returns the list of all groups ids.
     *
     * @since 5.2M4
     * @throws ClientException
     */
    List<String> getGroupIds() throws ClientException;

    /**
     * Returns groups matching given criteria.
     *
     * @param filter filter with field names as keys
     * @param fulltext field names used for fulltext match
     * @since 5.2M4
     * @throws ClientException
     */
    DocumentModelList searchGroups(Map<String, Serializable> filter,
            HashSet<String> fulltext) throws ClientException;

    /**
     * Creates a group from given model.
     *
     * @return the created group model
     * @since 5.2M4
     * @throws ClientException
     * @throws GroupAlreadyExistsException
     */
    DocumentModel createGroup(DocumentModel groupModel) throws ClientException,
            GroupAlreadyExistsException;

    /**
     * Updates group represented by given model.
     *
     * @since 5.2M4
     * @throws DirectoryException if given entry does not exist
     * @throws ClientException
     */
    void updateGroup(DocumentModel groupModel) throws ClientException;

    /**
     * Deletes group represented by given model.
     *
     * @param groupModel
     * @since 5.2M4
     * @throws DirectoryException if given entry does not exist
     * @throws ClientException
     */
    void deleteGroup(DocumentModel groupModel) throws ClientException;

    /**
     * Deletes group with given id.
     *
     * @param groupId
     * @since 5.2M4
     * @throws DirectoryException if given entry does not exist
     * @throws ClientException
     */
    void deleteGroup(String groupId) throws ClientException;

    /**
     * Returns a bare group model.
     * <p>
     * Can be used for group creation/search screens.
     *
     * @since 5.2M4
     * @throws ClientException
     */
    DocumentModel getBareGroupModel() throws ClientException;

    /**
     * Return the group document model with this id or null if group does not
     * exist.
     *
     * @param groupName the group identifier
     * @since 5.2M4
     * @throws ClientException
     */
    DocumentModel getGroupModel(String groupName) throws ClientException;

    String getDefaultGroup();

    String getGroupListingMode() throws ClientException;

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
     * Get users from a group and its subgroups.
     *
     * @param groupId ID of the group
     * @return
     */
    List<String> getUsersInGroupAndSubGroups(String groupId)
            throws ClientException;

    /**
     * Returns true is users referential is read only (ie : LDAP) -> can not add
     * users -> can not delete users.
     */
    Boolean areGroupsReadOnly() throws ClientException;

    /**
     * Returns true is groups referential is read only (ie : LDAP) -> can not
     * add groups -> can not delete groups.
     */
    Boolean areUsersReadOnly() throws ClientException;

    /**
     * Gets the user directory name.
     *
     * @return the user directory name.
     * @throws ClientException
     */
    String getUserDirectoryName() throws ClientException;

    /**
     * Returns the user directory schema name.
     *
     * @since 5.2M4
     * @throws ClientException
     */
    String getUserSchemaName() throws ClientException;

    /**
     * Returns the user directory id field.
     *
     * @since 5.2M4
     * @throws ClientException
     */
    String getUserIdField() throws ClientException;

    /**
     * Gets the user email field.
     *
     * @return the user email field.
     * @throws ClientException
     */
    String getUserEmailField() throws ClientException;

    /**
     * Gets the user search fields, the fields to use when a principal search is
     * done.
     *
     * @return the search fields.
     * @throws ClientException
     */
    Set<String> getUserSearchFields() throws ClientException;

    /**
     * Gets the group directory name.
     *
     * @return the group directory name.
     * @throws ClientException
     */
    String getGroupDirectoryName() throws ClientException;

    /**
     * Returns the group directory schema name.
     *
     * @since 5.2M4
     * @throws ClientException
     */
    String getGroupSchemaName() throws ClientException;

    /**
     * Returns the group directory id field.
     *
     * @since 5.2M4
     * @throws ClientException
     */
    String getGroupIdField() throws ClientException;

    /**
     * Gets the group members field.
     *
     * @return the group members field.
     * @throws ClientException
     */
    String getGroupMembersField() throws ClientException;

    /**
     * Gets the group sub-groups field.
     *
     * @return the sub-groups field.
     * @throws ClientException
     */
    String getGroupSubGroupsField() throws ClientException;

    /**
     * Gets the group parent-groups field.
     *
     * @return the parent-groups field.
     * @throws ClientException
     */
    String getGroupParentGroupsField() throws ClientException;

    /**
     * Gets the anonymous user id.
     *
     * @return the anonymous user id, or the default one if none is defined.
     * @throws ClientException
     */
    String getAnonymousUserId() throws ClientException;

    /** Gets the Digest Auth directory. */
    String getDigestAuthDirectory() throws ClientException;

    /** Gets the Digest Auth realm. */
    String getDigestAuthRealm() throws ClientException;

    /**
     * Sets the given configuration on the service.
     *
     * @param descriptor the descriptor as parsed from xml, merged from the
     *            previous one if it exists.
     */
    void setConfiguration(UserManagerDescriptor descriptor)
            throws ClientException;

    /**
     * Returns the list of administrators groups.
     *
     * @since 5.3 GA
     */
    List<String> getAdministratorsGroups();

    // DEPRECATED API

    /**
     * @deprecated use {@link #getUserModel(String)}
     */
    @Deprecated
    DocumentModel getModelForUser(String name) throws ClientException;

    /**
     * @deprecated use {@link #getUserIds()} or {@link #searchUsers(null)}
     */
    @Deprecated
    List<NuxeoPrincipal> getAvailablePrincipals() throws ClientException;

    /**
     * @deprecated use {@link #createUser(DocumentModel)}
     */
    @Deprecated
    void createPrincipal(NuxeoPrincipal principal) throws ClientException;

    /**
     * @deprecated use {@link #updateUser(DocumentModel)}
     */
    @Deprecated
    void updatePrincipal(NuxeoPrincipal principal) throws ClientException;

    /**
     * @deprecated use {@link #deleteUser(DocumentModel)}
     */
    @Deprecated
    void deletePrincipal(NuxeoPrincipal principal) throws ClientException;

    /**
     * @deprecated use {@link #searchUsers(Map, Set)}
     */
    @Deprecated
    List<NuxeoPrincipal> searchByMap(Map<String, Serializable> filter,
            Set<String> pattern) throws ClientException;

    /**
     * @deprecated use {@link #getGroupIds()} or {@link #searchGroups(Map, Set)}
     */
    @Deprecated
    List<NuxeoGroup> getAvailableGroups() throws ClientException;

    /**
     * @deprecated use {@link #createGroup(DocumentModel)}
     */
    @Deprecated
    void createGroup(NuxeoGroup group) throws ClientException;

    /**
     * @deprecated use {@link #deleteGroup(DocumentModel)}
     */
    @Deprecated
    void deleteGroup(NuxeoGroup group) throws ClientException;

    /**
     * @deprecated use {@link #updateGroup(DocumentModel)}
     */
    @Deprecated
    void updateGroup(NuxeoGroup group) throws ClientException;

    /**
     * For an ACP, get the list of user that has a permission. This method
     * should be use with care as it can cause performance issues while getting
     * the list of users.
     *
     * @since 5.4.2
     * @param perm the permission
     * @param acp The access control policy of the document
     * @return the list of user ids
     */
    String[] getUsersForPermission(String perm, ACP acp);

}
