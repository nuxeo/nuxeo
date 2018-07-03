/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.jwt;

import static java.lang.Boolean.FALSE;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.nuxeo.ecm.jwt.JWTClaims.CLAIM_AUDIENCE;
import static org.nuxeo.ecm.jwt.JWTClaims.CLAIM_SUBJECT;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.runtime.api.Framework;

/**
 * JSON Web Token (JWT) Authentication Plugin.
 * <p>
 * The Authorization Bearer token from the headers is checked with the {@link JWTService} for validity, and if it is
 * valid the authentication is done for the token's subject.
 * <p>
 * If an "aud" claim ({@link JWTClaims#CLAIM_AUDIENCE}) is present in the token, it must be a prefix of the request HTTP
 * path info (excluding the web context). This allows limiting tokens for specific URL patterns.
 *
 * @since 10.3
 */
public class JWTAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(JWTAuthenticator.class);

    protected static final String BEARER_SP = "Bearer ";

    protected static final String ACCESS_TOKEN = "access_token";

    @Override
    public void initPlugin(Map<String, String> parameters) {
        // nothing to init
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null; // NOSONAR
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return FALSE;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        return FALSE;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest request, HttpServletResponse response) {
        String token = retrieveToken(request);
        if (token == null) {
            log.trace("No JWT token");
            return null;
        }
        JWTService service = Framework.getService(JWTService.class);
        Map<String, Object> claims = service.verifyToken(token);
        if (claims == null) {
            log.trace("JWT token invalid");
            return null;
        }
        Object sub = claims.get(CLAIM_SUBJECT);
        if (!(sub instanceof String)) {
            log.trace("JWT token contains non-String subject claim");
            return null;
        }
        String username = (String) sub;
        if (log.isTraceEnabled()) {
            log.trace("JWT token valid for username: " + username);
        }
        // check Audience
        Object aud = claims.get(CLAIM_AUDIENCE);
        if (aud != null) {
            if (!(aud instanceof String)) {
                log.trace("JWT token contains non-String audience claim");
                return null;
            }
            String audience = StringUtils.strip((String) aud, "/");
            String path = getRequestPath(request);
            if (!isEqualOrPathPrefix(path, audience)) {
                if (log.isTraceEnabled()) {
                    log.trace("JWT token for audience: " + audience + " but used with path: " + path);
                }
                return null;
            }
        }
        return new UserIdentificationInfo(username, username);
    }

    protected String retrieveToken(HttpServletRequest request) {
        String auth = request.getHeader(AUTHORIZATION);
        if (auth == null) {
            String token = request.getParameter(ACCESS_TOKEN);
            if (StringUtils.isNotEmpty(token)) {
                log.trace("Access token available from URI");
                return token;
            }
            log.trace("No Authorization header or URI access token");
        } else if (auth.startsWith(BEARER_SP)) {
            String token = auth.substring(BEARER_SP.length()).trim();
            if (!token.isEmpty()) {
                log.trace("Bearer token available");
                return token;
            }
            log.trace("Bearer token empty");
        } else {
            log.trace("Authorization header without Bearer token");
        }
        return null;
    }

    /**
     * Gets the request path. The returned value never starts nor ends with a slash.
     */
    protected static String getRequestPath(HttpServletRequest request) {
        String path = request.getServletPath(); // use decoded and normalized servlet path
        String info = request.getPathInfo();
        if (info != null) {
            path = path + info;
        }
        if (!path.isEmpty()) {
            path = path.substring(1); // strip initial /
        }
        return path;
    }

    /**
     * Compares path-wise a path with a prefix.
     */
    protected static boolean isEqualOrPathPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + '/');
    }

}
