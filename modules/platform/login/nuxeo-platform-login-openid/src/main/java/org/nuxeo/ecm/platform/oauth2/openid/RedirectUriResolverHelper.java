/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.oauth2.openid;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * Default RedirectUriResolver that allows overriding the redirect uri by setting a session attribute By default it will
 * use a fixed redirect uri since some provider do not support wildcards
 *
 * @since 5.7
 */
public class RedirectUriResolverHelper implements RedirectUriResolver {

    public static final String REDIRECT_URI_SESSION_ATTRIBUTE = "OPENID_REDIRECT_URI";

    @Override
    public String getRedirectUri(OpenIDConnectProvider openIDConnectProvider, HttpServletRequest request) {
        String redirectUri = (String) request.getSession().getAttribute(REDIRECT_URI_SESSION_ATTRIBUTE);
        // TODO - Use the requestedUrl for providers with support for wildcards
        // String requestedUrl = request.getParameter(NXAuthConstants.REQUESTED_URL);
        if (redirectUri == null) {
            redirectUri = VirtualHostHelper.getBaseURL(request) + LoginScreenHelper.getStartupPagePath() + "?" + ""
                    + "provider=" + openIDConnectProvider.oauth2Provider.getServiceName() + "&forceAnonymousLogin=true";
        }
        return redirectUri;
    }

}
