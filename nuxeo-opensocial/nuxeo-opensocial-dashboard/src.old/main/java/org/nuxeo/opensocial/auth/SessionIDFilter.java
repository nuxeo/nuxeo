/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

public class SessionIDFilter implements NuxeoAuthenticationPlugin {

    private static final String JSSESSIONID = "JSESSIONID";;

    public List<String> getUnAuthenticatedURLPrefix() {
        return new ArrayList<String>();
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return Boolean.FALSE; // we never use a prompt
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String[] params = httpRequest.getParameterValues(JSSESSIONID);
        if ((params != null) && (params[0] != null)) {
            String sessionID = params[0].trim();
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                UserIdentificationInfo result = (UserIdentificationInfo) session.getAttribute(NXAuthConstants.USERIDENT_KEY);
                return result;
            }
        }
        return null;
    }

    public void initPlugin(Map<String, String> parameters) {
        // nothing to do

    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return Boolean.FALSE;
    }

}
