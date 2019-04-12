/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Academie de Rennes - proxy CAS support
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.AnonymousAuthenticator;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Anonymous authenticator that redirect logout to CAS server authentication to connect to nuxeo.
 *
 * @author Benjamin JALON
 */
public class AnonymousAuthenticatorForCAS2 extends AnonymousAuthenticator {

    protected static final Log log = LogFactory.getLog(AnonymousAuthenticatorForCAS2.class);

    protected Cas2Authenticator casAuthenticator;

    @Override
    public Boolean handleLogout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        boolean isRedirectionToCas = false;

        Cookie[] cookies = httpRequest.getCookies();
        for (Cookie cookie : cookies) {
            if (NXAuthConstants.SSO_INITIAL_URL_REQUEST_KEY.equals(cookie.getName())) {
                isRedirectionToCas = true;
                break;
            }
        }

        if (isRedirectionToCas) {
            String authURL = getCas2Authenticator().getServiceURL(httpRequest, Cas2Authenticator.LOGIN_ACTION);
            String appURL = getCas2Authenticator().getAppURL(httpRequest);

            try {
                Map<String, String> urlParameters = new HashMap<>();
                urlParameters.put("service", appURL);
                String location = URIUtils.addParametersToURIQuery(authURL, urlParameters);
                httpResponse.sendRedirect(location);
                return true;
            } catch (IOException e) {
                log.error("Unable to redirect to CAS logout screen:", e);
                return false;
            }
        }

        return super.handleLogout(httpRequest, httpResponse);
    }

    public Cas2Authenticator getCas2Authenticator() {
        if (casAuthenticator != null) {
            return casAuthenticator;
        }

        PluggableAuthenticationService service = Framework.getService(PluggableAuthenticationService.class);
        if (service == null) {
            log.error("Can't get PluggableAuthenticationService");
            return null;
        }

        NuxeoAuthenticationPlugin plugin = service.getPlugin("CAS2_AUTH");
        if (plugin == null) {
            log.error("Can't get Cas Authenticator from PluggableAuthenticationService");
        }

        casAuthenticator = (Cas2Authenticator) plugin;
        return casAuthenticator;
    }

}
