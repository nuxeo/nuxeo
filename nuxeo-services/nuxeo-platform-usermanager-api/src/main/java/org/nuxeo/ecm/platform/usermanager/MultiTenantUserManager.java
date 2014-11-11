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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;

/**
 * @author Benjamin Jalon <bjalon@nuxeo.com>
 * 
 */
public interface MultiTenantUserManager extends Serializable {

    /**
     * Retrieves the principal with the given username or null if it does not
     * exist into the given context document. The context document must be
     * contained into the tenant
     * <p>
     * Can build principals for anonymous and virtual users as well as for users
     * defined in the users directory.
     * 
     * @throws ClientException
     * @since 5.5
     */
    NuxeoPrincipal getPrincipal(String username, DocumentModel context)
            throws ClientException;

    /**
     * Search matching groups through their defined search fields into the given
     * context document. The context document must be contained into the tenant.
     * 
     * @since 5.5
     */
    DocumentModelList searchGroups(String pattern, DocumentModel context)
            throws ClientException;

    /**
     * Returns the list of all user ids into the given context document. The
     * context document must be contained into the tenant.
     * 
     * @since 5.5
     * @throws ClientException
     */
    List<String> getUserIds(DocumentModel context) throws ClientException;

    /**
     * Creates user from given model into the given context document. The
     * context document must be contained into the tenant.
     * 
     * @since 5.5
     * @throws ClientException
     * @throws UserAlreadyExistsException
     */
    DocumentModel createUser(DocumentModel userModel, DocumentModel context)
            throws ClientException, UserAlreadyExistsException;

    /**
     * Updates user represented by given model into the given context document.
     * The context document must be contained into the tenant.
     * 
     * @param userModel
     * @since 5.5
     * @throws ClientException
     */
    void updateUser(DocumentModel userModel, DocumentModel context)
            throws ClientException;

    /**
     * Deletes user represented by given model into the given context document.
     * The context document must be contained into the tenant.
     * 
     * @since 5.5
     * @throws DirectoryException if given entry does not exist
     * @throws ClientException
     */
    void deleteUser(DocumentModel userModel, DocumentModel context)
            throws ClientException;

    /**
     * Deletes user with given id into the given context document. The context
     * document must be contained into the tenant.
     * 
     * @since 5.5
     * @throws DirectoryException if given entry does not exist
     * @throws ClientException
     */
    void deleteUser(String userId, DocumentModel context)
            throws ClientException;

    /**
     * Returns the document model representing user with given id or null if it
     * does not exist into the given context document. The context document must
     * be contained into the tenant.
     * 
     * @since 5.5
     * @throws ClientException
     */
    DocumentModel getUserModel(String userName, DocumentModel context)
            throws ClientException;

    /**
     * Returns users matching given pattern with the given context. if the
     * Document Context have a directory local configuration, the service try to
     * open the directory with directory suffix set into the local configuration
     * <p>
     * Pattern is used to fill a filter and fulltext map according to users
     * search fields configuration. Search is performed on each of these fields
     * (OR).
     * 
     * @since 5.5
     * @throws ClientException
     */
    DocumentModelList searchUsers(String pattern, DocumentModel context)
            throws ClientException;

    /**
     * Returns users matching given criteria and with the given context. if the
     * Document Context have a directory local configuration, the service try to
     * open the user directory with directory suffix set into the local
     * configuration
     * 
     * @param filter filter with field names as keys
     * @param fulltext field names used for fulltext match
     * @param context
     * @since 5.5
     * @throws ClientException
     */
    DocumentModelList searchUsers(Map<String, Serializable> filter,
            Set<String> fulltext, DocumentModel context) throws ClientException;

    /**
     * Returns the list of all groups ids with the given context. if the
     * Document Context have a directory local configuration, the service try to
     * open the user directory with directory suffix set into the local
     * configuration
     * 
     * @since 5.5
     * @throws ClientException
     */
    List<String> getGroupIds(DocumentModel context)
            throws ClientException;

