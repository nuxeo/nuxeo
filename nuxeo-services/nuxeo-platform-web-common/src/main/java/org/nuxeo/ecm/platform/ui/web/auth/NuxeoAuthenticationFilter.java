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
 *     Thierry Delprat
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.ui.web.auth;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.LoginResponseHandler;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.ui.web.auth.service.AuthenticationPluginDescriptor;
import org.nuxeo.ecm.platform.ui.web.auth.service.OpenUrlDescriptor;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Servlet filter handling Nuxeo authentication (JAAS + EJB).
 * <p>
 * Also handles logout and identity switch.
 *
 * @author Thierry Delprat
 * @author Bogdan Stefanescu
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 */
public class NuxeoAuthenticationFilter implements Filter {

    // protected static final String EJB_LOGIN_DOMAIN = "nuxeo-system-login";

    public static final String DEFAULT_START_PAGE = "nxstartup.faces";

    protected static final String LOGIN_DOMAIN = "nuxeo-ecm-web";

    protected static final String XMLHTTP_REQUEST_TYPE = "XMLHttpRequest";

    protected static final String LOGIN_JMS_CATEGORY = "NuxeoAuthentication";

    private static final Log log = LogFactory.getLog(NuxeoAuthenticationFilter.class);

    private static String anonymous;

    protected final boolean avoidReauthenticate = true;

    protected PluggableAuthenticationService service;

    protected List<String> unAuthenticatedURLPrefix;

    /**
     * On WebEngine (Jetty) we don't have JMS enabled so we should disable log
     */
    protected boolean byPassAuthenticationLog = false;

    /**
     * Which security domain to use
     */
    protected String securityDomain = LOGIN_DOMAIN;

    public void destroy() {
    }

    protected static boolean sendAuthenticationEvent(
            UserIdentificationInfo userInfo, String eventId, String comment) {

        LoginContext loginContext = null;
        try {
            try {
                loginContext = Framework.login();
            } catch (LoginException e) {
                log.error("Unable to log in in order to log Login event"
                        + e.getMessage());
                return false;
            }

            EventProducer evtProducer = null;
            try {
                evtProducer = Framework.getService(EventProducer.class);
            } catch (Exception e) {
                log.error("Unable to get Event producer: " + e.getMessage());
                return false;
            }

            Principal principal = new SimplePrincipal(userInfo.getUserName());

            Map<String, Serializable> props = new HashMap<String, Serializable>();
            props.put("AuthenticationPlugin", userInfo.getAuthPluginName());
            props.put("LoginPlugin", userInfo.getLoginPluginName());
            props.put("category", LOGIN_JMS_CATEGORY);
            props.put("comment", comment);

            EventContext ctx = new UnboundEventContext(principal, props);
            try {
                evtProducer.fireEvent(ctx.newEvent(eventId));
            } catch (ClientException e) {
                log.error("Unable to send authentication event", e);
            }
            return true;
        } finally {
            if (loginContext != null) {
                try {
                    loginContext.logout();
                } catch (LoginException e) {
                    log.error("Unable to logout: " + e.getMessage());
                }
            }
        }
    }

    protected boolean logAuthenticationAttempt(UserIdentificationInfo userInfo,
            boolean success) {
        if (byPassAuthenticationLog) {
            return true;
        }
        String userName = userInfo.getUserName();
        if (userName == null || userName.length() == 0) {
            userName = userInfo.getToken();
        }

        String eventId;
        String comment;
        if (success) {
            eventId = "loginSuccess";
            comment = userName + " successfully logged in using "
                    + userInfo.getAuthPluginName() + "Authentication";
        } else {
            eventId = "loginFailed";
            comment = userName + " failed to authenticate using "
                    + userInfo.getAuthPluginName() + "Authentication";
        }

        return sendAuthenticationEvent(userInfo, eventId, comment);
    }

