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
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * User workspace manager actions business interface.
 *
 * @author btatar
 */
public interface UserWorkspaceManagerActions extends Serializable {

    /**
     * Gets the current user personal workspace.
     *
     * @return the personal workspace
     * @throws ClientException
     */
    DocumentModel getCurrentUserPersonalWorkspace() throws ClientException;

    /**
     * Navigates to the current user personal workspace.
     *
     * @return
     * @throws ClientException
     */
    String navigateToCurrentUserPersonalWorkspace() throws ClientException;

    /**
     * Navigates to the overall workspace. Introduced for INA-221 (Rux).
     *
     * @return
     * @throws ClientException
     */
    String navigateToOverallWorkspace() throws ClientException;

    /**
     * Checks wether a personal document is selected.
     *
     * @return true if it is a personal document
     *         <p>
     *         false otherwise
     */
    boolean isShowingPersonalWorkspace();

    void destroy();

    void initialize();

}
