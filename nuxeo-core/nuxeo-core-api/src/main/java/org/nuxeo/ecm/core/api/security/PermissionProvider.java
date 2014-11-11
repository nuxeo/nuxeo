/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: PermissionProvider.java 28325 2007-12-24 08:29:26Z sfermigier $
 */

package org.nuxeo.ecm.core.api.security;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;

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
     * @param perm the name of a registered permissions that belongs to
     *            permission groups (aka compound permissions)
     * @return an array of a all compound permissions 'perm' is a sub-permission
     *         of, directly or not ; returns null if 'perm' is not registered or
     *         if 'perm' does not belong to any compound permission
     */
    String[] getPermissionGroups(String perm);

    /**
     * @return get the sorted list of UserVisiblePermission objects to be used in the
     *         permission management screen of the UI (be it web based, a rich
     *         client or any-thing else)
     * @throws ClientException
     */
    List<UserVisiblePermission> getUserVisiblePermissionDescriptors() throws ClientException;

    /**
     * @param typeName the name of a Core type of the document whose ACP is to
     *            be edited by the user
     * @return get the sorted list of UserVisiblePermission objects to be used in the
     *         permission management screen of the UI (be it web based, a rich
     *         client or any-thing else) ; if no specific permissions are
     *         registered for typeName, the default list is returned
     * @throws ClientException
     */
    List<UserVisiblePermission> getUserVisiblePermissionDescriptors(String typeName) throws ClientException;

    /**
     * @param perm the name of a registered compound permission
     * @return the list of permission names of sub-permissions of 'perm'
     * @throws ClientException if 'perm' is not a registered permission
     */
    String[] getSubPermissions(String perm) throws ClientException;

    /**
     * @param perm the name of a registered permission
     * @return the list of alias permissions to 'perm'
     * @throws ClientException if 'perm' is not a registered permission
     */
    String[] getAliasPermissions(String perm) throws ClientException;



}
