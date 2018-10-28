/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.auth;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.ERROR_AUTHENTICATION_FAILED;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.ERROR_CONNECTION_FAILED;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.FORCE_ANONYMOUS_LOGIN;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.FORM_SUBMITTED_MARKER;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGINCONTEXT_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGIN_ERROR;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGIN_PAGE;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGIN_STATUS_CODE;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGOUT_PAGE;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.PAGE_AFTER_SWITCH;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.REDIRECT_URL;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.REQUESTED_URL;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.SECURITY_ERROR;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.SESSION_TIMEOUT;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.SSO_INITIAL_URL_REQUEST_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.START_PAGE_SAVE_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.SWITCH_USER_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.SWITCH_USER_PAGE;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.USERIDENT_KEY;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.NamingException;
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
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfoCallbackHandler;
import org.nuxeo.ecm.platform.login.PrincipalImpl;
import org.nuxeo.ecm.platform.login.TrustingLoginPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.LoginResponseHandler;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPropagator;
import org.nuxeo.ecm.platform.ui.web.auth.service.AuthenticationPluginDescriptor;
import org.nuxeo.ecm.platform.ui.web.auth.service.OpenUrlDescriptor;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.session.NuxeoHttpSessionMonitor;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.model.ComponentManager;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

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

    private static final Log log = LogFactory.getLog(NuxeoAuthenticationFilter.class);

    // protected static final String EJB_LOGIN_DOMAIN = "nuxeo-system-login";

    /**
     * @deprecated Since 8.4. Use {@link LoginScreenHelper#getStartupPagePath()} instead.
     * @see LoginScreenHelper
     */
    @Deprecated
    public static final String DEFAULT_START_PAGE = "nxstartup.faces";

    /**
     * LoginContext domain name in use by default in Nuxeo.
     */
    public static final String LOGIN_DOMAIN = "nuxeo-ecm-web";

    protected static final String XMLHTTP_REQUEST_TYPE = "XMLHttpRequest";

    protected static final String LOGIN_CATEGORY = "NuxeoAuthentication";

    protected static volatile Boolean isLoginSynchronized;

    /** Used internally as a marker. */
    protected static final Principal DIRECTORY_ERROR_PRINCIPAL = new PrincipalImpl("__DIRECTORY_ERROR__\0\0\0");

    protected static final String INDEX_JSP = "index.jsp";

    protected static final String SLASH_INDEX_JSP = "/" + INDEX_JSP;

    /** The Seam conversation id query parameter. */
    protected static final String CONVERSATION_ID = "conversationId";

    private static String anonymous;

    protected final boolean avoidReauthenticate = true;

    protected volatile PluggableAuthenticationService service;

    protected ReentrantReadWriteLock unAuthenticatedURLPrefixLock = new ReentrantReadWriteLock();

    protected List<String> unAuthenticatedURLPrefix;

    /**
     * On WebEngine (Jetty) we don't have JMS enabled so we should disable log
     */
    protected boolean byPassAuthenticationLog = false;

    /**
     * Which security domain to use
     */
    protected String securityDomain = LOGIN_DOMAIN;

    // @since 5.7
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Timer requestTimer = registry.timer(
            MetricRegistry.name("nuxeo", "web", "authentication", "requests", "count"));

    protected final Counter concurrentCount = registry.counter(
            MetricRegistry.name("nuxeo", "web", "authentication", "requests", "concurrent", "count"));

    protected final Counter concurrentMaxCount = registry.counter(
            MetricRegistry.name("nuxeo", "web", "authentication", "requests", "concurrent", "max"));

    protected final Counter loginCount = registry.counter(
            MetricRegistry.name("nuxeo", "web", "authentication", "logged-users"));

    @Override
    public void destroy() {
    }

    protected static boolean sendAuthenticationEvent(UserIdentificationInfo userInfo, String eventId, String comment) {

        LoginContext loginContext = null;
        try {
            try {
                loginContext = Framework.login();
            } catch (LoginException e) {
                log.error("Unable to log in in order to log Login event" + e.getMessage());
                return false;
            }

            EventProducer evtProducer = Framework.getService(EventProducer.class);
            NuxeoPrincipal principal = new UserPrincipal(userInfo.getUserName(), null, false, false);

            Map<String, Serializable> props = new HashMap<>();
            props.put("AuthenticationPlugin", userInfo.getAuthPluginName());
            props.put("LoginPlugin", userInfo.getLoginPluginName());
            props.put("category", LOGIN_CATEGORY);
            props.put("comment", comment);

            EventContext ctx = new UnboundEventContext(principal, props);
            evtProducer.fireEvent(ctx.newEvent(eventId));
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

    protected boolean logAuthenticationAttempt(UserIdentificationInfo userInfo, boolean success) {
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
            comment = userName + " successfully logged in using " + userInfo.getAuthPluginName() + " authentication";
            loginCount.inc();
        } else {
            eventId = "loginFailed";
            comment = userName + " failed to authenticate using " + userInfo.getAuthPluginName() + " authentication";
        }

        return sendAuthenticationEvent(userInfo, eventId, comment);
    }

    protected boolean logLogout(UserIdentificationInfo userInfo) {
        if (byPassAuthenticationLog) {
            return true;
        }
        loginCount.dec();
        String userName = userInfo.getUserName();
        if (userName == null || userName.length() == 0) {
            userName = userInfo.getToken();
        }

        String eventId = "logout";
        String comment = userName + " logged out";

        return sendAuthenticationEvent(userInfo, eventId, comment);
    }

    protected static boolean isLoginSynchronized() {
        if (isLoginSynchronized != null) {
            return isLoginSynchronized;
        }
        if (Framework.getRuntime() == null) {
            return false;
        }
        synchronized (NuxeoAuthenticationFilter.class) {
            if (isLoginSynchronized != null) {
                return isLoginSynchronized;
            }
            return isLoginSynchronized = !Boolean.parseBoolean(Framework.getProperty(
                    "org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter.isLoginNotSynchronized", "true"));
        }
    }

    protected Principal doAuthenticate(CachableUserIdentificationInfo cachableUserIdent,
            HttpServletRequest httpRequest) {

        LoginContext loginContext;
        try {
            CallbackHandler handler = service.getCallbackHandler(cachableUserIdent.getUserInfo());
            loginContext = new LoginContext(securityDomain, handler);

            if (isLoginSynchronized()) {
                synchronized (NuxeoAuthenticationFilter.class) {
                    loginContext.login();
                }
            } else {
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
            if (log.isInfoEnabled()) {
                log.info(String.format("Login failed for %s on request %s",
                        cachableUserIdent.getUserInfo().getUserName(), httpRequest.getRequestURI()));
            }
            log.debug(e, e);
            logAuthenticationAttempt(cachableUserIdent.getUserInfo(), false);
            Throwable cause = e.getCause();
            if (cause instanceof DirectoryException) {
                Throwable rootCause = ExceptionUtils.getRootCause(cause);
                if (rootCause instanceof NamingException
                        && rootCause.getMessage().contains("LDAP response read timed out")
                        || rootCause instanceof SocketException) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Exception root cause is either a NamingException with \"LDAP response read timed out\""
                                        + " or a SocketException, setting the status code to %d,"
                                        + " more relevant than an illegitimate 401.",
                                HttpServletResponse.SC_GATEWAY_TIMEOUT));
                    }
                    httpRequest.setAttribute(LOGIN_STATUS_CODE, HttpServletResponse.SC_GATEWAY_TIMEOUT);
                }
                return DIRECTORY_ERROR_PRINCIPAL;
            }
            return null;
        }

        // store login context for the time of the request
        // TODO logincontext is also stored in cachableUserIdent - it is really
        // needed to store it??
        httpRequest.setAttribute(LOGINCONTEXT_KEY, loginContext);

        // store user ident
        cachableUserIdent.setLoginContext(loginContext);
        boolean createSession = needSessionSaving(cachableUserIdent.getUserInfo());
        HttpSession session = httpRequest.getSession(createSession);
        if (session != null) {
            session.setAttribute(USERIDENT_KEY, cachableUserIdent);
        }

        service.onAuthenticatedSessionCreated(httpRequest, session, cachableUserIdent);

        return cachableUserIdent.getPrincipal();
    }

    private boolean switchUser(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String deputyLogin = (String) httpRequest.getAttribute(SWITCH_USER_KEY);
        String targetPageAfterSwitch = (String) httpRequest.getAttribute(PAGE_AFTER_SWITCH);
        if (targetPageAfterSwitch == null) {
            targetPageAfterSwitch = LoginScreenHelper.getStartupPagePath();
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

        httpRequest.getSession(false);
        service.reinitSession(httpRequest);

        CachableUserIdentificationInfo newCachableUserIdent = new CachableUserIdentificationInfo(deputyLogin,
                deputyLogin);

        newCachableUserIdent.getUserInfo().setLoginPluginName(TrustingLoginPlugin.NAME);
        newCachableUserIdent.getUserInfo().setAuthPluginName(cachableUserIdent.getUserInfo().getAuthPluginName());

        Principal principal = doAuthenticate(newCachableUserIdent, httpRequest);
        if (principal != null && principal != DIRECTORY_ERROR_PRINCIPAL) {
            NuxeoPrincipal nxUser = (NuxeoPrincipal) principal;
            if (originatingUser != null) {
                nxUser.setOriginatingUser(originatingUser);
            }
            propagateUserIdentificationInformation(cachableUserIdent);
        }

        // reinit Seam so the afterResponseComplete does not crash
        // ServletLifecycle.beginRequest(httpRequest);

        // flag redirect to avoid being caught by URLPolicy
        request.setAttribute(DISABLE_REDIRECT_REQUEST_KEY, Boolean.TRUE);
        String baseURL = service.getBaseURL(request);
        ((HttpServletResponse) response).sendRedirect(baseURL + targetPageAfterSwitch);

        return true;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final Timer.Context contextTimer = requestTimer.time();
        concurrentCount.inc();
        if (concurrentCount.getCount() > concurrentMaxCount.getCount()) {
            concurrentMaxCount.inc();
        }
        try {
            doInitIfNeeded();
            doFilterInternal(request, response, chain);
        } finally {
            ClientLoginModule.clearThreadLocalLogin();
            contextTimer.stop();
            concurrentCount.dec();
        }
    }

    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (bypassAuth((HttpServletRequest) request)) {
            chain.doFilter(request, response);
            return;
        }

        String tokenPage = getRequestedPage(request);
        if (tokenPage.equals(SWITCH_USER_PAGE)) {
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

        NuxeoAuthenticationPropagator.CleanupCallback propagatedAuthCb = null;

        String forceAnonymousLoginParam = httpRequest.getParameter(FORCE_ANONYMOUS_LOGIN);
        boolean forceAnonymousLogin = Boolean.parseBoolean(forceAnonymousLoginParam);

        try {
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

                if (cachableUserIdent != null && cachableUserIdent.getUserInfo() != null) {
                    if (cachableUserIdent.getUserInfo().getUserName().equals(getAnonymousId())) {
                        if (forceAnonymousLogin) {
                            cachableUserIdent = null;
                        }
                    }

                    if (service.needResetLogin(request)) {
                        HttpSession session = httpRequest.getSession(false);
                        if (session != null) {
                            session.removeAttribute(USERIDENT_KEY);
                        }
                        // first propagate the login because invalidation may
                        // require
                        // an authenticated session
                        propagatedAuthCb = service.propagateUserIdentificationInformation(cachableUserIdent);
                        // invalidate Session !
                        try {
                            service.invalidateSession(request);
                        } finally {
                            if (propagatedAuthCb != null) {
                                propagatedAuthCb.cleanup();
                                propagatedAuthCb = null;
                            }
                        }
                        // TODO perform logout?
                        cachableUserIdent = null;
                    }
                }

                // identity found in cache
                if (cachableUserIdent != null && cachableUserIdent.getUserInfo() != null) {
                    log.debug("userIdent found in cache, get the Principal from it without reloggin");

                    NuxeoHttpSessionMonitor.instance().updateEntry(httpRequest);

                    principal = cachableUserIdent.getPrincipal();
                    log.debug("Principal = " + principal.getName());
                    propagatedAuthCb = service.propagateUserIdentificationInformation(cachableUserIdent);

                    String requestedPage = getRequestedPage(httpRequest);
                    if (LOGOUT_PAGE.equals(requestedPage)) {
                        boolean redirected = handleLogout(request, response, cachableUserIdent);
                        cachableUserIdent = null;
                        principal = null;
                        if (redirected && httpRequest.getParameter(FORM_SUBMITTED_MARKER) == null) {
                            return;
                        }
                    } else if (LOGIN_PAGE.equals(requestedPage)) {
                        if (handleLogin(httpRequest, httpResponse)) {
                            return;
                        }
                    } else {
                        targetPageURL = getSavedRequestedURL(httpRequest, httpResponse);
                    }
                }

                // identity not found in cache or reseted by logout
                if (cachableUserIdent == null || cachableUserIdent.getUserInfo() == null) {
                    UserIdentificationInfo userIdent = handleRetrieveIdentity(httpRequest, httpResponse);
                    if (userIdent != null && userIdent.containsValidIdentity()
                            && userIdent.getUserName().equals(getAnonymousId())) {
                        if (forceAnonymousLogin) {
                            userIdent = null;
                        }
                    }
                    if ((userIdent == null || !userIdent.containsValidIdentity()) && !bypassAuth(httpRequest)) {
                        boolean res = handleLoginPrompt(httpRequest, httpResponse);
                        if (res) {
                            return;
                        }
                    } else {
                        String redirectUrl = VirtualHostHelper.getRedirectUrl(httpRequest);
                        HttpSession session = httpRequest.getSession(false);
                        if (session != null) {
                            session.setAttribute(REDIRECT_URL, redirectUrl);
                        }
                        // restore saved Starting page
                        targetPageURL = getSavedRequestedURL(httpRequest, httpResponse);
                    }
                    if (userIdent != null && userIdent.containsValidIdentity()) {
                        // do the authentication
                        cachableUserIdent = new CachableUserIdentificationInfo(userIdent);
                        principal = doAuthenticate(cachableUserIdent, httpRequest);
                        if (principal != null && principal != DIRECTORY_ERROR_PRINCIPAL) {
                            // Do the propagation too ????
                            propagatedAuthCb = service.propagateUserIdentificationInformation(cachableUserIdent);
                            // setPrincipalToSession(httpRequest, principal);
                            // check if the current authenticator is a
                            // LoginResponseHandler
                            NuxeoAuthenticationPlugin plugin = getAuthenticator(cachableUserIdent);
                            if (plugin instanceof LoginResponseHandler) {
                                // call the extended error handler
                                if (((LoginResponseHandler) plugin).onSuccess((HttpServletRequest) request,
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
                                if (((LoginResponseHandler) plugin).onError((HttpServletRequest) request,
                                        (HttpServletResponse) response)) {
                                    return;
                                }
                            } else {
                                // use the old method
                                String err = principal == DIRECTORY_ERROR_PRINCIPAL ? ERROR_CONNECTION_FAILED
                                        : ERROR_AUTHENTICATION_FAILED;
                                httpRequest.setAttribute(LOGIN_ERROR, err);
                                boolean res = handleLoginPrompt(httpRequest, httpResponse);
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
                        // In case of a download redirection, the base url is already contained in the target
                        String url = targetPageURL.startsWith(baseURL) ? targetPageURL : baseURL + targetPageURL;
                        httpResponse.sendRedirect(url);
                        return;
                    }

                } else {
                    // simply continue request
                    chain.doFilter(new NuxeoSecuredRequestWrapper(httpRequest, principal), response);
                }
            } else {
                chain.doFilter(request, response);
            }
        } finally {
            if (propagatedAuthCb != null) {
                propagatedAuthCb.cleanup();
            }
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

    public NuxeoAuthenticationPlugin getAuthenticator(CachableUserIdentificationInfo ci) {
        String key = ci.getUserInfo().getAuthPluginName();
        if (key != null) {
            NuxeoAuthenticationPlugin authPlugin = service.getPlugin(key);
            return authPlugin;
        }
        return null;
    }

    protected static CachableUserIdentificationInfo retrieveIdentityFromCache(HttpServletRequest httpRequest) {

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            CachableUserIdentificationInfo cachableUserInfo = (CachableUserIdentificationInfo) session.getAttribute(
                    USERIDENT_KEY);
            if (cachableUserInfo != null) {
                return cachableUserInfo;
            }
        }

        return null;
    }

    private String getAnonymousId() throws ServletException {
        if (anonymous == null) {
            anonymous = Framework.getService(UserManager.class).getAnonymousUserId();
        }
        return anonymous;
    }

    protected void doInitIfNeeded() throws ServletException {
        if (service == null && Framework.getRuntime() != null) {
            synchronized (this) {
                if (service != null) {
                    return;
                }
                PluggableAuthenticationService svc = (PluggableAuthenticationService) Framework.getRuntime()
                                                                                               .getComponent(
                                                                                                       PluggableAuthenticationService.NAME);
                new ComponentManager.Listener() {
                    // nullify service field if components are restarting
                    @Override
                    public void beforeStart(ComponentManager mgr, boolean isResume) {
                        service = null;
                        uninstall();
                    }
                }.install();
                service = svc;
            }
        }
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        String val = config.getInitParameter("byPassAuthenticationLog");
        if (val != null && Boolean.parseBoolean(val)) {
            byPassAuthenticationLog = true;
        }
        val = config.getInitParameter("securityDomain");
        if (val != null) {
            securityDomain = val;
        }

    }

    /**
     * Save requested URL before redirecting to login form.
     * <p>
     * Returns true if target url is a valid startup page.
     */
    public boolean saveRequestedURLBeforeRedirect(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        final boolean hasRequestedSessionId = !StringUtils.isBlank(httpRequest.getRequestedSessionId());

        HttpSession session = httpRequest.getSession(false);
        final boolean isTimeout = session == null && hasRequestedSessionId;

        if (!httpResponse.isCommitted()) {
            session = httpRequest.getSession(true);
        }

        if (session == null) {
            return false;
        }

        String requestPage;
        boolean requestPageInParams = false;
        if (httpRequest.getParameter(REQUESTED_URL) != null) {
            requestPageInParams = true;
            requestPage = httpRequest.getParameter(REQUESTED_URL);
        } else {
            requestPage = getRequestedUrl(httpRequest);
        }

        if (requestPage == null) {
            return false;
        }

        // add a flag to tell that the Session looks like having timed out
        if (isTimeout && !requestPage.equals(LoginScreenHelper.getStartupPagePath())) {
            session.setAttribute(SESSION_TIMEOUT, Boolean.TRUE);
        } else {
            session.removeAttribute(SESSION_TIMEOUT);
        }

        // avoid saving to session is start page is not valid or if it's
        // already in the request params
        if (isStartPageValid(requestPage)) {
            if (!requestPageInParams) {
                session.setAttribute(START_PAGE_SAVE_KEY, requestPage);
            }
            return true;
        }

        // avoid redirect if not useful
        for (String startupPagePath : LoginScreenHelper.getStartupPagePaths()) {
            if (requestPage.startsWith(startupPagePath)
                    && LoginScreenHelper.getStartupPagePath().equals(startupPagePath)) {
                return true;
            }
        }

        return false;
    }

    /**
     * The requested URL is like the requested page but also includes the query string, except without conversation id.
     */
    public static String getRequestedUrl(HttpServletRequest request) {
        String path = getRequestedPage(request);
        String qs = request.getQueryString();
        if (StringUtils.isNotEmpty(qs)) {
            // strip conversation id
            if (qs.contains(CONVERSATION_ID)) {
                List<NameValuePair> list = URLEncodedUtils.parse(qs, UTF_8);
                if (list.removeIf(pair -> pair.getName().equals(CONVERSATION_ID))) {
                    qs = URLEncodedUtils.format(list, UTF_8);
                }
            }
            if (!qs.isEmpty()) {
                path = path + '?' + qs;
            }
        }
        return path;
    }

    protected static String getSavedRequestedURL(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        HttpSession session = httpRequest.getSession(false);
        String requestedPage = httpRequest.getParameter(REQUESTED_URL);
        if (StringUtils.isBlank(requestedPage)) {
            // retrieve from session
            if (session != null) {
                requestedPage = (String) session.getAttribute(START_PAGE_SAVE_KEY);
            }

            // retrieve from SSO cookies
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (SSO_INITIAL_URL_REQUEST_KEY.equals(cookie.getName())) {
                        requestedPage = cookie.getValue();
                        cookie.setPath("/");
                        // enforce cookie removal
                        cookie.setMaxAge(0);
                        httpResponse.addCookie(cookie);
                    }
                }
            }
        }

        if (requestedPage != null) {
            // retrieve URL fragment from cookie
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (NXAuthConstants.START_PAGE_FRAGMENT_KEY.equals(cookie.getName())) {
                        try {
                            requestedPage = UriBuilder.fromUri(requestedPage)
                                                      .fragment(URLDecoder.decode(cookie.getValue(), "UTF-8"))
                                                      .build()
                                                      .toString();
                        } catch (UnsupportedEncodingException e) {
                            log.error("Failed to decode start page url fragment", e);
                        }
                        // enforce cookie removal
                        cookie.setMaxAge(0);
                        httpResponse.addCookie(cookie);
                    }
                }
            }
        }

        // clean up session
        if (session != null) {
            session.removeAttribute(START_PAGE_SAVE_KEY);
        }

        return requestedPage;
    }

    protected boolean isStartPageValid(String startPage) {
        if (startPage == null) {
            return false;
        }
        try {
            // Sometimes, the service is not initialized at startup
            doInitIfNeeded();
        } catch (ServletException e) {
            return false;
        }
        for (String prefix : service.getStartURLPatterns()) {
            if (startPage.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    protected boolean handleLogout(ServletRequest request, ServletResponse response,
            CachableUserIdentificationInfo cachedUserInfo) throws ServletException {
        logLogout(cachedUserInfo.getUserInfo());

        request.setAttribute(DISABLE_REDIRECT_REQUEST_KEY, Boolean.TRUE);
        Map<String, String> parameters = new HashMap<>();
        String securityError = request.getParameter(SECURITY_ERROR);
        if (securityError != null) {
            parameters.put(SECURITY_ERROR, securityError);
        }
        if (cachedUserInfo.getPrincipal().getName().equals(getAnonymousId())) {
            parameters.put(FORCE_ANONYMOUS_LOGIN, "true");
        }
        String requestedUrl = request.getParameter(REQUESTED_URL);
        if (requestedUrl != null) {
            parameters.put(REQUESTED_URL, requestedUrl);
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
            redirected = Boolean.TRUE.equals(
                    logoutPlugin.handleLogout((HttpServletRequest) request, (HttpServletResponse) response));
        }

        // invalidate Session !
        service.invalidateSession(request);

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (!redirected && !XMLHTTP_REQUEST_TYPE.equalsIgnoreCase(httpRequest.getHeader("X-Requested-With"))) {
            String baseURL = service.getBaseURL(request);
            try {
                String url = baseURL + LoginScreenHelper.getStartupPagePath();
                url = URIUtils.addParametersToURIQuery(url, parameters);
                ((HttpServletResponse) response).sendRedirect(url);
                redirected = true;
            } catch (IOException e) {
                log.error("Unable to redirect to default start page after logout : " + e.getMessage());
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
    protected void propagateUserIdentificationInformation(CachableUserIdentificationInfo cachableUserIdent) {
        service.propagateUserIdentificationInformation(cachableUserIdent);
    }

    // Plugin API
    protected void initUnAuthenticatedURLPrefix() {
        // gather unAuthenticated URLs
        unAuthenticatedURLPrefix = new ArrayList<>();
        for (String pluginName : service.getAuthChain()) {
            NuxeoAuthenticationPlugin plugin = service.getPlugin(pluginName);
            if (plugin == null) {
                throw new NullPointerException("Could not find plugin for name '" + pluginName + "'");
            }
            List<String> prefix = plugin.getUnAuthenticatedURLPrefix();
            if (prefix != null && !prefix.isEmpty()) {
                unAuthenticatedURLPrefix.addAll(prefix);
            }
        }
    }

    protected boolean bypassAuth(HttpServletRequest httpRequest) {
        if (unAuthenticatedURLPrefix == null) {
            try {
                unAuthenticatedURLPrefixLock.writeLock().lock();
                // late init to allow plugins registered after this filter init
                initUnAuthenticatedURLPrefix();
            } finally {
                unAuthenticatedURLPrefixLock.writeLock().unlock();
            }
        }

        try {
            unAuthenticatedURLPrefixLock.readLock().lock();
            String requestPage = getRequestedPage(httpRequest);
            for (String prefix : unAuthenticatedURLPrefix) {
                if (requestPage.startsWith(prefix)) {
                    return true;
                }
            }
        } finally {
            unAuthenticatedURLPrefixLock.readLock().unlock();
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
        String path = httpRequest.getServletPath(); // use decoded and normalized servlet path
        if (path.endsWith(SLASH_INDEX_JSP)) {
            // the welcome file (index.jsp) is present in the servlet path
            if (!httpRequest.getRequestURI().contains(SLASH_INDEX_JSP)) {
                // remove it if it was not specified explicitly
                path = path.substring(0, path.length() - INDEX_JSP.length());
            }
        }
        String info = httpRequest.getPathInfo();
        if (info != null) {
            path = path + info;
        }
        if (!path.isEmpty()) {
            path = path.substring(1); // strip initial /
        }
        return path;
    }

    protected boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        // A specific auth chain may prevent the filter to relay to a login prompt.
        if (!service.doHandlePrompt(httpRequest)) {
            buildUnauthorizedResponse(httpRequest, httpResponse);
            return true;
        }

        return handleLogin(httpRequest, httpResponse);

    }

    private boolean handleLogin(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String baseURL = service.getBaseURL(httpRequest);

        // go through plugins to get UserIndentity
        for (String pluginName : service.getAuthChain(httpRequest)) {
            NuxeoAuthenticationPlugin plugin = service.getPlugin(pluginName);
            AuthenticationPluginDescriptor descriptor = service.getDescriptor(pluginName);
            if (plugin == null) {
                throw new NullPointerException("Could not find plugin for name '" + pluginName + "'");
            }
            if (Boolean.TRUE.equals(plugin.needLoginPrompt(httpRequest))) {
                if (descriptor.getNeedStartingURLSaving()) {
                    saveRequestedURLBeforeRedirect(httpRequest, httpResponse);
                }

                HttpServletResponse response = new HttpServletResponseWrapper(httpResponse) {
                    @Override
                    public void sendRedirect(String location) throws IOException {
                        HttpServletResponse response = (HttpServletResponse) getResponse();
                        StringBuilder sb = new StringBuilder();
                        sb.append("<script type=\"text/javascript\">\n");
                        sb.append("document.cookie = '" + NXAuthConstants.START_PAGE_FRAGMENT_KEY
                                + "=' + encodeURIComponent(window.location.hash.substring(1) || '') + '; path=/';\n");
                        sb.append("window.location = '" + location + "';\n");
                        sb.append("</script>");
                        String script = sb.toString();

                        response.setStatus(SC_UNAUTHORIZED);
                        response.setContentType("text/html;charset=UTF-8");
                        response.setContentLength(script.length());
                        response.getWriter().write(script);
                    }
                };
                return Boolean.TRUE.equals(plugin.handleLoginPrompt(httpRequest, response, baseURL));
            }
        }

        log.warn("No auth plugin can be found to do the Login Prompt");
        return false;
    }

    private void buildUnauthorizedResponse(HttpServletRequest req, HttpServletResponse resp) {

        try {
            String loginUrl = VirtualHostHelper.getBaseURL(req) + LOGIN_PAGE;
            resp.addHeader("Location", loginUrl);
            resp.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
            resp.getWriter().write("Please log in at: " + loginUrl);
        } catch (IOException e) {
            log.error("Unable to write login page on unauthorized response", e);
        }
    }

    protected UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        UserIdentificationInfo userIdent = null;

        // go through plugins to get UserIdentity
        for (String pluginName : service.getAuthChain(httpRequest)) {
            NuxeoAuthenticationPlugin plugin = service.getPlugin(pluginName);
            if (plugin != null) {
                log.debug("Trying to retrieve userIdentification using plugin " + pluginName);
                userIdent = plugin.handleRetrieveIdentity(httpRequest, httpResponse);
                if (userIdent != null && userIdent.containsValidIdentity()) {
                    // fill information for the Login module
                    userIdent.setAuthPluginName(pluginName);

                    // get the target login module
                    String loginModulePlugin = service.getDescriptor(pluginName).getLoginModulePlugin();
                    userIdent.setLoginPluginName(loginModulePlugin);

                    // get the additional parameters
                    Map<String, String> parameters = service.getDescriptor(pluginName).getParameters();
                    if (userIdent.getLoginParameters() != null) {
                        // keep existing parameters set by the auth plugin
                        if (parameters == null) {
                            parameters = new HashMap<>();
                        }
                        parameters.putAll(userIdent.getLoginParameters());
                    }
                    userIdent.setLoginParameters(parameters);

                    break;
                }
            } else {
                log.error("Auth plugin " + pluginName + " can not be retrieved from service");
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
                    log.debug("Found User identity in cache :" + savedUserInfo.getUserInfo().getUserName());
                    userIdent = new UserIdentificationInfo(savedUserInfo.getUserInfo());
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

    /**
     * Does a forced login as the given user. Bypasses all authentication checks.
     *
     * @param username the user name
     * @return the login context, which MUST be used for logout in a {@code finally} block
     * @throws LoginException
     */
    public static LoginContext loginAs(String username) throws LoginException {
        UserIdentificationInfo userIdent = new UserIdentificationInfo(username, "");
        userIdent.setLoginPluginName(TrustingLoginPlugin.NAME);
        PluggableAuthenticationService authService = (PluggableAuthenticationService) Framework.getRuntime()
                                                                                               .getComponent(
                                                                                                       PluggableAuthenticationService.NAME);
        CallbackHandler callbackHandler;
        if (authService != null) {
            callbackHandler = authService.getCallbackHandler(userIdent);
        } else {
            callbackHandler = new UserIdentificationInfoCallbackHandler(userIdent);
        }
        LoginContext loginContext = new LoginContext(LOGIN_DOMAIN, callbackHandler);

        if (isLoginSynchronized()) {
            synchronized (NuxeoAuthenticationFilter.class) {
                loginContext.login();
            }
        } else {
            loginContext.login();
        }
        return loginContext;
    }

}