    protected boolean logLogout(UserIdentificationInfo userInfo) {
        if (byPassAuthenticationLog) {
            return true;
        }
        String userName = userInfo.getUserName();
        if (userName == null || userName.length() == 0) {
            userName = userInfo.getToken();
        }

        String eventId = "logout";
        String comment = userName + " logged out";

        return sendAuthenticationEvent(userInfo, eventId, comment);
    }

    protected Principal doAuthenticate(
            CachableUserIdentificationInfo cachableUserIdent,
            HttpServletRequest httpRequest) {

        LoginContext loginContext;
        try {
            CallbackHandler handler = service.getCallbackHandler(cachableUserIdent.getUserInfo());
            loginContext = new LoginContext(securityDomain, handler);

            synchronized (NuxeoAuthenticationFilter.class) {
                loginContext.login();
            }

            Principal principal = (Principal) loginContext.getSubject().getPrincipals().toArray()[0];
            cachableUserIdent.setPrincipal(principal);
            cachableUserIdent.setAlreadyAuthenticated(true);
            // re-set the userName since for some SSO based on token,
            // the userName is not known before login is completed
            cachableUserIdent.getUserInfo().setUserName(principal.getName());

            logAuthenticationAttempt(cachableUserIdent.getUserInfo(), true);
        } catch (LoginException e) {
            // TODO Auto-generated catch block
            logAuthenticationAttempt(cachableUserIdent.getUserInfo(), false);
            return null;
        }

        // store login context for the time of the request
        // TODO logincontext is also stored in cachableUserIdent - it is really
        // needed to store it??
        httpRequest.setAttribute(NXAuthConstants.LOGINCONTEXT_KEY, loginContext);

        // store user ident
        cachableUserIdent.setLoginContext(loginContext);
        boolean createSession = needSessionSaving(cachableUserIdent.getUserInfo());
        HttpSession session = httpRequest.getSession(createSession);
        if (session != null) {
            session.setAttribute(NXAuthConstants.USERIDENT_KEY,
                    cachableUserIdent);
        }

        service.onAuthenticatedSessionCreated(httpRequest, session,
                cachableUserIdent);

        return cachableUserIdent.getPrincipal();
    }

    private boolean switchUser(ServletRequest request,
            ServletResponse response, FilterChain chain) throws IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String deputyLogin = (String) httpRequest.getAttribute(NXAuthConstants.SWITCH_USER_KEY);
        String targetPageAfterSwitch = (String) httpRequest.getAttribute(NXAuthConstants.PAGE_AFTER_SWITCH);
        if (targetPageAfterSwitch == null) {
            targetPageAfterSwitch = DEFAULT_START_PAGE;
        }

        CachableUserIdentificationInfo cachableUserIdent = retrieveIdentityFromCache(httpRequest);
        String originatingUser = cachableUserIdent.getUserInfo().getUserName();

        if (deputyLogin == null) {
            // simply switch back to the previous identity
            NuxeoPrincipal currentPrincipal = (NuxeoPrincipal) cachableUserIdent.getPrincipal();
            String previousUser = currentPrincipal.getOriginatingUser();
            if (previousUser == null) {
                return false;
            }
            deputyLogin = previousUser;
            originatingUser = null;
        }

        try {
            cachableUserIdent.getLoginContext().logout();
        } catch (LoginException e1) {
            log.error("Error while logout from main identity", e1);
        }

        HttpSession session = httpRequest.getSession(false);
        session = service.reinitSession(httpRequest);

        CachableUserIdentificationInfo newCachableUserIdent = new CachableUserIdentificationInfo(
                deputyLogin, deputyLogin);

        newCachableUserIdent.getUserInfo().setLoginPluginName("Trusting_LM");
        newCachableUserIdent.getUserInfo().setAuthPluginName(
                cachableUserIdent.getUserInfo().getAuthPluginName());

