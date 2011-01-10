package org.nuxeo.ecm.platform.ui.web.auth.oauth;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoSecuredRequestWrapper;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

public class NuxeoOAuthFilter implements Filter {

    protected static Log log = LogFactory.getLog(NuxeoOAuthFilter.class);

    protected static OAuthValidator validator;

    protected static OAuthConsumerRegistry consumerRegistry;

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    protected OAuthValidator getValidator() {
        if (validator==null) {
            validator = new SimpleOAuthValidator();
        }
        return validator;
    }

    protected OAuthConsumerRegistry getOAuthConsumerRegistry() {
        if (consumerRegistry==null) {
            consumerRegistry = new OAuthConsumerRegistry();
        }
        return consumerRegistry;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String uri = httpRequest.getRequestURI();
            String authHeader = httpRequest.getHeader("Authorization");

            // process OAuth 3 legged calls
            if (uri.contains("/oauth/")) {
                String call = uri.split("/oauth/")[1];

                if (call.equals("authorize")) {
                    processAuthorize(httpRequest, httpResponse);
                }
                else if (call.equals("request-token")) {
                    processRequestToken(httpRequest, httpResponse);
                }
                else if (call.equals("access-token")) {
                    processAccessToken(httpRequest, httpResponse);

                } else {
                    httpResponse.sendError(500, "OAuth call not supported");
                }

            }
            // Signed request (simple 2 legged OAuth call or signed request after a 3 ledged nego)
            else if (authHeader != null && authHeader.contains("OAuth")) {


                LoginContext loginContext = processSignedRequest(httpRequest, httpResponse);
                // foward the call if authenticated
                if (loginContext!=null) {
                    Principal principal = (Principal) loginContext.getSubject().getPrincipals().toArray()[0];
                    try {
                        chain.doFilter(new NuxeoSecuredRequestWrapper(httpRequest,principal), response);
                    }
                    finally {
                        try {
                            loginContext.logout();
                        } catch (LoginException e) {
                            log.warn("Error when loging out", e);
                        }
                    }
                } else {
                    //httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
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

    protected void processAuthorize(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {

        String token = httpRequest.getParameter(OAuth.OAUTH_TOKEN);

        if (httpRequest.getMethod().equals("GET")) {

            log.info("OAuth authorize : from end user ");

            // initial access => send to real login page
            String loginUrl = VirtualHostHelper.getBaseURL(httpRequest);

            httpRequest.getSession(true).setAttribute("OAUTH-INFO", RequestTokenStore.instance().get(token));

            String redirectUrl = "oauthGrant.jsp" + "?" + OAuth.OAUTH_TOKEN + "=" + token;
            redirectUrl = URLEncoder.encode(redirectUrl, "UTF-8");
            loginUrl = loginUrl + "login.jsp?requestedUrl=" + redirectUrl;

            httpResponse.sendRedirect(loginUrl);

        } else {
            // post after permission validation
            log.info("OAuth authorize validate ");

            String nuxeo_login = httpRequest.getParameter("nuxeo_login");

            // XXX get what user has granted !!!

            Map<String, String> data = RequestTokenStore.instance().generateVerifier(token);
            RequestTokenStore.instance().get(token).put("nuxeo-login", nuxeo_login);

            StringBuffer sb = new StringBuffer(data.get(OAuth.OAUTH_CALLBACK));
            sb.append("?");
            sb.append(OAuth.OAUTH_TOKEN);
            sb.append("=");
            sb.append(data.get(OAuth.OAUTH_TOKEN));
            sb.append("&");
            sb.append("oauth_verifier");
            sb.append("=");
            sb.append(data.get("oauth_verifier"));

            String targetUrl = sb.toString();

            log.info("redirecting user after successful grant " + sb.toString());

            httpResponse.sendRedirect(targetUrl);
        }

    }

    protected void processRequestToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse)  throws IOException, ServletException {

        OAuthMessage message = OAuthServlet.getMessage(httpRequest,null);
        String consumerKey = message.getConsumerKey();
        //String consumerSecret = getConsumerSecret(consumerKey);

        OAuthConsumer consumer = getOAuthConsumerRegistry().getConsumer(consumerKey);
        OAuthAccessor accessor = new OAuthAccessor(consumer);

        OAuthValidator validator = getValidator();
        try {
            validator.validateMessage(message, accessor);
        }
        catch (Exception e) {
            log.error("Error while validating OAuth signature", e);
            httpResponse.sendError(500, "Can not validate signature");
            return;
        }

        log.info("OAuth request-token : generate a tmp token");
        String callBack = message.getParameter(OAuth.OAUTH_CALLBACK);

        // XXX should not only use consumerKey !!!
        Map<String, String> data = RequestTokenStore.instance().store(consumerKey, callBack);

        httpResponse.setContentType("application/x-www-form-urlencoded");
        httpResponse.setStatus(HttpServletResponse.SC_OK);

        StringBuffer sb = new StringBuffer();
        sb.append(OAuth.OAUTH_TOKEN);
        sb.append("=");
        sb.append(data.get(OAuth.OAUTH_TOKEN));
        sb.append("&");
        sb.append(OAuth.OAUTH_TOKEN_SECRET);
        sb.append("=");
        sb.append(data.get(OAuth.OAUTH_TOKEN_SECRET));
        sb.append("&oauth_callback_confirmed=true");

        log.info("returning : " + sb.toString());

        httpResponse.getWriter().write(sb.toString());

    }

    protected void processAccessToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {

        OAuthMessage message = OAuthServlet.getMessage(httpRequest,null);
        String consumerKey = message.getConsumerKey();
        String token = message.getToken();

        OAuthConsumer consumer = getOAuthConsumerRegistry().getConsumer(consumerKey);
        OAuthAccessor accessor = new OAuthAccessor(consumer);

        Map<String, String> data = RequestTokenStore.instance().get(token);

        accessor.requestToken = data.get(OAuth.OAUTH_TOKEN);
        accessor.tokenSecret = data.get(OAuth.OAUTH_TOKEN_SECRET);

        OAuthValidator validator = getValidator();

        try {
            validator.validateMessage(message, accessor);
        }
        catch (Exception e) {
            log.error("Error while validating OAuth signature", e);
            httpResponse.sendError(500, "Can not validate signature");
            return;
        }

        log.info("OAuth access-token : generate a real token");

        String verif = message.getParameter("oauth_verifier");
        token = message.getParameter(OAuth.OAUTH_TOKEN);

        log.info("OAuth verifier = " + verif);

        // cleanup temp store
        RequestTokenStore.instance().remove(token);

        if (data.get("oauth_verifier").equals(verif)) {
            // Ok we can authenticate
            Map<String, String> newdata = AccessTokenStore.instance().generate(data);

            httpResponse.setContentType("application/x-www-form-urlencoded");
            httpResponse.setStatus(HttpServletResponse.SC_OK);

            StringBuffer sb = new StringBuffer();
            sb.append(OAuth.OAUTH_TOKEN);
            sb.append("=");
            sb.append(data.get(OAuth.OAUTH_TOKEN));
            sb.append("&");
            sb.append(OAuth.OAUTH_TOKEN_SECRET);
            sb.append("=");
            sb.append(data.get(OAuth.OAUTH_TOKEN_SECRET));

            log.info("returning : " + sb.toString());

            httpResponse.getWriter().write(sb.toString());

        } else {
            log.error("Verifier does not match : can not continue");
            httpResponse.sendError(500, "Verifier is not correct");
            return;
        }
    }

    protected LoginContext processSignedRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {


        OAuthMessage message = OAuthServlet.getMessage(httpRequest,null);

        String consumerKey = message.getConsumerKey();
        String signatureMethod = message.getSignatureMethod();

        log.info("Received OAuth signed request on " + httpRequest.getRequestURI() + " with consumerKey="
                + consumerKey + " and signature method "
                + signatureMethod);

        OAuthConsumer consumer = getOAuthConsumerRegistry().getConsumer(consumerKey);

        if (consumer == null) {
            log.error("Consumer " + consumerKey + " is unknow, can not authenticated");
            httpResponse.sendError(500, "Consumer " + consumerKey + " is not registred");
            return null;
        } else {

            OAuthAccessor accessor = new OAuthAccessor(consumer);
            OAuthValidator validator = getValidator();

            Map<String, String> data = AccessTokenStore.instance().get(message.getToken());
            String targetLogin = null;
            if (data!=null) {
                // Auth was done via 3 legged
                accessor.accessToken = data.get(OAuth.OAUTH_TOKEN);
                accessor.tokenSecret = data.get(OAuth.OAUTH_TOKEN_SECRET);
                targetLogin = data.get("nuxeo-login");
            } else {
                // find login from consumer ?
                // or find login from OpenSocial headers
            }

            try {
                validator.validateMessage(message, accessor);
                if (targetLogin!=null) {
                    LoginContext loginContext = NuxeoAuthenticationFilter.loginAs(targetLogin);
                    return loginContext;
                } else {
                    // see about opensocial user id
                    // XX
                }
            }
            catch (Exception e) {
                log.error("Error while validating OAuth signature", e);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Can not validate signature");
            }
        }
        return null;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // TODO Auto-generated method stub

    }

}
