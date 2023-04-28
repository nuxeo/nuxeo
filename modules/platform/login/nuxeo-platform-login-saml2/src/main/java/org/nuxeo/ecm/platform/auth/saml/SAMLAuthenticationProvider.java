/*
 * (C) Copyright 2014-2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.getSAMLHttpCookie;
import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.getSAMLSessionCookie;
import static org.nuxeo.ecm.platform.auth.saml.SAMLUtils.setLoginError;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGIN_ERROR;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.auth.saml.processor.SAMLProcessorFactory;
import org.nuxeo.ecm.platform.auth.saml.processor.SLOOutboundProcessor;
import org.nuxeo.ecm.platform.auth.saml.processor.WebSSOOutboundProcessor;
import org.nuxeo.ecm.platform.auth.saml.user.AbstractUserResolver;
import org.nuxeo.ecm.platform.auth.saml.user.EmailBasedUserResolver;
import org.nuxeo.ecm.platform.auth.saml.user.UserMapperBasedResolver;
import org.nuxeo.ecm.platform.auth.saml.user.UserResolver;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLinkComputer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.usermapper.service.UserMapperService;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.xml.config.GlobalParserPoolInitializer;
import org.opensaml.xmlsec.config.DecryptionParserPoolInitializer;
import org.opensaml.xmlsec.config.GlobalAlgorithmRegistryInitializer;

/**
 * A SAML2 authentication provider.
 *
 * @since 6.0
 */
