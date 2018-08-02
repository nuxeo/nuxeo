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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;

/**
 * @author Benjamin Jalon <bjalon@nuxeo.com>
 */
public interface MultiTenantUserManager extends Serializable {

    /**
     * Retrieves the principal with the given username or null if it does not exist into the given context document. The
     * context document must be contained into the tenant
     * <p>
     * Can build principals for anonymous and virtual users as well as for users defined in the users directory.
     *
     * @since 5.5
     */
    NuxeoPrincipal getPrincipal(String username, DocumentModel context);

    /**
     * Search matching groups through their defined search fields into the given context document. The context document
     * must be contained into the tenant.
     *
     * @since 5.5
     */
    DocumentModelList searchGroups(String pattern, DocumentModel context);

    /**
     * Returns the list of all user ids into the given context document. The context document must be contained into the
     * tenant.
     *
     * @since 5.5
     */
    List<String> getUserIds(DocumentModel context);

    /**
     * Creates user from given model into the given context document. The context document must be contained into the
     * tenant.
     *
     * @since 5.5
     * @throws UserAlreadyExistsException
     */
    DocumentModel createUser(DocumentModel userModel, DocumentModel context) throws
            UserAlreadyExistsException;

    /**
     * Updates user represented by given model into the given context document. The context document must be contained
     * into the tenant.
     *
     * @param userModel
     * @since 5.5
     */
    void updateUser(DocumentModel userModel, DocumentModel context);

    /**
     * Deletes user represented by given model into the given context document. The context document must be contained
     * into the tenant.
     *
     * @since 5.5
     */
    void deleteUser(DocumentModel userModel, DocumentModel context);

    /**
     * Deletes user with given id into the given context document. The context document must be contained into the
     * tenant.
     *
     * @since 5.5
     */
    void deleteUser(String userId, DocumentModel context);

    /**
     * Returns the document model representing user with given id or null if it does not exist into the given context
     * document. The context document must be contained into the tenant.
     *
     * @since 5.5
     */
    DocumentModel getUserModel(String userName, DocumentModel context);

    /**
     * Returns users matching given pattern with the given context. if the Document Context have a directory local
     * configuration, the service try to open the directory with directory suffix set into the local configuration
     * <p>
     * Pattern is used to fill a filter and fulltext map according to users search fields configuration. Search is
     * performed on each of these fields (OR).
     *
     * @since 5.5
     */
    DocumentModelList searchUsers(String pattern, DocumentModel context);

    /**
     * Returns users matching given criteria and with the given context. if the Document Context have a directory local
     * configuration, the service try to open the user directory with directory suffix set into the local configuration
     *
     * @param filter filter with field names as keys
     * @param fulltext field names used for fulltext match
     * @param context
     * @since 5.5
     */
    DocumentModelList searchUsers(Map<String, Serializable> filter, Set<String> fulltext, DocumentModel context);

    /**
     * Returns the list of all groups ids with the given context. if the Document Context have a directory local
     * configuration, the service try to open the user directory with directory suffix set into the local configuration
     *
     * @since 5.5
     */
    List<String> getGroupIds(DocumentModel context);

    /**
     * Returns groups matching given criteria with the given context. if the Document Context have a directory local
     * configuration, the service try to open the user directory with directory suffix set into the local configuration
     *
     * @param filter filter with field names as keys
     * @param fulltext field names used for fulltext match
     * @param context
     * @since 5.5
     */
    DocumentModelList searchGroups(Map<String, Serializable> filter, Set<String> fulltext, DocumentModel context);

    /**
     * Creates a group from given model with the given context. If the Document Context have a directory local
     * configuration, the service will append at the end of the groupname the directory suffix set into the local
     * configuration of the context document.
     *
     * @return the created group model
     * @since 5.5
     * @throws GroupAlreadyExistsException
     */
    DocumentModel createGroup(DocumentModel groupModel, DocumentModel context) throws
            GroupAlreadyExistsException;

    /**
     * Updates group represented by given model with the given context. If the Document Context have a directory local
     * configuration, the service will append at the end of the groupname the directory suffix set into the local
     * configuration of the context document.
     *
     * @since 5.5
     */
    void updateGroup(DocumentModel groupModel, DocumentModel context);

    /**
     * Deletes group represented by given model with the given context. If the Document Context have a directory local
     * configuration, the service will append at the end of the groupname the directory suffix set into the local
     * configuration of the context document.
     *
     * @param groupModel
     * @since 5.5
     */
    void deleteGroup(DocumentModel groupModel, DocumentModel context);

    /**
     * Deletes group with given id with the given context. If the Document Context have a directory local configuration,
     * the service will append at the end of the groupname the directory suffix set into the local configuration of the
     * context document.
     *
     * @param groupId
     * @since 5.5
     */
    void deleteGroup(String groupId, DocumentModel context);

    /**
     * Return the group document model with this id concatenated with the directory local config (if not null) or null
     * if group does not exist.
     *
     * @param groupName the group identifier
     * @since 5.5
     */
    DocumentModel getGroupModel(String groupName, DocumentModel context);

    /**
     * Returns the list of groups that belong to this group with the given context. If the Document Context have a
     * directory local configuration, the service will append at the end of the groupname the directory suffix set into
     * the local configuration of the context document.
     *
     * @param parentId the name of the parent group.
     * @return
     * @since 5.5
     */
    List<String> getGroupsInGroup(String parentId, DocumentModel context);

    /**
     * Returns the list of groups that are not members of other groups with the given context.
     *
     * @return
     * @since 5.5
     */
    List<String> getTopLevelGroups(DocumentModel context);

    /**
     * Returns the list of users that belong to this group into the given context
     *
     * @param groupId ID of the group
     * @return
     * @since 5.5
     */
    List<String> getUsersInGroup(String groupId, DocumentModel context);

    /**
     * Get users from a group and its subgroups into the given context
     *
     * @param groupId ID of the group
     * @return
     * @since 5.5
     */
    List<String> getUsersInGroupAndSubGroups(String groupId, DocumentModel context);

    /**
     * Returns true is users referential is read only (ie : LDAP) -> can not add users -> can not delete users.
     *
     * @since 5.5
     */
    Boolean areGroupsReadOnly();

    /**
     * Returns true is groups referential is read only (ie : LDAP) -> can not add groups -> can not delete groups.
     */
    Boolean areUsersReadOnly();

    /**
     * For an ACP, get the list of user that has a permission into the given context. This method should be use with
     * care as it can cause performance issues while getting the list of users.
     *
     * @since 5.5
     * @param perm the permission
     * @param acp The access control policy of the document
     * @return the list of user ids
     */
    String[] getUsersForPermission(String perm, ACP acp, DocumentModel context);

}
