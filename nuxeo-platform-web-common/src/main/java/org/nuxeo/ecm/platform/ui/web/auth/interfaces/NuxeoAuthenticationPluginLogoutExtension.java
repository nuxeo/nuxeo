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

package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface NuxeoAuthenticationPluginLogoutExtension {

    /**
     * Handles logout operation.
     * <p>
     * Generic logout (killing session and Seam objects) is done by LogoutActionBean
     * This interface must be implemented by auth plugin when the target auth system
     * needs a specific logout procedure.
     *
     * @return true if caller must stop execution (ie: logout generated a redirect),
     *      false otherwise
     */
    Boolean handleLogout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

}