public class SAMLAuthenticationProvider
        implements NuxeoAuthenticationPlugin, LoginProviderLinkComputer, NuxeoAuthenticationPluginLogoutExtension {

    private static final Logger log = LogManager.getLogger(SAMLAuthenticationProvider.class);

    /*
     * @since 11.2
     */
    public static final String ERROR_PAGE = "/saml/error.jsp";

    /*
     * @since 11.2
     */
    public static final String ERROR_AUTH = "error.saml.auth";

    /*
     * @since 11.2
     */
    public static final String ERROR_USER = "error.saml.userMapping";

    // User Resolver
    protected static final Class<? extends UserResolver> DEFAULT_USER_RESOLVER_CLASS = EmailBasedUserResolver.class;

    protected static final Class<? extends UserResolver> USERMAPPER_USER_RESOLVER_CLASS = UserMapperBasedResolver.class;

    protected UserResolver userResolver;

    /** @since 2023.0 */
    protected SAMLProcessorFactory processorFactory;

    @Override
    public void initPlugin(Map<String, String> parameters) {

        // Initialize the User Resolver
        String userResolverClassname = parameters.get("userResolverClass");
        Class<? extends UserResolver> userResolverClass = null;
        if (isBlank(userResolverClassname)) {
            UserMapperService ums = Framework.getService(UserMapperService.class);
            if (ums != null) {
                userResolverClass = USERMAPPER_USER_RESOLVER_CLASS;
            } else {
                userResolverClass = DEFAULT_USER_RESOLVER_CLASS;
            }
        } else {
            try {
                userResolverClass = Class.forName(userResolverClassname).asSubclass(AbstractUserResolver.class);
            } catch (ClassNotFoundException e) {
                throw new NuxeoException("Failed get user resolver class " + userResolverClassname, e);
            }

        }
        try {
            userResolver = userResolverClass.getConstructor().newInstance();
            userResolver.init(parameters);
        } catch (ReflectiveOperationException e) {
            log.error("Failed to initialize user resolver {}", userResolverClassname);
        }

        // Initialize the OpenSAML library
        try {
            // don't use InitializationService.initialize
            // because it tries to configure MetricRegistry for version 4.x whereas we have 5.x
            new org.opensaml.core.xml.config.XMLObjectProviderInitializer().init();
            new org.opensaml.saml.config.impl.XMLObjectProviderInitializer().init();
            new org.opensaml.xmlsec.config.impl.XMLObjectProviderInitializer().init();
            new GlobalParserPoolInitializer().init();
            new GlobalAlgorithmRegistryInitializer().init();
            new DecryptionParserPoolInitializer().init();
        } catch (InitializationException e) {
            throw new NuxeoException("Failed to initialize OpenSAML library", e);
        }

        processorFactory = new SAMLProcessorFactory(parameters);
        ;

        // contribute icon and link to the Login Screen
        if (StringUtils.isNotBlank(parameters.get("name"))) {
            LoginScreenHelper.registerSingleProviderLoginScreenConfig(parameters.get("name"), parameters.get("icon"),
                    null, parameters.get("label"), parameters.get("description"), this);
        }
    }

    @Override
    public String computeUrl(HttpServletRequest request, String requestedUrl) {
        var processor = processorFactory.retrieveOutboundProcessor(WebSSOOutboundProcessor.PROFILE_URI);

        var catchRedirectResponse = new CatchRedirectHttpServletResponse();
        processor.execute(request, catchRedirectResponse);

        return catchRedirectResponse.getLocation();
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest request, HttpServletResponse response, String baseURL) {

        String loginError = (String) request.getAttribute(LOGIN_ERROR);
        if (loginError != null) {
            try {
                request.getRequestDispatcher(ERROR_PAGE).forward(request, response);
                return Boolean.TRUE;
            } catch (ServletException | IOException e) {
                log.error("Failed to redirect to error page", e);
                return Boolean.FALSE;
            }
        }

        String loginURL = computeUrl(request, null);
        try {
            response.sendRedirect(loginURL);
        } catch (IOException e) {
            log.error("Unable to send redirect on {}", loginURL, e);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    // Retrieves user identification information from the request.
    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest request, HttpServletResponse response) {
        var processor = processorFactory.retrieveInboundProcessor(request).orElse(null);
        // Check if the inbound binding is supported
        if (processor == null) {
            return null;
        }
        processor.execute(request, response);

        // check for processing errors
        if (request.getAttribute(LOGIN_ERROR) != null) {
            return null;
        }

        // handle SLO
        var credential = (SAMLCredential) request.getAttribute("SAMLCredential");
        if (credential == null) {
            // credential may be null in case of SLO
            return null;
        }

        Optional<String> userId = findOrCreateNuxeoUser(userResolver, credential);

        if (userId.isEmpty()) {
            log.warn("Failed to resolve user with NameID: {}", credential.getNameID().getValue());
            setLoginError(request, ERROR_USER);
            return null;
        }

        // store session id in a cookie
        getSAMLSessionCookie(credential).ifPresent(samlSessionCookie -> {
            Cookie cookie = samlSessionCookie.toCookie(request);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        });

        // redirect to URL in relay state if any
        HttpSession session = request.getSession(!response.isCommitted());
        if (session != null) {
            if (StringUtils.isNotEmpty(credential.getRelayState())) {
                session.setAttribute(NXAuthConstants.START_PAGE_SAVE_KEY, credential.getRelayState());
            }
        }

        UserIdentificationInfo userIdent = new UserIdentificationInfo(userId.get());
        userIdent.setCredentialsChecked(true);
        return userIdent;
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return Boolean.TRUE;
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    /**
     * Returns a Logout URL to use with HTTP Redirect
     */
    protected String getSLOUrl(HttpServletRequest request, HttpServletResponse response) {
        var processor = processorFactory.retrieveOutboundProcessor(SLOOutboundProcessor.PROFILE_URI);
        if (processor == null) {
            return null;
        }

        var catchRedirectResponse = new CatchRedirectHttpServletResponse();
        processor.execute(request, catchRedirectResponse);

        return catchRedirectResponse.getLocation();
    }

    @Override
    public Boolean handleLogout(HttpServletRequest request, HttpServletResponse response) {
        String logoutURL = getSLOUrl(request, response);

        if (logoutURL == null) {
            return Boolean.FALSE;
        }

        log.debug("Send redirect to {}", logoutURL);

        try {
            response.sendRedirect(logoutURL);
        } catch (IOException e) {
            log.error("Unable to send redirect on {}", logoutURL, e);
            return Boolean.FALSE;
        }

        getSAMLHttpCookie(request).ifPresent(cookie -> removeCookie(response, cookie));

        return Boolean.TRUE;
    }

    protected void removeCookie(HttpServletResponse httpResponse, Cookie cookie) {
        log.debug("Removing cookie {}", cookie.getName());
        cookie.setMaxAge(0);
        cookie.setValue("");
        httpResponse.addCookie(cookie);
    }

    protected Optional<String> findOrCreateNuxeoUser(UserResolver userResolver, SAMLCredential credential) {
        return Optional.ofNullable(Framework.doPrivileged(() -> userResolver.findOrCreateNuxeoUser(credential)));
    }
}
