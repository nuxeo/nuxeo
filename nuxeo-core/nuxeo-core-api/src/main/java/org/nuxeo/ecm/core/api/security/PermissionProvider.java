/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: PermissionProvider.java 28325 2007-12-24 08:29:26Z sfermigier $
 */

package org.nuxeo.ecm.core.api.security;

import java.util.List;


/**
 * Provider for existing permission and permission groups.
 *
 * @author Bogdan Stefanescu
 * @author Olivier Grisel
 */
public interface PermissionProvider {

    /**
     * @return an array of a all registered permission names
     */
    String[] getPermissions();

    /**
     * @param perm the name of a registered permissions that belongs to permission groups (aka compound permissions)
     * @return an array of a all compound permissions 'perm' is a sub-permission of, directly or not ; returns null if
     *         'perm' is not registered or if 'perm' does not belong to any compound permission
     */
    String[] getPermissionGroups(String perm);

    /**
     * @return get the sorted list of UserVisiblePermission objects to be used in the permission management screen of
     *         the UI (be it web based, a rich client or any-thing else)
     */
    List<UserVisiblePermission> getUserVisiblePermissionDescriptors();

    /**
     * @param typeName the name of a Core type of the document whose ACP is to be edited by the user
     * @return get the sorted list of UserVisiblePermission objects to be used in the permission management screen of
     *         the UI (be it web based, a rich client or any-thing else) ; if no specific permissions are registered for
     *         typeName, the default list is returned
     */
    List<UserVisiblePermission> getUserVisiblePermissionDescriptors(String typeName);

    /**
     * @param perm the name of a registered compound permission
     * @return the list of permission names of sub-permissions of 'perm'
     */
    String[] getSubPermissions(String perm);

    /**
     * @param perm the name of a registered permission
     * @return the list of alias permissions to 'perm'
     */
    String[] getAliasPermissions(String perm);

}
