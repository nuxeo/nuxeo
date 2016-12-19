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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.login.Authenticator;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * @author Anahide Tchertchian
 * @author Sun Seng David TAN <stan@nuxeo.com>
 * @author Benjamin Jalon <bjalon@nuxeo.com>
 */
public interface UserManager extends Authenticator, EventListener, Serializable {

    enum MatchType {
        EXACT, SUBSTRING
    }

    @Override
    boolean checkUsernamePassword(String username, String password);

    boolean validatePassword(String password);

    /**
     * Retrieves the principal with the given username or null if it does not exist.
     * <p>
     * Can build principals for anonymous and virtual users as well as for users defined in the users directory.
     */
    NuxeoPrincipal getPrincipal(String username);

    /**
     * Returns the nuxeo group with given name or null if it does not exist.
     */
    NuxeoGroup getGroup(String groupName);

    /**
     * @deprecated see {@link #searchUsers(String)}
     */
    @Deprecated
    List<NuxeoPrincipal> searchPrincipals(String pattern);

    /**
     * Search matching groups through their defined search fields
     *
     * @since 5.5
     */
    DocumentModelList searchGroups(String pattern);

    /**
     * Returns the list of all user ids.
     *
     * @since 5.2M4
     */
    List<String> getUserIds();

    /**
     * Creates user from given model.
     *
     * @since 5.2M4
     * @throws UserAlreadyExistsException
     */
    DocumentModel createUser(DocumentModel userModel) throws UserAlreadyExistsException;

    /**
     * Updates user represented by given model.
     *
     * @since 5.2M4
     */
    void updateUser(DocumentModel userModel);

    /**
     * Deletes user represented by given model.
     *
     * @since 5.2M4
     * @throws DirectoryException if given entry does not exist
     */
    void deleteUser(DocumentModel userModel);

    /**
     * Deletes user with given id.
     *
     * @since 5.2M4
     * @throws DirectoryException if given entry does not exist
     */
    void deleteUser(String userId);

    /**
     * Returns a bare user model.
     * <p>
     * Can be used for user creation/search screens.
     *
     * @since 5.2M4
     */
    DocumentModel getBareUserModel();

    /**
     * Returns the document model representing user with given id or null if it does not exist.
     *
     * @since 5.2M4
     */
    DocumentModel getUserModel(String userName);

    /**
     * Returns users matching given pattern
     * <p>
     * Pattern is used to fill a filter and fulltext map according to users search fields configuration. Search is
     * performed on each of these fields (OR).
     *
     * @since 5.2M4
     */
    DocumentModelList searchUsers(String pattern);

    /**
     * Returns users matching given criteria.
     *
     * @param filter filter with field names as keys
     * @param fulltext field names used for fulltext match
     * @since 5.2M4
     */
    DocumentModelList searchUsers(Map<String, Serializable> filter, Set<String> fulltext);

    String getUserListingMode();

    String getUserSortField();

    Pattern getUserPasswordPattern();

    /**
     * Returns the list of all groups ids.
     *
     * @since 5.2M4
     */
    List<String> getGroupIds();

    /**
     * Returns groups matching given criteria.
     *
     * @param filter filter with field names as keys
     * @param fulltext field names used for fulltext match
     * @since 5.2M4
     */
    DocumentModelList searchGroups(Map<String, Serializable> filter, Set<String> fulltext);

    /**
     * Creates a group from given model
     *
     * @return the created group model
     * @since 5.2M4
     * @throws GroupAlreadyExistsException
     */
    DocumentModel createGroup(DocumentModel groupModel) throws GroupAlreadyExistsException;

    /**
     * Updates group represented by given model.
     *
     * @since 5.2M4
     * @throws DirectoryException if given entry does not exist
     */
    void updateGroup(DocumentModel groupModel);

    /**
     * Deletes group represented by given model.
     *
     * @since 5.2M4
     * @throws DirectoryException if given entry does not exist
     */
    void deleteGroup(DocumentModel groupModel);

