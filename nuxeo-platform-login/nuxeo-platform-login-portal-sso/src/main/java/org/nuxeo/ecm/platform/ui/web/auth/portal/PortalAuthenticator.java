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

package org.nuxeo.ecm.platform.ui.web.auth.portal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

import com.noelios.restlet.util.Base64;

public class PortalAuthenticator implements NuxeoAuthenticationPlugin {

    private static final String SECRET_KEY_NAME = "secret";

    private static final String MAX_AGE_KEY_NAME = "maxAge";

    private static final String TS_HEADER = "NX_TS";

    private static final String RANDOM_HEADER = "NX_RD";

    private static final String TOKEN_HEADER = "NX_TOKEN";

    private static final String USER_HEADER = "NX_USER";

    private static final String TOKEN_SEP = ":";

    //
    private String secret = "secret";

    // one hour by default
    private long maxAge = 60 * 60;

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return false;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String ts = httpRequest.getHeader(TS_HEADER);
        String random = httpRequest.getHeader(RANDOM_HEADER);
        String token = httpRequest.getHeader(TOKEN_HEADER);
        String userName = httpRequest.getHeader(USER_HEADER);

        if (userName == null || ts == null || random == null || token == null) {
            return null;
        }

        if (validateToken(ts, random, token, userName)) {
            return new UserIdentificationInfo(userName, userName);
        } else {
            return null;
        }
    }

    public void initPlugin(Map<String, String> parameters) {
        if (parameters.containsKey(SECRET_KEY_NAME)) {
            secret = parameters.get(SECRET_KEY_NAME);
        }
        if (parameters.containsKey(MAX_AGE_KEY_NAME)) {
            String maxAgeStr = parameters.get(MAX_AGE_KEY_NAME);
            if (maxAgeStr != null && !maxAgeStr.equals("")) {
                maxAge = Long.parseLong(maxAgeStr);
            }
        }
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return false;
    }

    private Boolean validateToken(String ts, String random, String token,
            String userName) {
        // reconstruct the token
        String clearToken = ts + TOKEN_SEP + random + TOKEN_SEP + secret
                + TOKEN_SEP + userName;

        byte[] hashedToken;
        try {
            hashedToken = MessageDigest.getInstance("MD5").digest(
                    clearToken.getBytes());
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
        String base64HashedToken = Base64.encodeBytes(hashedToken);

        // check that tokens are the same => that we have the same shared key
        if (!base64HashedToken.equals(token)) {
            return false;
        }

        // check time stamp
        long portalTS = Long.parseLong(ts);
        long currentTS = (new Date()).getTime();

        return (currentTS - portalTS) / 1000 <= maxAge;
    }

}
