/* (C) Copyright 2002-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.auth.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

/**
 * The Web Service Servlet needs no login prompt and / or authentiocation.
 * <p>
 * I see 2 different scenarios:
 * <ol>
 * <li>The client application is a standalone application. It connects to WS with the
 * real credentials and keeps a session only for WS. It has nothing to do with the Web
 * Application or whatsoever. Initially client comes to MainEntrancePoint and tries to
 * get a Stateful WebService (actual WS perfoming the job). NuxeoAuthenticationFilter
 * (NAF) finds no authentication data in message. It has to let the request pass and not
 * forward the request to login page. The WS makes the authentication based on user
 * credentials.
 * <li>The client application reuses a Web Session or uses another mechanism to hold a
 * HTTP Session (the SSO case). Client comes to MainEntrancePoint and tries to gets a
 * Stateful WebService (actual WS perfoming the job) calling a different method (no
 * user/pass). NAF finds the authentication data in message this time. It establishes the
 * JAAS context and forwards the request on chain. The WS is not doing authentication
 * anymore, but relies on the JAAS context already established.Further, the same will
 * apply while communicating with SFWS. The SFWS relies on JAAS Login Context established
 * by NAF, while the Core Session is managed internally. The SFWS will be able to work
 * only if the JAAS context is kept valid (the Web Session is on).
 * </ol>
 * This plugin has to only block the login form for the requests addressed to WS. The
 * requests are identified by the prefix of the URL.
 *
 * @author rux
 */
public class WebServicesAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(WebServicesAuthenticator.class);

    protected String skipURL;

    public List<String> getUnAuthenticatedURLPrefix() {
        //skip webservices URL
        List<String> prefixes = new ArrayList<String>();
        prefixes.add(skipURL);
        return prefixes;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        //no need of login of whatsoever type
        return false;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        //WebServices not aware of any identity
        return null;
    }

    public void initPlugin(Map<String, String> parameters) {
        //store the URL prefix to skip as being called a webservice
        skipURL = parameters.get("URLSkip");
        log.debug("Configured URL to skip: " + skipURL);
        if (skipURL == null) {
            skipURL = "webservices/";
        }
        log.info("WebServices Authentication filter configured - " + skipURL);
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        //no need of login of whatsoever type
        return false;
    }

}
