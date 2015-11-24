/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.oauth2.openid;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * Default RedirectUriResolver that allows overriding the redirect uri by setting a session attribute
 * By default it will use a fixed redirect uri since some provider do not support wildcards
 *
 * @since 5.7
 */
public class RedirectUriResolverHelper implements RedirectUriResolver {

    public static final String REDIRECT_URI_SESSION_ATTRIBUTE = "OPENID_REDIRECT_URI";

    @Override
    public String getRedirectUri(OpenIDConnectProvider openIDConnectProvider, HttpServletRequest request) {
        String redirectUri = (String) request.getSession().getAttribute(REDIRECT_URI_SESSION_ATTRIBUTE);
        // TODO - Use the requestedUrl for providers with support for wildcards
        //String requestedUrl = request.getParameter(NXAuthConstants.REQUESTED_URL);
        if (redirectUri == null) {
            redirectUri =  VirtualHostHelper.getBaseURL(request) + "nxstartup.faces?" + ""
                    + "provider=" + openIDConnectProvider.oauth2Provider.getServiceName()
                    + "&forceAnonymousLogin=true";
        }
        return redirectUri;
    }

}
