/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.auth.oauth;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.Principal;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.oauth.consumers.NuxeoOAuthConsumer;
import org.nuxeo.ecm.platform.oauth.consumers.OAuthConsumerRegistry;
import org.nuxeo.ecm.platform.oauth.keys.OAuthServerKeyManager;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthToken;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthTokenStore;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoSecuredRequestWrapper;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * This Filter is registred as a pre-Filter of NuxeoAuthenticationFilter.
 *
 * It is used to handle OAuth Authentication :
 *
 * - 3 legged OAuth negociation - 2 legged OAuth (Signed fetch)
 *
 *
 * @author tiry
 *
 */
public class NuxeoOAuthFilter implements NuxeoAuthPreFilter {

    protected static Log log = LogFactory.getLog(NuxeoOAuthFilter.class);

    protected static OAuthValidator validator;

    protected static OAuthConsumerRegistry consumerRegistry;

    protected OAuthValidator getValidator() {
        if (validator == null) {
            validator = new SimpleOAuthValidator();
        }
        return validator;
    }

    protected OAuthConsumerRegistry getOAuthConsumerRegistry() {
        if (consumerRegistry == null) {
            consumerRegistry = Framework.getLocalService(OAuthConsumerRegistry.class);
        }
        return consumerRegistry;
    }

    protected OAuthTokenStore getOAuthTokenStore() {
        return Framework.getLocalService(OAuthTokenStore.class);
    }

    protected boolean isOAuthSignedRequest(HttpServletRequest httpRequest) {

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.contains("OAuth")) {
            return true;
        }

        if ("GET".equals(httpRequest.getMethod())
                && httpRequest.getParameter("oauth_signature") != null) {
            return true;
        } else if ("POST".equals(httpRequest.getMethod())
                && "application/x-www-form-urlencoded".equals(httpRequest.getContentType())
                && httpRequest.getParameter("oauth_signature") != null) {
            return true;
        }

        return false;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String uri = httpRequest.getRequestURI();

