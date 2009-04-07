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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;

public interface NuxeoAuthenticationPlugin {

    /**
     * Handles the Login Prompt.
     *
     * @param httpRequest the request
     * @param httpResponse the response
     * @return true if AuthFilter must stop execution (ie: login prompt generated a redirect),
     *      false otherwise
     */
    Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL);

    /**
     * Retrieves user identification information from the request.
     *
     * @param httpRequest
     * @param httpResponse
     * @return UserIdentificationInfo
     */
    UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * Defines if the authentication plugin needs to do a login prompt.
     *
     * @return true if LoginPrompt is used
     */
    Boolean needLoginPrompt(HttpServletRequest httpRequest);


    /**
     * Initializes the Plugin from parameters set in the XML descriptor.
     *
     * @param parameters
     */
    void initPlugin(Map<String, String> parameters);

    /**
     * Returns the list of prefix for unauthenticated URLs,
     * typically the URLs associated to login prompt.
     *
     *  @return
     */
    List<String> getUnAuthenticatedURLPrefix();

}
