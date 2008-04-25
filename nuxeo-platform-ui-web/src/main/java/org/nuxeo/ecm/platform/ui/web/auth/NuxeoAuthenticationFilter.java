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
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
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
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Seam;
import org.jboss.seam.contexts.ContextAdaptor;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.core.Manager;
import org.jboss.security.SecurityAssociation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.ui.web.auth.service.AuthenticationPluginDescriptor;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.ui.web.rest.FancyURLRequestWrapper;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;

/**
 * Servlet filter handling Nuxeo authentication (JAAS + EJB).
 * <p>
 * Also handles logout.
 *
 * @author Thierry Delprat
 * @author Bogdan Stefanescu
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 */
public class NuxeoAuthenticationFilter implements Filter {

    // protected static final String EJB_LOGIN_DOMAIN = "nuxeo-system-login";

    protected static final String LOGIN_DOMAIN = "nuxeo-ecm-web";

    public static final String START_PAGE_SAVE_KEY = "Nuxeo5_Start_Page";

    public static final String DEFAULT_START_PAGE = "nxstartup.faces";

    protected static final String LOGIN_JMS_CATEGORY = "NuxeoAuthentication";

    private static final Log log = LogFactory.getLog(NuxeoAuthenticationFilter.class);

    protected final boolean avoidReauthenticate = true;

    protected PluggableAuthenticationService service;

    protected List<String> unAuthenticatedURLPrefix;

    protected static List<String> validStartURLs;

    public void destroy() {
    }

