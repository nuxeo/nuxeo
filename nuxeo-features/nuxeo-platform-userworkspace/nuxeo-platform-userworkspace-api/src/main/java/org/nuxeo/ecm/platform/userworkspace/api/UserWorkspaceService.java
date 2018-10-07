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
 *     btatar
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.userworkspace.api;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * User workspace service class that is used to get the document model for the personal workspace of the current user.
 *
 * @author btatar
 */
public interface UserWorkspaceService extends Serializable {

    /**
     * Gets the current user personal workspace.
     * <p>
     * If this personal workspace does not exist then a new one will be created for the user owning the core session.
     *
     * @param userCoreSession the user core session
     * @return the user personal workspace
     * @since 9.3
     */
    DocumentModel getCurrentUserPersonalWorkspace(CoreSession userCoreSession);

    /**
     * Gets the current user personal workspace from a lower level.
     * <p>
     * If this personal workspace does not exist then a new one will be created for the user who is represented by first
     * argument.
     *
     * @param userName the current user
     * @param currentDocument the current document on which the user was on
     * @return the DocumentModel for the personal workspace of the current user
     */
    DocumentModel getCurrentUserPersonalWorkspace(String userName, DocumentModel currentDocument);

    /**
     * @deprecated since 9.3. User personal workspaces have always been stored in default domain. The context is
     *             useless. Simply use {@link #getCurrentUserPersonalWorkspace(CoreSession)}.
     */
    @Deprecated
    DocumentModel getCurrentUserPersonalWorkspace(CoreSession userCoreSession, DocumentModel context);

    /**
     * Gets a detached user workspace of a specified user.
     *
     * @param userName is the username of the wanted user's workspace owner
     * @param context is a document to determine the domain
     * @return the DocumentModel for the personal workspace
     * @since 5.5
     */
    DocumentModel getUserPersonalWorkspace(String userName, DocumentModel context);

    /**
     * Gets a detached user workspace of a specified user depending of the passed principal.
     *
     * @param principal of the wanted user's workspace owner
     * @param context is a document to determine the domain
     * @return the DocumentModel for the personal workspace
     * @since 5.7
     */
    DocumentModel getUserPersonalWorkspace(NuxeoPrincipal principal, DocumentModel context);

    /**
     * Checks whether the passed document is under the user's workspace (or is the workspace itself).
     *
     * @param principal the user
     * @param username the username, if principal is not available
     * @param doc the document
     * @return {@code true} if the document is under the user's workspace
     * @since 9.2
     */
    boolean isUnderUserWorkspace(NuxeoPrincipal principal, String username, DocumentModel doc);

    /**
     * Invalidates the user workspace service and force re-computation of user workspace root location.
     *
     * @since 9.3
     */
    void invalidate();

}
