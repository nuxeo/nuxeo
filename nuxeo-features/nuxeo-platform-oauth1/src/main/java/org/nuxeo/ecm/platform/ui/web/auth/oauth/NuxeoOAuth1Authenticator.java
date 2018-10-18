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
package org.nuxeo.ecm.platform.ui.web.auth.oauth;

import static java.lang.Boolean.FALSE;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static net.oauth.OAuth.OAUTH_SIGNATURE;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.oauth.consumers.NuxeoOAuthConsumer;
import org.nuxeo.ecm.platform.oauth.consumers.OAuthConsumerRegistry;
import org.nuxeo.ecm.platform.oauth.keys.OAuthServerKeyManager;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthToken;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthTokenStore;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

/**
 * OAuth 1 Authentication Plugin.
 *
 * @since 10.3
 */
public class NuxeoOAuth1Authenticator implements NuxeoAuthenticationPlugin {

    private static final Logger log = LogManager.getLogger(NuxeoOAuth1Authenticator.class);

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
        if (!isOAuth1SignedRequest(request)) {
            log.trace("Not an OAuth 1 signed request");
            return null;
        }
        String username = getIdentity(request);
        if (username == null) {
            log.trace("OAuth 1 auth failed");
            return null;
        }
        log.trace("OAuth 1 auth for user: {}", username);
        return new UserIdentificationInfo(username, username);
    }

    protected boolean isOAuth1SignedRequest(HttpServletRequest request) {
        String auth = request.getHeader(AUTHORIZATION);
        if (auth != null && auth.contains("OAuth")) {
            return true;
        }
        if (request.getParameter(OAUTH_SIGNATURE) != null) {
            return true;
        }
        return false;
    }

    protected String getIdentity(HttpServletRequest request) {
        return TransactionHelper.runInTransaction(() -> {
            try {
                return getOAuth1Identity(request);
            } catch (IOException e) {
                log.debug(e, e);
                return null;
            }
        });
    }

    /**
     * Verifies OAuth information and returns identity.
     */
    protected String getOAuth1Identity(HttpServletRequest request) throws IOException {
        String url = getRequestURL(request);
        OAuthMessage message = OAuthServlet.getMessage(request, url);

        String consumerKey = message.getConsumerKey();
        String signatureMethod = message.getSignatureMethod();
        OAuthConsumerRegistry consumerRegistry = Framework.getService(OAuthConsumerRegistry.class);
        NuxeoOAuthConsumer consumer = consumerRegistry.getConsumer(consumerKey, signatureMethod);

        if (consumer == null && consumerKey != null) {
            OAuthServerKeyManager okm = Framework.getService(OAuthServerKeyManager.class);
            if (consumerKey.equals(okm.getInternalKey())) {
                consumer = okm.getInternalConsumer();
            }
        }
        if (consumer == null) {
            return null;
        }

        OAuthAccessor accessor = new OAuthAccessor(consumer);
        OAuthValidator validator = new SimpleOAuthValidator();
        OAuthTokenStore tokenStore = Framework.getService(OAuthTokenStore.class);
        OAuthToken aToken = tokenStore.getAccessToken(message.getToken());
        String username;
        if (aToken != null) {
            // three-legged auth
            accessor.accessToken = aToken.getToken();
            accessor.tokenSecret = aToken.getTokenSecret();
            username = aToken.getNuxeoLogin();
        } else {
            // two-legged auth
            if (!consumer.allowSignedFetch()) {
                return null;
            }
            username = consumer.getSignedFetchUser();
            if (NuxeoOAuthConsumer.SIGNEDFETCH_OPENSOCIAL_VIEWER.equals(username)) {
                username = message.getParameter("opensocial_viewer_id");
            } else if (NuxeoOAuthConsumer.SIGNEDFETCH_OPENSOCIAL_OWNER.equals(username)) {
                username = message.getParameter("opensocial_owner_id");
            }
        }
        try {
            validator.validateMessage(message, accessor);
            return username;
        } catch (OAuthException | URISyntaxException e) {
            log.debug("Invalid OAuth signature", e);
            return null;
        }
    }

    /**
     * Gets the URL used for this request by checking the X-Forwarded-Proto header used in the request.
     */
    // public for tests
    public static String getRequestURL(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && !url.startsWith(forwardedProto)) {
            url = forwardedProto + url.substring(url.indexOf("://"));
        }
        return url;
    }

}