    protected static boolean sendAuthenticationEvent(
            UserIdentificationInfo userInfo, String eventId, String comment) {

        LoginContext loginContext = null;
        try {
            try {
                loginContext = Framework.login();
            } catch (LoginException e) {
                log.error("Unable to log in in order to log Login event" +
                        e.getMessage());
                return false;
            }

            DocumentMessageProducer producer;
            try {
                producer = Framework.getService(DocumentMessageProducer.class);
            } catch (Exception e) {
                log.error("Unable to get JMS message producer: " +
                        e.getMessage());
                return false;
            }

            // XXX : Catch all errors to be sure to logout

            Map<String, Serializable> props = new HashMap<String, Serializable>();
            DocumentModel dm = new DocumentMessageImpl();

            props.put("AuthenticationPlugin", userInfo.getAuthPluginName());
            props.put("LoginPlugin", userInfo.getLoginPluginName());

            Principal systemPrincipal = (Principal) loginContext.getSubject().getPrincipals().toArray()[0];

            CoreEvent event = new CoreEventImpl(eventId, dm, props,
                    systemPrincipal, LOGIN_JMS_CATEGORY, comment);

            DocumentMessage msg = new DocumentMessageImpl(dm, event);

            producer.produce(msg);

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

        String userName = userInfo.getUserName();
        if (userName == null || userName.length() == 0) {
            userName = userInfo.getToken();
        }

        String eventId;
        String comment;
        if (success) {
            eventId = "loginSuccess";
            comment = userName + " successfully logged in using " +
                    userInfo.getAuthPluginName() + "Authentication";
        } else {
            eventId = "loginFailed";
            comment = userName + " failed to authenticate using " +
                    userInfo.getAuthPluginName() + "Authentication";
        }

        return sendAuthenticationEvent(userInfo, eventId, comment);
    }

    protected boolean logLogout(UserIdentificationInfo userInfo) {
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
            // handler = new
            // JbossSecurityPropagationCallbackHandler(cachableUserIdent.getUserInfo());
            // handler = new
            // UserIdentificationInfoCallbackHandler(cachableUserIdent.getUserInfo());
            CallbackHandler handler = new JBossUserIdentificationInfoCallbackHandler(
                    cachableUserIdent.getUserInfo());

            loginContext = new LoginContext(LOGIN_DOMAIN, handler);
            loginContext.login();

            Principal principal = (Principal) loginContext.getSubject().getPrincipals().toArray()[0];
            cachableUserIdent.setPrincipal(principal);
            cachableUserIdent.setAlreadyAuthenticated(Boolean.TRUE);
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
        httpRequest.setAttribute(NXAuthContants.LOGINCONTEXT_KEY, loginContext);

        // store user ident
        cachableUserIdent.setLoginContext(loginContext);
        boolean createSession = needSessionSaving(cachableUserIdent.getUserInfo());
        HttpSession session = httpRequest.getSession(createSession);
        if (session != null) {
            session.setAttribute(NXAuthContants.USERIDENT_KEY,
                    cachableUserIdent);
        }

        return cachableUserIdent.getPrincipal();
    }

    private boolean switchUser(ServletRequest request,
            ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String deputyLogin = (String) httpRequest.getAttribute(NXAuthContants.SWITCH_USER_KEY);

        if (deputyLogin == null) {
            return false;
        }

        CachableUserIdentificationInfo cachableUserIdent = retrieveIdentityFromCache(httpRequest);
        String originatingUser = cachableUserIdent.getUserInfo().getUserName();
        try {
            cachableUserIdent.getLoginContext().logout();
        } catch (LoginException e1) {
            log.error("Error while logout from main identity :" +
                    e1.getMessage());
        }

        // destroy session
        // because of Seam Phase Listener we can't use Seam.invalidateSession()
        // because the session would be invalidated at the end of the request !
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Manager.instance().endConversation(true);
            Manager.instance().endRequest(ContextAdaptor.getSession(session));
            Lifecycle.endRequest(session);
            Lifecycle.setServletRequest(null);
            Lifecycle.setPhaseId(null);

            session = httpRequest.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
        // create new one
        session = httpRequest.getSession(true);

        CachableUserIdentificationInfo newCachableUserIdent = new CachableUserIdentificationInfo(
                deputyLogin, deputyLogin);

        newCachableUserIdent.getUserInfo().setLoginPluginName("Trusting_LM");
        newCachableUserIdent.getUserInfo().setAuthPluginName(
                cachableUserIdent.getUserInfo().getAuthPluginName());

        Principal principal = doAuthenticate(newCachableUserIdent, httpRequest);
        if (principal != null) {

            NuxeoPrincipal nxUser = (NuxeoPrincipal) principal;
            nxUser.setOriginatingUser(originatingUser);
            propagateUserIdentificationInformation(cachableUserIdent);
        }

        // reinit Seam so the afterResponseComplete does not crash
        Lifecycle.beginRequest(session.getServletContext(), session,
                httpRequest);

        // flag redirect to not be catched by URLPolicy
        request.setAttribute(URLPolicyService.DISABLE_REDIRECT_REQUEST_KEY,
                Boolean.TRUE);
        String baseURL = BaseURL.getBaseURL(request);
        ((HttpServletResponse) response).sendRedirect(baseURL +
                DEFAULT_START_PAGE);

        return true;
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        String tokenPage = getRequestedPage(request);
        if (tokenPage.equals(NXAuthContants.SWITCH_USER_PAGE)) {
            boolean result = switchUser(request, response, chain);
            if (result) {
                return;
            }
        }

        if (request instanceof NuxeoSecuredRequestWrapper) {
            log.debug("ReEntering Nuxeo Authentication Filter ... exiting directly");
            chain.doFilter(request, response);
            return;
        } else if (request instanceof FancyURLRequestWrapper) {
            log.debug("ReEntering Nuxeo Authentication Filter after URL rewrite ... exiting directly");
            chain.doFilter(request, response);
            return;
        } else {
            log.debug("Entering Nuxeo Authentication Filter");
        }

        if (avoidReauthenticate) {
            log.debug("Principal cache is activated");
        } else {
            log.debug("Principal cache is NOT activated");
        }

        String targetPageURL = null;
        CachableUserIdentificationInfo cachableUserIdent = null;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Principal principal = httpRequest.getUserPrincipal();

        if (principal == null) {
            log.debug("Principal not found inside Request via getUserPrincipal");
            // need to authenticate !

            // retrieve user & password
            if (avoidReauthenticate) {
                log.debug("Try getting authentication from cache");
                cachableUserIdent = retrieveIdentityFromCache(httpRequest);
            }

            if (cachableUserIdent == null ||
                    cachableUserIdent.getUserInfo() == null) {
                UserIdentificationInfo userIdent = handleRetrieveIdentity(
                        httpRequest, httpResponse);
                if ((userIdent == null || !userIdent.containsValidIdentity()) &&
                        !bypassAuth(httpRequest)) {
                    boolean res = handleLoginPrompt(httpRequest, httpResponse);
                    if (res) {
                        return;
                    }
                } else {
                    // restore saved Starting page
                    targetPageURL = getSavedRequestedURL(httpRequest);
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
                    } else {

                        httpRequest.setAttribute(NXAuthContants.LOGIN_ERROR,
                                "authentication.failed");
                        boolean res = handleLoginPrompt(httpRequest,
                                httpResponse);
                        if (res) {
                            return;
                        }
                    }

                }
            } else {
                log.debug("userIdent found in cache, get the Principal from it without reloggin");

                String requestedPage = getRequestedPage(httpRequest);
                if (requestedPage.equals(NXAuthContants.LOGOUT_PAGE)) {
                    boolean redirected = handleLogout(request, response,
                            cachableUserIdent);
                    if (redirected) {
                        return;
                    }
                }

                principal = cachableUserIdent.getPrincipal();
                log.debug("Principal = " + principal.getName());
                propagateUserIdentificationInformation(cachableUserIdent);
            }
        }

        if (principal != null) {
            if (targetPageURL != null) {
                // forward to target page
                String baseURL = BaseURL.getBaseURL(httpRequest);

                // httpRequest.getRequestDispatcher(targetPageURL).forward(new
                // NuxeoSecuredRequestWrapper(httpRequest, principal),
                // response);
                httpResponse.sendRedirect(baseURL + targetPageURL);
                return;
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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        log.debug("Exit Nuxeo Authentication filter");
    }

    protected static CachableUserIdentificationInfo retrieveIdentityFromCache(
            HttpServletRequest httpRequest) {

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            CachableUserIdentificationInfo cachableUserInfo = (CachableUserIdentificationInfo) session.getAttribute(NXAuthContants.USERIDENT_KEY);
            if (cachableUserInfo != null) {
                return cachableUserInfo;
            }
        }

        return null;
    }

    public void init(FilterConfig config) throws ServletException {

        service = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                PluggableAuthenticationService.NAME);
        if (service == null) {
            log.error("Unable to get Service " +
                    PluggableAuthenticationService.NAME);
            throw new ServletException(
                    "Can't initialize Nuxeo Pluggable Authentication Service");
        }

        // gather unAuthenticated URLs
        unAuthenticatedURLPrefix = new ArrayList<String>();
        for (String pluginName : service.getAuthChain()) {
            NuxeoAuthenticationPlugin plugin = service.getPlugin(pluginName);
            List<String> prefix = plugin.getUnAuthenticatedURLPrefix();
            if (prefix != null && !prefix.isEmpty()) {
                unAuthenticatedURLPrefix.addAll(prefix);
            }
        }
        validStartURLs = service.getStartURLPatterns();
    }