        Principal principal = doAuthenticate(newCachableUserIdent, httpRequest);
        if (principal != null) {
            NuxeoPrincipal nxUser = (NuxeoPrincipal) principal;
            if (originatingUser != null) {
                nxUser.setOriginatingUser(originatingUser);
            }
            propagateUserIdentificationInformation(cachableUserIdent);
        }

        // reinit Seam so the afterResponseComplete does not crash
        // ServletLifecycle.beginRequest(httpRequest);

        // flag redirect to avoid being caught by URLPolicy
        request.setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY,
                Boolean.TRUE);
        String baseURL = service.getBaseURL(request);
        ((HttpServletResponse) response).sendRedirect(baseURL
                + targetPageAfterSwitch);

        return true;
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        // NXP-5555: set encoding to UTF-8 in case this method is called before
        // encoding is set to UTF-8 on the request
        if (request.getCharacterEncoding() == null) {
            try {
                request.setCharacterEncoding("UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error(e, e);
            }
        }

        String tokenPage = getRequestedPage(request);
        if (tokenPage.equals(NXAuthConstants.SWITCH_USER_PAGE)) {
            boolean result = switchUser(request, response, chain);
            if (result) {
                return;
            }
        }

        if (request instanceof NuxeoSecuredRequestWrapper) {
            log.debug("ReEntering Nuxeo Authentication Filter ... exiting directly");
            chain.doFilter(request, response);
            return;
        } else if (service.canBypassRequest(request)) {
            log.debug("ReEntering Nuxeo Authentication Filter after URL rewrite ... exiting directly");
            chain.doFilter(request, response);
            return;
        } else {
            log.debug("Entering Nuxeo Authentication Filter");
        }

        String targetPageURL = null;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Principal principal = httpRequest.getUserPrincipal();

        if (principal == null) {
            log.debug("Principal not found inside Request via getUserPrincipal");
            // need to authenticate !

            // retrieve user & password
            CachableUserIdentificationInfo cachableUserIdent;
            if (avoidReauthenticate) {
                log.debug("Try getting authentication from cache");
                cachableUserIdent = retrieveIdentityFromCache(httpRequest);
            } else {
                log.debug("Principal cache is NOT activated");
            }

            if (cachableUserIdent != null
                    && cachableUserIdent.getUserInfo() != null
                    && service.needResetLogin(request)) {
                HttpSession session = httpRequest.getSession(false);
                if (session != null) {
                    session.removeAttribute(NXAuthConstants.USERIDENT_KEY);
                }
                // first propagate the login because invalidation may require
                // an authenticated session
                propagateUserIdentificationInformation(cachableUserIdent);
                // invalidate Session !
                service.invalidateSession(request);
                // TODO perform logout?
                cachableUserIdent = null;
            }

            // identity found in cache
            if (cachableUserIdent != null
                    && cachableUserIdent.getUserInfo() != null) {
                log.debug("userIdent found in cache, get the Principal from it without reloggin");

                principal = cachableUserIdent.getPrincipal();
                log.debug("Principal = " + principal.getName());
                propagateUserIdentificationInformation(cachableUserIdent);

                String requestedPage = getRequestedPage(httpRequest);
                if (requestedPage.equals(NXAuthConstants.LOGOUT_PAGE)) {
                    boolean redirected = handleLogout(request, response,
                            cachableUserIdent);
                    cachableUserIdent = null;
                    principal = null;
                    if (redirected
                            && httpRequest.getParameter(NXAuthConstants.FORM_SUBMITTED_MARKER) == null) {
                        return;
                    }
                } else {
                    targetPageURL = getSavedRequestedURL(httpRequest,
                            httpResponse);
                }
            }

            // identity not found in cache or reseted by logout
            if (cachableUserIdent == null
                    || cachableUserIdent.getUserInfo() == null) {
                UserIdentificationInfo userIdent = handleRetrieveIdentity(
                        httpRequest, httpResponse);
                if (userIdent != null
                        && userIdent.getUserName().equals(getAnonymousId())) {
                    String forceAuth = httpRequest.getParameter(NXAuthConstants.FORCE_ANONYMOUS_LOGIN);
                    if (forceAuth != null && forceAuth.equals("true")) {
                        userIdent = null;
                    }
                }
                if ((userIdent == null || !userIdent.containsValidIdentity())
                        && !bypassAuth(httpRequest)) {

                    boolean res = handleLoginPrompt(httpRequest, httpResponse);
                    if (res) {
                        return;
                    }
                } else {
                    // restore saved Starting page
                    targetPageURL = getSavedRequestedURL(httpRequest,
                            httpResponse);
                }

                if (userIdent != null && userIdent.containsValidIdentity()) {
                    // do the authentication
                    cachableUserIdent = new CachableUserIdentificationInfo(
                            userIdent);
                    principal = doAuthenticate(cachableUserIdent, httpRequest);
                    if (principal != null) {
                        // Do the propagation too ????
                        propagateUserIdentificationInformation(cachableUserIdent);
                        // setPrincipalToSession(httpRequest, principal);
                        // check if the current authenticator is a
                        // LoginResponseHandler
                        NuxeoAuthenticationPlugin plugin = getAuthenticator(cachableUserIdent);
                        if (plugin instanceof LoginResponseHandler) {
                            // call the extended error handler
                            if (((LoginResponseHandler) plugin).onSuccess(
                                    (HttpServletRequest) request,
                                    (HttpServletResponse) response)) {
                                return;
                            }
                        }
                    } else {
                        // first check if the current authenticator is a
                        // LoginResponseHandler
                        NuxeoAuthenticationPlugin plugin = getAuthenticator(cachableUserIdent);
                        if (plugin instanceof LoginResponseHandler) {
                            // call the extended error handler
                            if (((LoginResponseHandler) plugin).onError(
                                    (HttpServletRequest) request,
                                    (HttpServletResponse) response)) {
                                return;
                            }
                        } else {
                            // use the old method
                            httpRequest.setAttribute(
                                    NXAuthConstants.LOGIN_ERROR,
                                    NXAuthConstants.ERROR_AUTHENTICATION_FAILED);
                            boolean res = handleLoginPrompt(httpRequest,
                                    httpResponse);
                            if (res) {
                                return;
                            }
                        }
                    }

                }
            }
        }

        if (principal != null) {
            if (targetPageURL != null && targetPageURL.length() > 0) {
                // forward to target page
                String baseURL = service.getBaseURL(request);

                // httpRequest.getRequestDispatcher(targetPageURL).forward(new
                // NuxeoSecuredRequestWrapper(httpRequest, principal),
                // response);
                if (XMLHTTP_REQUEST_TYPE.equalsIgnoreCase(httpRequest.getHeader("X-Requested-With"))) {
                    // httpResponse.setStatus(200);
                    return;
                } else {
                    httpResponse.sendRedirect(baseURL + targetPageURL);
                    return;
                }

            } else {
                // simply continue request
                chain.doFilter(new NuxeoSecuredRequestWrapper(httpRequest,
                        principal), response);
            }
        } else {
            chain.doFilter(request, response);
        }

        if (!avoidReauthenticate) {
            // destroy login context
            log.debug("Log out");
            LoginContext lc = (LoginContext) httpRequest.getAttribute("LoginContext");
            if (lc != null) {
                try {
                    lc.logout();
                } catch (LoginException e) {
                    log.error(e, e);
                }
            }
        }
        log.debug("Exit Nuxeo Authentication filter");
    }

    public NuxeoAuthenticationPlugin getAuthenticator(
            CachableUserIdentificationInfo ci) {
        String key = ci.getUserInfo().getAuthPluginName();
        if (key != null) {
            NuxeoAuthenticationPlugin authPlugin = service.getPlugin(key);
            return authPlugin;
        }
        return null;
    }

    protected static CachableUserIdentificationInfo retrieveIdentityFromCache(
            HttpServletRequest httpRequest) {

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            CachableUserIdentificationInfo cachableUserInfo = (CachableUserIdentificationInfo) session.getAttribute(NXAuthConstants.USERIDENT_KEY);
            if (cachableUserInfo != null) {
                return cachableUserInfo;
            }
        }

        return null;
    }

    private String getAnonymousId() throws ServletException {
        if (anonymous == null) {
            try {
                UserManager um = Framework.getService(UserManager.class);
                anonymous = um.getAnonymousUserId();
            } catch (Exception e) {
                log.error("Can't find anonymous User id", e);
                anonymous = "";
                throw new ServletException("Can't find anonymous user id");
            }
        }
        return anonymous;
    }

    public void init(FilterConfig config) throws ServletException {
        String val = config.getInitParameter("byPassAuthenticationLog");
        if (val != null && Boolean.parseBoolean(val)) {
            byPassAuthenticationLog = true;
        }
        val = config.getInitParameter("securityDomain");
        if (val != null) {
            securityDomain = val;
        }

        service = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                PluggableAuthenticationService.NAME);
        if (service == null) {
            log.error("Unable to get Service "
                    + PluggableAuthenticationService.NAME);
            throw new ServletException(
                    "Can't initialize Nuxeo Pluggable Authentication Service");
        }
    }

    /**
     * Save requested URL before redirecting to login form.
     * <p>
     * Returns true if target url is a valid startup page.
     */
    public boolean saveRequestedURLBeforeRedirect(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        HttpSession session;
        if (httpResponse.isCommitted()) {
            session = httpRequest.getSession(false);
        } else {
            session = httpRequest.getSession(true);
        }

        if (session == null) {
            return false;
        }

        String requestPage;
        boolean requestPageInParams = false;
        if (httpRequest.getParameter(NXAuthConstants.REQUESTED_URL) != null) {
            requestPageInParams = true;
            requestPage = httpRequest.getParameter(NXAuthConstants.REQUESTED_URL);
        } else {
            requestPage = getRequestedUrl(httpRequest);
        }

        if (requestPage == null) {
            return false;
        }

        // avoid redirect if not useful
        if (requestPage.startsWith(DEFAULT_START_PAGE)) {
            return true;
        }

        // avoid saving to session is start page is not valid or if it's
        // already in the request params
        if (isStartPageValid(requestPage)) {
            if (!requestPageInParams) {
                session.setAttribute(NXAuthConstants.START_PAGE_SAVE_KEY,
                        requestPage);
            }
            return true;
        }

        return false;
    }

    public static String getRequestedUrl(HttpServletRequest httpRequest) {
        String completeURI = httpRequest.getRequestURI();
        String qs = httpRequest.getQueryString();
        String context = httpRequest.getContextPath() + '/';
        String requestPage = completeURI.substring(context.length());
        if (qs != null && qs.length() > 0) {
            // remove conversationId if present
            if (qs.contains("conversationId")) {
                qs = qs.replace("conversationId", "old_conversationId");
            }
            requestPage = requestPage + '?' + qs;
        }
        return requestPage;
    }

    protected static String getSavedRequestedURL(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String requestedPage = null;
        if (httpRequest.getParameter(NXAuthConstants.REQUESTED_URL) == null) {
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                requestedPage = (String) session.getAttribute(NXAuthConstants.START_PAGE_SAVE_KEY);
                if (requestedPage != null) {
                    // clean up session
                    session.removeAttribute(NXAuthConstants.START_PAGE_SAVE_KEY);
                }
            }
        } else {
            requestedPage = httpRequest.getParameter(NXAuthConstants.REQUESTED_URL);
        }

        // if SSO authentication cookie store the initial URL asked

        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (NXAuthConstants.SSO_INITIAL_URL_REQUEST_KEY.equals(cookie.getName())) {
                    requestedPage = cookie.getValue();
                    cookie.setMaxAge(0);
                }
            }
        }

        String requestedUrl = httpRequest.getParameter(NXAuthConstants.REQUESTED_URL);
        if (requestedUrl != null && !"".equals(requestedUrl)) {
            try {
                return URLDecoder.decode(requestedUrl, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error("Unable to get the requestedUrl parameter" + e);
            }
        }
        return requestedPage;
    }

    protected boolean isStartPageValid(String startPage) {
        if (startPage == null) {
            return false;
        }
        for (String prefix : service.getStartURLPatterns()) {
            if (startPage.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    protected boolean handleLogout(ServletRequest request,
            ServletResponse response,
            CachableUserIdentificationInfo cachedUserInfo)
            throws ServletException {
        logLogout(cachedUserInfo.getUserInfo());

        // invalidate Session !
        service.invalidateSession(request);

        request.setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, true);
        Map<String, String> parameters = new HashMap<String, String>();
        String securityError = request.getParameter(NXAuthConstants.SECURITY_ERROR);
        if (securityError != null) {
            parameters.put(NXAuthConstants.SECURITY_ERROR, securityError);
        }
        if (cachedUserInfo.getPrincipal().getName().equals(getAnonymousId())) {
            parameters.put(NXAuthConstants.FORCE_ANONYMOUS_LOGIN, "true");
        }
        String requestedUrl = request.getParameter(NXAuthConstants.REQUESTED_URL);
        if (requestedUrl != null) {
            parameters.put(NXAuthConstants.REQUESTED_URL, requestedUrl);
        }
        // Reset JSESSIONID Cookie
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        httpResponse.addCookie(cookie);

        String pluginName = cachedUserInfo.getUserInfo().getAuthPluginName();
        NuxeoAuthenticationPlugin authPlugin = service.getPlugin(pluginName);
        NuxeoAuthenticationPluginLogoutExtension logoutPlugin = null;

        if (authPlugin instanceof NuxeoAuthenticationPluginLogoutExtension) {
            logoutPlugin = (NuxeoAuthenticationPluginLogoutExtension) authPlugin;
        }

        boolean redirected = false;
        if (logoutPlugin != null) {
            redirected = logoutPlugin.handleLogout(
                    (HttpServletRequest) request,
                    (HttpServletResponse) response);
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (!redirected
                && !XMLHTTP_REQUEST_TYPE.equalsIgnoreCase(httpRequest.getHeader("X-Requested-With"))) {
            String baseURL = service.getBaseURL(request);
            try {
                String url = baseURL + DEFAULT_START_PAGE;
                url = URIUtils.addParametersToURIQuery(url, parameters);
                ((HttpServletResponse) response).sendRedirect(url);
                redirected = true;
            } catch (IOException e) {
                log.error("Unable to redirect to default start page after logout : "
                        + e.getMessage());
            }
        }

        try {
            cachedUserInfo.getLoginContext().logout();
        } catch (LoginException e) {
            log.error("Unable to logout " + e.getMessage());
        }
        return redirected;
    }

    // App Server JAAS SPI
    protected void propagateUserIdentificationInformation(
            CachableUserIdentificationInfo cachableUserIdent) {
        service.propagateUserIdentificationInformation(cachableUserIdent);
    }

    // Plugin API

    protected void initUnAuthenticatedURLPrefix() {
        // gather unAuthenticated URLs
        unAuthenticatedURLPrefix = new ArrayList<String>();
        for (String pluginName : service.getAuthChain()) {
            NuxeoAuthenticationPlugin plugin = service.getPlugin(pluginName);
            List<String> prefix = plugin.getUnAuthenticatedURLPrefix();
            if (prefix != null && !prefix.isEmpty()) {
                unAuthenticatedURLPrefix.addAll(prefix);
            }
        }
    }

    protected boolean bypassAuth(HttpServletRequest httpRequest) {
        if (unAuthenticatedURLPrefix == null) {
            // late init to allow plugins registered after this filter init
            initUnAuthenticatedURLPrefix();
        }
        String requestPage = getRequestedPage(httpRequest);
        for (String prefix : unAuthenticatedURLPrefix) {
            if (requestPage.startsWith(prefix)) {
                return true;
            }
        }

        List<OpenUrlDescriptor> openUrls = service.getOpenUrls();
        for (OpenUrlDescriptor openUrl : openUrls) {
            if (openUrl.allowByPassAuth(httpRequest)) {
                return true;
            }
        }

        return false;
    }

    public static String getRequestedPage(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            return getRequestedPage(httpRequest);
        } else {
            return null;
        }
    }

    protected static String getRequestedPage(HttpServletRequest httpRequest) {
        String requestURI = httpRequest.getRequestURI();
        String context = httpRequest.getContextPath() + '/';

        return requestURI.substring(context.length());
    }

    protected boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String baseURL = service.getBaseURL(httpRequest);

        // go through plugins to get UserIndentity
        for (String pluginName : service.getAuthChain(httpRequest)) {
            NuxeoAuthenticationPlugin plugin = service.getPlugin(pluginName);
            AuthenticationPluginDescriptor descriptor = service.getDescriptor(pluginName);

            if (plugin.needLoginPrompt(httpRequest)) {
                if (descriptor.getNeedStartingURLSaving()) {
                    saveRequestedURLBeforeRedirect(httpRequest, httpResponse);
                }
                return plugin.handleLoginPrompt(httpRequest, httpResponse,
                        baseURL);
            }
        }

        log.warn("No auth plugin can be found to do the Login Prompt");
        return false;
    }

    protected UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        UserIdentificationInfo userIdent = null;

        // go through plugins to get UserIndentity
        for (String pluginName : service.getAuthChain(httpRequest)) {
            NuxeoAuthenticationPlugin plugin = service.getPlugin(pluginName);
            if (plugin != null) {
                log.debug("Trying to retrieve userIndetification using plugin "
                        + pluginName);
                userIdent = plugin.handleRetrieveIdentity(httpRequest,
                        httpResponse);
                if (userIdent != null && userIdent.containsValidIdentity()) {
                    // fill information for the Login module
                    userIdent.setAuthPluginName(pluginName);

                    // get the target login module
                    String loginModulePlugin = service.getDescriptor(pluginName).getLoginModulePlugin();
                    userIdent.setLoginPluginName(loginModulePlugin);

                    // get the additional parameters
                    Map<String, String> parameters = service.getDescriptor(
                            pluginName).getParameters();
                    userIdent.setLoginParameters(parameters);

                    break;
                }
            } else {
                log.error("Auth plugin " + pluginName
                        + " can not be retrieved from service");
            }
        }

        // Fall back to cache (used only when avoidReautenticated=false)
        if (userIdent == null || !userIdent.containsValidIdentity()) {
            log.debug("user/password not found in request, try into identity cache");
            HttpSession session = httpRequest.getSession(false);
            if (session == null) {
                // possible we need a new session
                if (httpRequest.isRequestedSessionIdValid()) {
                    session = httpRequest.getSession(true);
                }
            }
            if (session != null) {
                CachableUserIdentificationInfo savedUserInfo = retrieveIdentityFromCache(httpRequest);
                if (savedUserInfo != null) {
                    log.debug("Found User identity in cache :"
                            + savedUserInfo.getUserInfo().getUserName() + '/'
                            + savedUserInfo.getUserInfo().getPassword());
                    userIdent = new UserIdentificationInfo(
                            savedUserInfo.getUserInfo());
                    savedUserInfo.setPrincipal(null);
                }
            }
        } else {
            log.debug("User/Password found as parameter of the request");
        }

        return userIdent;
    }

    protected boolean needSessionSaving(UserIdentificationInfo userInfo) {
        String pluginName = userInfo.getAuthPluginName();

        AuthenticationPluginDescriptor desc = service.getDescriptor(pluginName);

        if (desc.getStateful()) {
            return true;
        } else {
            return desc.getNeedStartingURLSaving();
        }
    }

}
