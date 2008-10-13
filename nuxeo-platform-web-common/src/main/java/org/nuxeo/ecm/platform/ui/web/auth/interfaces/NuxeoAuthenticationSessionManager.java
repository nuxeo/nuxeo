/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;

/**
 * SessionManager interface for Authentication Filter.
 *
 * @author tiry
 */
public interface NuxeoAuthenticationSessionManager {

    /**
     * returns true if request does not require to be authenticated
     *
     * @param request
     * @return
     */
    boolean bypassRequest(ServletRequest request);

    /**
     * Used to know if SessionManager is applicable for a given request.
     */
    boolean isAvalaible(ServletRequest request);

    /**
     * Destroys web session and associated resources.
     */
    void invalidateSession(ServletRequest request);

    /**
     * Reinitializes a Session.
     */
    HttpSession reinitSession(ServletRequest request);



    /**
     *
     * CallBack for session creation
     *
     * @param request
     * @param session
     * @param cachebleUserInfo
     */
    void authenticatedSessionCreated(ServletRequest request, HttpSession session, CachableUserIdentificationInfo cachebleUserInfo);
}
