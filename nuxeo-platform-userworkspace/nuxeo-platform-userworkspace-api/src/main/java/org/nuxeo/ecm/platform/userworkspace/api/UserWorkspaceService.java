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
 *     btatar
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.userworkspace.api;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * User workspace service class that is used to get the document model for the
 * personal workspace of the current user.
 *
 * @author btatar
 *
 */
public interface UserWorkspaceService extends Serializable {

    /**
     * Gets the current user personal workspace from a lower level.
     * <p>
     * If this personal workspace does not exist then a new one will be created
     * for the user who is represented by first argument.
     *
     * @param userName the current user
     * @param currentDocument the current document on which the user was on
     * @return the DocumentModel for the personal workspace of the current user
     * @throws ClientException
     */
    DocumentModel getCurrentUserPersonalWorkspace(String userName,
            DocumentModel currentDocument) throws ClientException;


    DocumentModel getCurrentUserPersonalWorkspace(CoreSession userCoreSession, DocumentModel context)
            throws ClientException;

}
