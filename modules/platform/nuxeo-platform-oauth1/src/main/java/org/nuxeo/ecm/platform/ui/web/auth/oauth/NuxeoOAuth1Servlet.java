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

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static net.oauth.OAuth.OAUTH_CALLBACK;
import static net.oauth.OAuth.OAUTH_TOKEN;
import static net.oauth.OAuth.OAUTH_TOKEN_SECRET;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.REQUESTED_URL;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.oauth.consumers.NuxeoOAuthConsumer;
import org.nuxeo.ecm.platform.oauth.consumers.OAuthConsumerRegistry;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthToken;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthTokenStore;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

/**
 * Servlet for the /oauth endpoint.
 *
 * @since 10.3
 */
public class NuxeoOAuth1Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(NuxeoOAuth1Servlet.class);

    public static final String ENDPOINT_REQUEST_TOKEN = "/request-token";

    public static final String ENDPOINT_AUTHORIZE = "/authorize";

    public static final String ENDPOINT_ACCESS_TOKEN = "/access-token";

    public static final String OAUTH_VERIFIER = "oauth_verifier";

    public static final String OAUTH_CALLBACK_CONFIRMED = "oauth_callback_confirmed";

    public static final String NUXEO_LOGIN_PARAM = "nuxeo_login";

    public static final String DURATION_PARAM = "duration";

    public static final String OAUTH_INFO_SESSION_KEY = "OAUTH-INFO";

    public static final String GRANT_PAGE = "oauthGrant.jsp";

    public static final String LOGIN_PAGE = "login.jsp";

    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

    protected static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new NuxeoException(e); // cannot happen
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            response.sendError(SC_NOT_FOUND);
        } else if (pathInfo.equals(ENDPOINT_REQUEST_TOKEN)) {
            doGetRequestToken(request, response);
        } else if (pathInfo.equals(ENDPOINT_AUTHORIZE)) {
            doGetAuthorize(request, response);
        } else if (pathInfo.equals(ENDPOINT_ACCESS_TOKEN)) {
            doGetAccessToken(request, response);
        } else {
            response.sendError(SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            response.sendError(SC_NOT_FOUND);
        } else if (pathInfo.equals(ENDPOINT_AUTHORIZE)) {
            doPostAuthorize(request, response);
        } else {
            response.sendError(SC_NOT_FOUND);
        }
    }

    /**
     * Generates a request token, redirects to the Nuxeo login page, and provides a later redirect URL to the OAuth
     * grant page.
     */
    protected void doGetAuthorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = request.getParameter(OAUTH_TOKEN);
        // create request token
        OAuthTokenStore tokenStore = Framework.getService(OAuthTokenStore.class);
        OAuthToken rToken = tokenStore.getRequestToken(token);
        // store it in session
        request.getSession(true).setAttribute(OAUTH_INFO_SESSION_KEY, rToken);
        // redirect to login page with appropriate further redirect URL
        String redirectUrl = GRANT_PAGE + "?" + OAUTH_TOKEN + "=" + urlEncode(token);
        String url = VirtualHostHelper.getBaseURL(request) + LOGIN_PAGE + "?" + REQUESTED_URL + "="
                + urlEncode(redirectUrl);
        response.sendRedirect(url);
    }

    /**
     * Adds a verifier and username to the request token and redirects to the callback URL.
     */
    protected void doPostAuthorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = request.getParameter(OAUTH_TOKEN);
        String username = request.getParameter(NUXEO_LOGIN_PARAM);
        String duration = request.getParameter(DURATION_PARAM);
        // add verifier and username to request token
        OAuthTokenStore tokenStore = Framework.getService(OAuthTokenStore.class);
        OAuthToken rToken = tokenStore.addVerifierToRequestToken(token, Long.valueOf(duration));
        rToken.setNuxeoLogin(username);
        // find callback url
        String callbackUrl = rToken.getCallbackUrl();
        if (callbackUrl == null) {
            // get the callback url from the consumer
            OAuthConsumerRegistry consumerRegistry = Framework.getService(OAuthConsumerRegistry.class);
            NuxeoOAuthConsumer consumer = consumerRegistry.getConsumer(rToken.getConsumerKey());
            if (consumer == null) {
                int sc = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.CONSUMER_KEY_UNKNOWN).intValue();
                response.sendError(sc, "Unknown consumer key");
                return;
            }
            callbackUrl = consumer.getCallbackURL();
            if (callbackUrl == null) {
                log.error("No callback URL configured for consumer: " + rToken.getConsumerKey());
                response.sendError(SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put(OAUTH_TOKEN, rToken.getToken());
        params.put(OAUTH_VERIFIER, rToken.getVerifier());
        String targetUrl = URIUtils.addParametersToURIQuery(callbackUrl, params);
        response.sendRedirect(targetUrl);
    }

    protected void doGetRequestToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // get consumer
        OAuthMessage message = OAuthServlet.getMessage(request, null);
        String consumerKey = message.getConsumerKey();
        OAuthConsumerRegistry consumerRegistry = Framework.getService(OAuthConsumerRegistry.class);
        NuxeoOAuthConsumer consumer = consumerRegistry.getConsumer(consumerKey, message.getSignatureMethod());
        if (consumer == null) {
            int sc = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.CONSUMER_KEY_UNKNOWN).intValue();
            response.sendError(sc, "Unknown consumer key");
            return;
        }
        OAuthAccessor accessor = new OAuthAccessor(consumer);
        OAuthValidator validator = new SimpleOAuthValidator();
        try {
            validator.validateMessage(message, accessor);
        } catch (OAuthException | URISyntaxException | IOException e) {
            int sc = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.SIGNATURE_INVALID).intValue();
            response.sendError(sc, "Cannot validate signature");
            return;
        }
        String callbackUrl = message.getParameter(OAUTH_CALLBACK);
        OAuthTokenStore tokenStore = Framework.getService(OAuthTokenStore.class);
        OAuthToken rToken = tokenStore.createRequestToken(consumerKey, callbackUrl);

        Map<String, String> params = new LinkedHashMap<>();
        params.put(OAUTH_TOKEN, rToken.getToken());
        params.put(OAUTH_TOKEN_SECRET, rToken.getTokenSecret());
        params.put(OAUTH_CALLBACK_CONFIRMED, "true");
        String body = URIUtils.getURIQuery(params);
        response.setStatus(SC_OK);
        response.setContentType(APPLICATION_X_WWW_FORM_URLENCODED);
        response.getWriter().write(body);
    }

    protected void doGetAccessToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        OAuthMessage message = OAuthServlet.getMessage(request, null);
        String consumerKey = message.getConsumerKey();
        String token = message.getToken();
        OAuthConsumerRegistry consumerRegistry = Framework.getService(OAuthConsumerRegistry.class);
        NuxeoOAuthConsumer consumer = consumerRegistry.getConsumer(consumerKey, message.getSignatureMethod());
        if (consumer == null) {
            int sc = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.CONSUMER_KEY_UNKNOWN).intValue();
            response.sendError(sc, "Unknown consumer key");
            return;
        }
        // get request token
        OAuthAccessor accessor = new OAuthAccessor(consumer);
        OAuthTokenStore tokenStore = Framework.getService(OAuthTokenStore.class);
        OAuthToken rToken = tokenStore.getRequestToken(token);
        accessor.requestToken = rToken.getToken();
        accessor.tokenSecret = rToken.getTokenSecret();
        // validate signature
        OAuthValidator validator = new SimpleOAuthValidator();
        try {
            validator.validateMessage(message, accessor);
        } catch (OAuthException | URISyntaxException | IOException e) {
            int errCode = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.SIGNATURE_INVALID).intValue();
            response.sendError(errCode, "Cannot validate signature");
            return;
        }

        String verifier = message.getParameter(OAUTH_VERIFIER);
        boolean allowByPassVerifier = false;
        if (verifier == null) {
            // here we don't have the verifier in the request
            // this is strictly prohibited in the spec
            // => see http://tools.ietf.org/html/rfc5849 page 11
            // Anyway since iGoogle does not seem to forward the verifier
            // we allow it for designated consumers
            allowByPassVerifier = consumer.allowBypassVerifier();
        }
        if (!rToken.getVerifier().equals(verifier) && !allowByPassVerifier) {
            response.sendError(SC_UNAUTHORIZED, "Verifier is not correct");
            return;
        }
        OAuthToken aToken = tokenStore.createAccessTokenFromRequestToken(rToken);
        response.setStatus(SC_OK);
        response.setContentType(APPLICATION_X_WWW_FORM_URLENCODED);
        Map<String, String> params = new LinkedHashMap<>();
        params.put(OAUTH_TOKEN, aToken.getToken());
        params.put(OAUTH_TOKEN_SECRET, aToken.getTokenSecret());
        String body = URIUtils.getURIQuery(params);
        response.getWriter().write(body);
    }

}
