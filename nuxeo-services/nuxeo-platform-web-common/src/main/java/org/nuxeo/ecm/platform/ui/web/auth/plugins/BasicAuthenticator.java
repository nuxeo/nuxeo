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

package org.nuxeo.ecm.platform.ui.web.auth.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

public class BasicAuthenticator implements NuxeoAuthenticationPlugin {

    protected static final String REALM_NAME_KEY = "RealmName";
    protected static final String FORCE_PROMPT_KEY = "ForcePromptURL";
    protected static final String AUTO_PROMPT_KEY = "AutoPrompt";
    protected static final String PROMPT_URL_KEY = "PromptUrl";
    protected static final String DEFAULT_REALMNAME = "Nuxeo 5";
    protected static final String BA_HEADER_NAME = "WWW-Authenticate";

    protected String realName;

    protected Boolean autoPrompt = false;

    protected List<String> forcePromptURLs;

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        try {
            String baHeader = "Basic realm=\"" + realName + '\"';
            httpResponse.addHeader(BA_HEADER_NAME, baHeader);
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return false;
        }
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String auth = httpRequest.getHeader("authorization");

        if (auth != null && auth.toLowerCase().startsWith("basic")) {
            int idx = auth.indexOf(' ');
            String b64userpassword = auth.substring(idx + 1);
            byte[] clearUp = Base64.decode(b64userpassword);
            String userpassword = new String(clearUp);
            String[] up = userpassword.split(":");
            if (up.length==2) {
                String username = up[0];
                String password = up[1];
                return new UserIdentificationInfo(username, password);
            }
            else {
                return null;
            }
        }
        return null;
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        if (autoPrompt) {
            return true;
        } else {
            String requestedURI = httpRequest.getRequestURI();
            String context = httpRequest.getContextPath() + '/';
            requestedURI = requestedURI.substring(context.length());
            for (String prefixURL : forcePromptURLs) {
                if (requestedURI.startsWith(prefixURL)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void initPlugin(Map<String, String> parameters) {
        if (parameters.containsKey(REALM_NAME_KEY)) {
            realName = parameters.get(REALM_NAME_KEY);
        } else {
            realName = DEFAULT_REALMNAME;
        }

        if (parameters.containsKey(AUTO_PROMPT_KEY)) {
            autoPrompt = parameters.get(AUTO_PROMPT_KEY).equalsIgnoreCase("true");
        }

        forcePromptURLs = new ArrayList<String>();
        for (String key : parameters.keySet()) {
            if (key.startsWith(FORCE_PROMPT_KEY)) {
                forcePromptURLs.add(parameters.get(key));
            }
        }
    }

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

}