    /**
     * Returns groups matching given criteria with the given context. if the
     * Document Context have a directory local configuration, the service try to
     * open the user directory with directory suffix set into the local
     * configuration
     * 
     * @param filter filter with field names as keys
     * @param fulltext field names used for fulltext match
     * @param context
     * @since 5.5
     * @throws ClientException
     */
    DocumentModelList searchGroups(Map<String, Serializable> filter,
            Set<String> fulltext, DocumentModel context) throws ClientException;

    /**
     * Creates a group from given model with the given context. If the Document
     * Context have a directory local configuration, the service will append at
     * the end of the groupname the directory suffix set into the local
     * configuration of the context document.
     * 
     * @return the created group model
     * @since 5.5
     * @throws ClientException
     * @throws GroupAlreadyExistsException
     */
    DocumentModel createGroup(DocumentModel groupModel, DocumentModel context)
            throws ClientException, GroupAlreadyExistsException;

    /**
     * Updates group represented by given model with the given context. If the
     * Document Context have a directory local configuration, the service will
     * append at the end of the groupname the directory suffix set into the
     * local configuration of the context document.
     * 
     * @since 5.5
     * @throws DirectoryException if given entry does not exist
     * @throws ClientException
     */
    void updateGroup(DocumentModel groupModel, DocumentModel context)
            throws ClientException;

    /**
     * Deletes group represented by given model with the given context. If the
     * Document Context have a directory local configuration, the service will
     * append at the end of the groupname the directory suffix set into the
     * local configuration of the context document.
     * 
     * @param groupModel
     * @since 5.5
     * @throws DirectoryException if given entry does not exist
     * @throws ClientException
     */
    void deleteGroup(DocumentModel groupModel, DocumentModel context)
            throws ClientException;

    /**
     * Deletes group with given id with the given context. If the Document
     * Context have a directory local configuration, the service will append at
     * the end of the groupname the directory suffix set into the local
     * configuration of the context document.
     * 
     * @param groupId
     * @since 5.5
     * @throws DirectoryException if given entry does not exist
     * @throws ClientException
     */
    void deleteGroup(String groupId, DocumentModel context)
            throws ClientException;

    /**
     * Return the group document model with this id concatenated with the
     * directory local config (if not null) or null if group does not exist.
     * 
     * @param groupName the group identifier
     * @since 5.5
     * @throws ClientException
     */
    DocumentModel getGroupModel(String groupName, DocumentModel context)
            throws ClientException;

    /**
     * Returns the list of groups that belong to this group with the given
     * context. If the Document Context have a directory local configuration,
     * the service will append at the end of the groupname the directory suffix
     * set into the local configuration of the context document.
     * 
     * @param parentId the name of the parent group.
     * @return
     * @throws ClientException
     * @since 5.5
     */
    List<String> getGroupsInGroup(String parentId, DocumentModel context)
            throws ClientException;

    /**
     * Returns the list of groups that are not members of other groups with the
     * given context.
     * 
     * @return
     * @throws ClientException
     * @since 5.5
     */
    List<String> getTopLevelGroups(DocumentModel context)
            throws ClientException;

    /**
     * Returns the list of users that belong to this group into the given
     * context
     * 
     * @param groupId ID of the group
     * @return
     * @since 5.5
     */
    List<String> getUsersInGroup(String groupId, DocumentModel context)
            throws ClientException;

    /**
     * Get users from a group and its subgroups into the given context
     * 
     * @param groupId ID of the group
     * @return
     * @since 5.5
     */
    List<String> getUsersInGroupAndSubGroups(String groupId,
            DocumentModel context) throws ClientException;

    /**
     * Returns true is users referential is read only (ie : LDAP) -> can not add
     * users -> can not delete users.
     * 
     * @since 5.5
     */
    Boolean areGroupsReadOnly() throws ClientException;

    /**
     * Returns true is groups referential is read only (ie : LDAP) -> can not
     * add groups -> can not delete groups.
     */
    Boolean areUsersReadOnly() throws ClientException;

    /**
     * For an ACP, get the list of user that has a permission into the given
     * context. This method should be use with care as it can cause performance
     * issues while getting the list of users.
     * 
     * @since 5.5
     * @param perm the permission
     * @param acp The access control policy of the document
     * @return the list of user ids
     */
    String[] getUsersForPermission(String perm, ACP acp, DocumentModel context);

}
