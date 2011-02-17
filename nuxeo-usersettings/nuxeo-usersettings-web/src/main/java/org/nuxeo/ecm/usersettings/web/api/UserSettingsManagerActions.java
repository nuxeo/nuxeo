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

package org.nuxeo.ecm.usersettings.web.api;

import java.io.Serializable;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * User preferences actions business interface.
 * 
 * @author btatar
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 * 
 */
public interface UserSettingsManagerActions extends Serializable {

    /**
     * Gets the current user personal workspace.
     * 
     * @return the personal workspace
     * @throws ClientException
     * @throws Exception
     */
    DocumentModel getCurrentUserSettings() throws Exception;

    /**
     * Navigates to the current user personal workspace.
     * 
     * @return
     * @throws ClientException
     */
    String navigateToCurrentUserSettings() throws Exception;

    void destroy();

    void initialize();

}
