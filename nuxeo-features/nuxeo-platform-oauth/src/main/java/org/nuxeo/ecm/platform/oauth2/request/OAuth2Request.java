/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */
package org.nuxeo.ecm.platform.oauth2.request;

import static org.nuxeo.ecm.platform.oauth2.Constants.CLIENT_ID_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.REDIRECT_URI_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.REDIRECT_URL_PARAM;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public abstract class OAuth2Request {

    protected String clientId;

    protected String redirectURI;

    public OAuth2Request() {
    }

    public OAuth2Request(HttpServletRequest request) {
        clientId = request.getParameter(CLIENT_ID_PARAM);
        redirectURI = decodeParameter(request, REDIRECT_URI_PARAM);
        // Fallback for non-RFC compliant client
        if (StringUtils.isBlank(redirectURI)) {
            redirectURI = decodeParameter(request, REDIRECT_URL_PARAM);
        }
    }

    public static String decodeParameter(HttpServletRequest request, String parameterName) {
        String value = request.getParameter(parameterName);
        try {
            if (StringUtils.isNotBlank(value)) {
                return URLDecoder.decode(value, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            // Nothing to do.
        }
        return value;
    }

    public String getRedirectURI() {
        return redirectURI;
    }

    public String getClientId() {
        return clientId;
    }
}
