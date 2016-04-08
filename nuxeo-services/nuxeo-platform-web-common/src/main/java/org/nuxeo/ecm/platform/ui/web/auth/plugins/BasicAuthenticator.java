/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;

public class BasicAuthenticator implements NuxeoAuthenticationPlugin {

    protected static final String REALM_NAME_KEY = "RealmName";

    protected static final String FORCE_PROMPT_KEY = "ForcePromptURL";

    protected static final String AUTO_PROMPT_KEY = "AutoPrompt";

    protected static final String PROMPT_URL_KEY = "PromptUrl";

    protected static final String DEFAULT_REALMNAME = "Nuxeo 5";

    protected static final String BA_HEADER_NAME = "WWW-Authenticate";

    protected static final String EXCLUDE_URL_KEY = "ExcludeBAHeader";

    protected String realName;

    protected Boolean autoPrompt = false;

    protected List<String> forcePromptURLs;

    private List<String> excludedHeadersForBasicAuth;

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        try {

            if (needToAddBAHeader(httpRequest)) {
                String baHeader = "Basic realm=\"" + realName + '\"';
                httpResponse.addHeader(BA_HEADER_NAME, baHeader);
            }
            int statusCode;
            Integer requestStatusCode = (Integer) httpRequest.getAttribute(NXAuthConstants.LOGIN_STATUS_CODE);
            if (requestStatusCode != null) {
                statusCode = requestStatusCode;
            } else {
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;
            }
            httpResponse.sendError(statusCode);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks if we need to include a basic auth header back to the client.
     *
     * @param httpRequest
     * @return true if we need to include the auth header
     * @since 5.9.2
     */
    private boolean needToAddBAHeader(HttpServletRequest httpRequest) {
        for (String header : excludedHeadersForBasicAuth) {
            if (StringUtils.isNotBlank(httpRequest.getHeader(header))) {
                return false;
            }
            if (httpRequest.getCookies() != null) {
                for (Cookie cookie : httpRequest.getCookies()) {
                    if (cookie.getName().equals(header)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String auth = httpRequest.getHeader("authorization");

        if (auth != null && auth.toLowerCase().startsWith("basic")) {
            int idx = auth.indexOf(' ');
            String b64userPassword = auth.substring(idx + 1);
            byte[] clearUp = Base64.decode(b64userPassword);
            String userCredentials = new String(clearUp);
            int idxOfColon = userCredentials.indexOf(':');
            if (idxOfColon > 0 && idxOfColon < userCredentials.length() - 1) {
                String username = userCredentials.substring(0, idxOfColon);
                String password = userCredentials.substring(idxOfColon + 1);
                // forcing session cookie re-generation at login
                PluggableAuthenticationService service = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                        PluggableAuthenticationService.NAME);
                service.invalidateSession(httpRequest);
                return new UserIdentificationInfo(username, password);
            } else {
                return null;
            }
        }
        return null;
    }

    @Override
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

    @Override
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
        for (Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey().startsWith(FORCE_PROMPT_KEY)) {
                forcePromptURLs.add(entry.getValue());
            }
        }

        excludedHeadersForBasicAuth = new ArrayList<>();
        for (Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey().startsWith(EXCLUDE_URL_KEY)) {
                excludedHeadersForBasicAuth.add(entry.getValue());
            }
        }
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

}
