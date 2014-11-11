/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.opensocial.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

public class OAuthSessionFilter implements NuxeoAuthenticationPlugin {
    private static final Log log = LogFactory.getLog(OAuthSessionFilter.class);

    public List<String> getUnAuthenticatedURLPrefix() {
        return new ArrayList<String>();
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return Boolean.FALSE; // we never use a prompt
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String header = httpRequest.getHeader("X-Shindig-AuthType");

        String[] params = httpRequest.getParameterValues("oauth_signature");
        if ((params != null) && (params[0] != null)) {
            String signature = params[0].trim();
            params = httpRequest.getParameterValues("opensocial_viewer_id");
            if ((params != null) && (params[0] != null)) {
                String viewer = params[0].trim();
                if (checkSignature(httpRequest, viewer, signature)) {
                    // for now we don't check the info given to us
                    UserIdentificationInfo info = new UserIdentificationInfo(
                            viewer, "");
                    info.setLoginPluginName("OAUTH");
                    return info;
                }
            }
        }
        return null;
    }

    private boolean checkSignature(HttpServletRequest httpRequest,
            String viewer, String signature) {
        return true;
    }

    public void initPlugin(Map<String, String> parameters) {
        // nothing to do
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return Boolean.FALSE;
    }

}