            // process OAuth 3 legged calls
            if (uri.contains("/oauth/")) {
                String call = uri.split("/oauth/")[1];

                if (call.equals("authorize")) {
                    processAuthorize(httpRequest, httpResponse);
                } else if (call.equals("request-token")) {
                    processRequestToken(httpRequest, httpResponse);
                } else if (call.equals("access-token")) {
                    processAccessToken(httpRequest, httpResponse);

                } else {
                    httpResponse.sendError(
                            HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                            "OAuth call not supported");
                }
                return;
            }
            // Signed request (simple 2 legged OAuth call or signed request
            // after a 3 ledged nego)
            else if (isOAuthSignedRequest(httpRequest)) {

                LoginContext loginContext = processSignedRequest(httpRequest,
                        httpResponse);
                // foward the call if authenticated
                if (loginContext != null) {
                    Principal principal = (Principal) loginContext.getSubject().getPrincipals().toArray()[0];
                    try {
                        chain.doFilter(new NuxeoSecuredRequestWrapper(
                                httpRequest, principal), response);
                    } finally {
                        try {
                            loginContext.logout();
                        } catch (LoginException e) {
                            log.warn("Error when loging out", e);
                        }
                    }
                } else {
                    if (!httpResponse.isCommitted()) {
                        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                    return;
                }
            }
            // Non OAuth calls can pass through
            else {
                chain.doFilter(request, response);
            }
        } else {
            // NON http calls ???
            chain.doFilter(request, response);
        }
    }

    protected void processAuthorize(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws IOException,
            ServletException {

        String token = httpRequest.getParameter(OAuth.OAUTH_TOKEN);

        if (httpRequest.getMethod().equals("GET")) {

            log.info("OAuth authorize : from end user ");

            // initial access => send to real login page
            String loginUrl = VirtualHostHelper.getBaseURL(httpRequest);

            httpRequest.getSession(true).setAttribute("OAUTH-INFO",
                    getOAuthTokenStore().getRequestToken(token));

            String redirectUrl = "oauthGrant.jsp" + "?" + OAuth.OAUTH_TOKEN
                    + "=" + token;
            redirectUrl = URLEncoder.encode(redirectUrl, "UTF-8");
            loginUrl = loginUrl + "login.jsp?requestedUrl=" + redirectUrl;

            httpResponse.sendRedirect(loginUrl);

        } else {
            // post after permission validation
            log.info("OAuth authorize validate ");

            String nuxeo_login = httpRequest.getParameter("nuxeo_login");
            String duration = httpRequest.getParameter("duration");

            // XXX get what user has granted !!!

            OAuthToken rToken = getOAuthTokenStore().addVerifierToRequestToken(
                    token, Long.parseLong(duration));
            rToken.setNuxeoLogin(nuxeo_login);

            String cbUrl = rToken.getCallbackUrl();
            if (cbUrl==null) {
                // get the callback url from the consumer ...
                String consumerKey = rToken.getConsumerKey();
                NuxeoOAuthConsumer consumer = getOAuthConsumerRegistry().getConsumer(consumerKey);
                if (consumer!=null) {
                    cbUrl = consumer.getCallbackURL();
                }

                if (cbUrl==null) {
                    // fall back to default Google oauth callback ...
                    cbUrl = "http://oauth.gmodules.com/gadgets/oauthcallback";
                }
            }

            StringBuffer sb = new StringBuffer(cbUrl);
            sb.append("?");
            sb.append(OAuth.OAUTH_TOKEN);
            sb.append("=");
            sb.append(rToken.getToken());
            sb.append("&");
            sb.append("oauth_verifier");
            sb.append("=");
            sb.append(rToken.getVerifier());

            String targetUrl = sb.toString();

            log.info("redirecting user after successful grant " + sb.toString());

            httpResponse.sendRedirect(targetUrl);
        }

    }

    protected void processRequestToken(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws IOException,
            ServletException {

        OAuthMessage message = OAuthServlet.getMessage(httpRequest, null);
        String consumerKey = message.getConsumerKey();

        NuxeoOAuthConsumer consumer = getOAuthConsumerRegistry().getConsumer(
                consumerKey, message.getSignatureMethod());
        if (consumer == null) {
            log.error("Consumer " + consumerKey + " is not registred");
            int errCode = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.CONSUMER_KEY_UNKNOWN);
            httpResponse.sendError(errCode, "Unknown consumer key");
            return;
        }
        OAuthAccessor accessor = new OAuthAccessor(consumer);

        OAuthValidator validator = getValidator();
        try {
            validator.validateMessage(message, accessor);
        } catch (Exception e) {
            log.error("Error while validating OAuth signature", e);
            int errCode = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.SIGNATURE_INVALID);
            httpResponse.sendError(errCode, "Can not validate signature");
            return;
        }

        log.info("OAuth request-token : generate a tmp token");
        String callBack = message.getParameter(OAuth.OAUTH_CALLBACK);

        // XXX should not only use consumerKey !!!
        OAuthToken rToken = getOAuthTokenStore().createRequestToken(
                consumerKey, callBack);

        httpResponse.setContentType("application/x-www-form-urlencoded");
        httpResponse.setStatus(HttpServletResponse.SC_OK);

        StringBuffer sb = new StringBuffer();
        sb.append(OAuth.OAUTH_TOKEN);
        sb.append("=");
        sb.append(rToken.getToken());
        sb.append("&");
        sb.append(OAuth.OAUTH_TOKEN_SECRET);
        sb.append("=");
        sb.append(rToken.getTokenSecret());
        sb.append("&oauth_callback_confirmed=true");

        log.info("returning : " + sb.toString());

        httpResponse.getWriter().write(sb.toString());

    }

    protected void processAccessToken(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws IOException,
            ServletException {

        OAuthMessage message = OAuthServlet.getMessage(httpRequest, null);
        String consumerKey = message.getConsumerKey();
        String token = message.getToken();

        NuxeoOAuthConsumer consumer = getOAuthConsumerRegistry().getConsumer(
                consumerKey, message.getSignatureMethod());

        if (consumer == null) {
            log.error("Consumer " + consumerKey + " is not registred");
            int errCode = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.CONSUMER_KEY_UNKNOWN);
            httpResponse.sendError(errCode, "Unknown consumer key");
            return;
        }

        OAuthAccessor accessor = new OAuthAccessor(consumer);

        OAuthToken rToken = getOAuthTokenStore().getRequestToken(token);

        accessor.requestToken = rToken.getToken();
        accessor.tokenSecret = rToken.getTokenSecret();

        OAuthValidator validator = getValidator();

        try {
            validator.validateMessage(message, accessor);
        } catch (Exception e) {
            log.error("Error while validating OAuth signature", e);
            int errCode = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.SIGNATURE_INVALID);
            httpResponse.sendError(errCode, "Can not validate signature");
            return;
        }

        log.info("OAuth access-token : generate a real token");

        String verif = message.getParameter("oauth_verifier");
        token = message.getParameter(OAuth.OAUTH_TOKEN);

        log.info("OAuth verifier = " + verif);

        boolean allowByPassVerifier=false;

        if (verif==null) {
            // here we don't have the verifier in the request
            // this is strictly prohibited in the spec
            // => see http://tools.ietf.org/html/rfc5849 page 11
            //
            // Anyway since iGoogle does not seem to forward the verifier
            // we allow it for designated consumers

            allowByPassVerifier = consumer.allowBypassVerifier();
        }

        if (rToken.getVerifier().equals(verif) || allowByPassVerifier) {

            // Ok we can authenticate
            OAuthToken aToken = getOAuthTokenStore().createAccessTokenFromRequestToken(
                    rToken);

            httpResponse.setContentType("application/x-www-form-urlencoded");
            httpResponse.setStatus(HttpServletResponse.SC_OK);

            StringBuffer sb = new StringBuffer();
            sb.append(OAuth.OAUTH_TOKEN);
            sb.append("=");
            sb.append(aToken.getToken());
            sb.append("&");
            sb.append(OAuth.OAUTH_TOKEN_SECRET);
            sb.append("=");
            sb.append(aToken.getTokenSecret());

            log.info("returning : " + sb.toString());

            httpResponse.getWriter().write(sb.toString());

        } else {

            log.error("Verifier does not match : can not continue");
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Verifier is not correct");
            return;
        }
    }

    protected LoginContext processSignedRequest(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws IOException,
            ServletException {

        OAuthMessage message = OAuthServlet.getMessage(httpRequest, null);

        String consumerKey = message.getConsumerKey();
        String signatureMethod = message.getSignatureMethod();

        log.info("Received OAuth signed request on "
                + httpRequest.getRequestURI() + " with consumerKey="
                + consumerKey + " and signature method " + signatureMethod);

        NuxeoOAuthConsumer consumer = getOAuthConsumerRegistry().getConsumer(
                consumerKey, signatureMethod);

        if (consumer == null && consumerKey != null) {
            OAuthServerKeyManager okm = Framework.getLocalService(OAuthServerKeyManager.class);
            if (consumerKey.equals(okm.getInternalKey())) {
                consumer = okm.getInternalConsumer();
            }
        }

        if (consumer == null) {
            int errCode = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.CONSUMER_KEY_UNKNOWN);
            log.error("Consumer " + consumerKey
                    + " is unknow, can not authenticated");
            httpResponse.sendError(errCode, "Consumer " + consumerKey
                    + " is not registred");
            return null;
        } else {

            OAuthAccessor accessor = new OAuthAccessor(consumer);
            OAuthValidator validator = getValidator();

            OAuthToken aToken = getOAuthTokenStore().getAccessToken(
                    message.getToken());

            String targetLogin = null;
            if (aToken != null) {
                // Auth was done via 3 legged
                accessor.accessToken = aToken.getToken();
                accessor.tokenSecret = aToken.getTokenSecret();
                targetLogin = aToken.getNuxeoLogin();
            } else {
                // 2 legged OAuth
                if (!consumer.allowSignedFetch()) {
                    // int errCode =
                    // OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.SIGNATURE_METHOD_REJECTED);
                    // We need to send a 403 to force client to ask for a new
                    // token in case the Access Token was deleted !!!
                    int errCode = HttpServletResponse.SC_UNAUTHORIZED;
                    httpResponse.sendError(errCode,
                            "Signed fetch is not allowed");
                    return null;
                }
                targetLogin = consumer.getSignedFetchUser();
                if (NuxeoOAuthConsumer.SIGNEDFETCH_OPENSOCIAL_VIEWER.equals(targetLogin)) {
                    targetLogin = message.getParameter("opensocial_viewer_id");
                } else if (NuxeoOAuthConsumer.SIGNEDFETCH_OPENSOCIAL_OWNER.equals(targetLogin)) {
                    targetLogin = message.getParameter("opensocial_owner_id");
                }
            }

            try {
                validator.validateMessage(message, accessor);
                if (targetLogin != null) {
                    LoginContext loginContext = NuxeoAuthenticationFilter.loginAs(targetLogin);
                    return loginContext;
                } else {
                    int errCode = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.USER_REFUSED);
                    httpResponse.sendError(errCode,
                            "No configured login information");
                    return null;
                }
            } catch (Exception e) {
                log.error("Error while validating OAuth signature", e);
                int errCode = OAuth.Problems.TO_HTTP_CODE.get(OAuth.Problems.SIGNATURE_INVALID);
                httpResponse.sendError(errCode, "Can not validate signature");
            }
        }
        return null;
    }

}