    /**
     * Deletes group with given id.
     *
     * @since 5.2M4
     * @throws DirectoryException if given entry does not exist
     */
    void deleteGroup(String groupId);

    /**
     * Returns a bare group model.
     * <p>
     * Can be used for group creation/search screens.
     *
     * @since 5.2M4
     */
    DocumentModel getBareGroupModel();

    /**
     * Return the group document model with this id or null if group does not exist.
     *
     * @param groupName the group identifier
     * @since 5.2M4
     */
    DocumentModel getGroupModel(String groupName);

    String getDefaultGroup();

    String getGroupListingMode();

    /**
     * Returns the list of groups that belong to this group.
     *
     * @param parentId the name of the parent group.
     */
    List<String> getGroupsInGroup(String parentId);

    /**
     * Returns the list of groups that are not members of other groups.
     */
    List<String> getTopLevelGroups();

    /**
     * Returns the list of users that belong to this group.
     *
     * @param groupId ID of the group
     */
    List<String> getUsersInGroup(String groupId);

    /**
     * Get users from a group and its subgroups.
     *
     * @param groupId ID of the group
     */
    List<String> getUsersInGroupAndSubGroups(String groupId);

    /**
     * Returns true is users referential is read only (ie : LDAP) -> can not add users -> can not delete users.
     */
    Boolean areGroupsReadOnly();

    /**
     * Returns true is groups referential is read only (ie : LDAP) -> can not add groups -> can not delete groups.
     */
    Boolean areUsersReadOnly();

    /**
     * Gets the user directory name.
     *
     * @return the user directory name.
     */
    String getUserDirectoryName();

    /**
     * Returns the user directory schema name.
     *
     * @since 5.2M4
     */
    String getUserSchemaName();

    /**
     * Returns the user directory id field.
     *
     * @since 5.2M4
     */
    String getUserIdField();

    /**
     * Gets the user email field.
     *
     * @return the user email field.
     */
    String getUserEmailField();

    /**
     * Gets the user search fields, the fields to use when a principal search is done.
     *
     * @return the search fields.
     */
    Set<String> getUserSearchFields();

    /**
     * Gets the group search fields.
     */
    Set<String> getGroupSearchFields();

    /**
     * Gets the group directory name.
     *
     * @return the group directory name.
     */
    String getGroupDirectoryName();

    /**
     * Returns the group directory schema name.
     *
     * @since 5.2M4
     */
    String getGroupSchemaName();

    /**
     * Returns the group directory id field.
     *
     * @since 5.2M4
     */
    String getGroupIdField();

    /**
     * Returns the group label field.
     *
     * @since 5.5
     */
    String getGroupLabelField();

    /**
     * Gets the group members field.
     *
     * @return the group members field.
     */
    String getGroupMembersField();

    /**
     * Gets the group sub-groups field.
     *
     * @return the sub-groups field.
     */
    String getGroupSubGroupsField();

    /**
     * Gets the group parent-groups field.
     *
     * @return the parent-groups field.
     */
    String getGroupParentGroupsField();

    /**
     * Gets the anonymous user id.
     *
     * @return the anonymous user id, or the default one if none is defined.
     */
    String getAnonymousUserId();

    /** Gets the Digest Auth directory. */
    String getDigestAuthDirectory();

    /** Gets the Digest Auth realm. */
    String getDigestAuthRealm();

    /**
     * Sets the given configuration on the service.
     *
     * @param descriptor the descriptor as parsed from xml, merged from the previous one if it exists.
     */
    void setConfiguration(UserManagerDescriptor descriptor);

    /**
     * Returns the list of administrators groups.
     *
     * @since 5.3 GA
     */
    List<String> getAdministratorsGroups();

    /**
     * For an ACP, get the list of user that has a permission. This method should be use with care as it can cause
     * performance issues while getting the list of users.
     *
     * @since 5.4.2
     * @param perm the permission
     * @param acp The access control policy of the document
     * @return the list of user ids
     */
    String[] getUsersForPermission(String perm, ACP acp);

}