    /**
     * Save requested URL before redirecting to login form.
     *
     * Returns true if target url is a valid startup page.
     */
    public static boolean saveRequestedURLBeforeRedirect(
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

        // avoid redirect if not usefull
        if (requestPage.equals(DEFAULT_START_PAGE)) {
            return true;
        }

        if (isStartPageValid(requestPage)) {
            session.setAttribute(START_PAGE_SAVE_KEY, requestPage);
            return true;
        }

        return false;
    }

    protected static String getSavedRequestedURL(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            return null;
        }
        String requestedPage = (String) session.getAttribute(START_PAGE_SAVE_KEY);
        if (requestedPage == null) {
            return null;
        }

        // clean up session
        session.removeAttribute(START_PAGE_SAVE_KEY);

        return requestedPage;
    }

    protected static boolean isStartPageValid(String startPage) {
        for (String prefix : validStartURLs) {
            if (startPage.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    protected boolean handleLogout(ServletRequest request,
            ServletResponse response,
            CachableUserIdentificationInfo cachedUserInfo) {
        logLogout(cachedUserInfo.getUserInfo());

        // invalidate Seam Session !
        try {
            Seam.invalidateSession();
        } catch (IllegalStateException e) {
            // invalidate by hand
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }

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

        if (!redirected) {
            String baseURL = BaseURL.getBaseURL(request);
            try {
                ((HttpServletResponse) response).sendRedirect(baseURL +
                        DEFAULT_START_PAGE);
                redirected = true;
            } catch (IOException e) {
                log.error("Unable to redirect to default start page after logout : " +
                        e.getMessage());
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

    protected static void propagateUserIdentificationInformation(
            CachableUserIdentificationInfo cachableUserIdent) {

        // JBoss specific implementation

        // need to transfer principal info onto calling thread...
        // this is normally done by ClientLoginModule, but in this
        // case we don't do a re-authentication.

        UserIdentificationInfo userInfo = cachableUserIdent.getUserInfo();

        final Object password = userInfo.getPassword().toCharArray();
        final Object cred = userInfo;
        final boolean useLP = userInfo.getLoginPluginName() != null;
        final Principal prin = cachableUserIdent.getPrincipal();
        final Subject subj = cachableUserIdent.getLoginContext().getSubject();

        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                if (useLP) {
                    SecurityAssociation.pushSubjectContext(subj, prin, cred);
                } else {
                    SecurityAssociation.pushSubjectContext(subj, prin, password);
                }
                return null;
            }
        });

    }

    // Plugin API

    protected boolean bypassAuth(HttpServletRequest httpRequest) {
        String requestPage = getRequestedPage(httpRequest);
        for (String prefix : unAuthenticatedURLPrefix) {
            if (requestPage.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    protected static String getRequestedPage(ServletRequest request) {
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

        String baseURL = BaseURL.getBaseURL(httpRequest);

        // go through plugins to get UserIndentity
        for (String pluginName : service.getAuthChain()) {
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

        log.error("No auth plugin can be found to do the Login Prompt");
        return false;
    }

    protected UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        UserIdentificationInfo userIdent = null;

        // go through plugins to get UserIndentity
        for (String pluginName : service.getAuthChain()) {
            NuxeoAuthenticationPlugin plugin = service.getPlugin(pluginName);
            if (plugin != null) {
                log.debug("Trying to retrieve userIndetification using plugin " +
                        pluginName);
                userIdent = plugin.handleRetrieveIdentity(httpRequest,
                        httpResponse);
                if (userIdent != null && userIdent.containsValidIdentity()) {
                    // fill information for the Login module
                    userIdent.setAuthPluginName(pluginName);

                    // get the target login module
                    String loginModulePlugin = service.getDescriptor(pluginName).getLoginModulePlugin();
                    userIdent.setLoginPluginName(loginModulePlugin);

                    // get the additionnal parameters
                    Map<String, String> parameters = service.getDescriptor(
                            pluginName).getParameters();
                    userIdent.setLoginParameters(parameters);

                    break;
                }
            } else {
                log.error("Auth plugin " + pluginName +
                        " can not be retrieved from service");
            }
        }

        // Fall back to cache (used only when avoidReautenticated=false)
        if (userIdent == null || !userIdent.containsValidIdentity()) {
            log.debug("user/password not found in request, try into identity cache");
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                CachableUserIdentificationInfo savedUserInfo = retrieveIdentityFromCache(httpRequest);
                if (savedUserInfo != null) {
                    log.debug("Found User identity in cache :" +
                            savedUserInfo.getUserInfo().getUserName() + '/' +
                            savedUserInfo.getUserInfo().getPassword());
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

        if (desc.getStatefull()) {
            return true;
        } else {
            return desc.getNeedStartingURLSaving();
        }
    }

}
