/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.duoweb.authentication;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.REQUESTED_URL;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.START_PAGE_SAVE_KEY;

import java.io.IOException;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.login.LoginPlugin;
import org.nuxeo.ecm.platform.login.LoginPluginDescriptor;
import org.nuxeo.ecm.platform.login.LoginPluginRegistry;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.FormAuthenticator;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;

import com.duosecurity.DuoWeb;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Authentication filter handles two factors authentication via Duo
 *
 * @since 5.9.5
 */
public class DuoFactorsAuthenticator extends FormAuthenticator {

    private static final Log log = LogFactory.getLog(FormAuthenticator.class);

    protected static final Random RANDOM = new SecureRandom();

    private static final String DUO_FACTOR_PAGE = "duofactors.jsp";

    private static final String SIG_REQUEST = "sig_request";

    private static final String SIG_RESPONSE = "sig_response";

    private static final String HOST_REQUEST = "host";

    private static final String POST_ACTION = "post_action";

    private static final String ONE_FACTOR_CHECK = "oneFactorCheck";

    private static final String TWO_FACTORS_CHECK = "twoFactorsCheck";

    private static final String HASHCODE = "hash";

    protected static final Integer CACHE_CONCURRENCY_LEVEL = 10;

    protected static final Integer CACHE_MAXIMUM_SIZE = 1000;

    // DuoWeb timeout is 1 minute => taking 4 minutes in case
    protected static final Integer CACHE_TIMEOUT = 4;

    private UserIdentificationInfo userIdent;

    private String IKEY;

    private String SKEY;

    private String AKEY;

    private String HOST;

    private Cache<String, UserIdentificationInfo> credentials = CacheBuilder.newBuilder().concurrencyLevel(CACHE_CONCURRENCY_LEVEL).maximumSize(
            CACHE_MAXIMUM_SIZE).expireAfterWrite(CACHE_TIMEOUT, TimeUnit.MINUTES).build();

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(ONE_FACTOR_CHECK) == null
                || !(Boolean) session.getAttribute(ONE_FACTOR_CHECK)) {
            if (session != null)
                session.setAttribute(START_PAGE_SAVE_KEY, getRequestedUrl
                        (httpRequest));
            super.handleLoginPrompt(httpRequest, httpResponse, baseURL);
            return Boolean.TRUE;
        } else if ((Boolean) session.getAttribute(ONE_FACTOR_CHECK)
                && (session.getAttribute(TWO_FACTORS_CHECK) == null || !(Boolean) session.getAttribute(TWO_FACTORS_CHECK))) {
            String redirectUrl = baseURL + DUO_FACTOR_PAGE;
            String postUrl = baseURL + LoginScreenHelper.getStartupPagePath();
            Map<String, String> parameters = new HashMap<>();
            try {
                String userName = httpRequest.getParameter(usernameKey);
                if (userName == null) {
                    session.setAttribute(ONE_FACTOR_CHECK, Boolean.FALSE);
                    return Boolean.FALSE;
                }
                String request_sig = DuoWeb.signRequest(IKEY, SKEY, AKEY, userName);
                parameters.put(SIG_REQUEST, request_sig);
                parameters.put(HOST_REQUEST, HOST);
                // Handle callback context
                String key = Integer.toHexString(userIdent.hashCode());
                credentials.put(key, userIdent);
                parameters.put(POST_ACTION, postUrl + "?" + HASHCODE + "=" + key);
                parameters.put(REQUESTED_URL, httpRequest.getParameter(REQUESTED_URL));
                redirectUrl = URIUtils.addParametersToURIQuery(redirectUrl, parameters);
                httpResponse.sendRedirect(redirectUrl);
            } catch (IOException e) {
                log.error(e, e);
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            return null;
        }
        if (session.getAttribute(ONE_FACTOR_CHECK) == null || !(Boolean) session.getAttribute(ONE_FACTOR_CHECK)) {
            userIdent = super.handleRetrieveIdentity(httpRequest, httpResponse);
            session = httpRequest.getSession(true);
            if (userIdent != null) {
                try {
                    NuxeoPrincipal principal = validateUserIdentity();
                    if (principal != null) {
                        session.setAttribute(ONE_FACTOR_CHECK, Boolean.TRUE);
                        return null;
                    } else {
                        httpRequest.setAttribute(NXAuthConstants.LOGIN_ERROR, NXAuthConstants.LOGIN_FAILED);
                        return null;
                    }
                } catch (LoginException e) {
                    log.error(e, e);
                    return null;
                }
            } else {
                session.setAttribute(ONE_FACTOR_CHECK, Boolean.FALSE);
                return null;
            }
        } else if (session.getAttribute(TWO_FACTORS_CHECK) == null
                || !(Boolean) session.getAttribute(TWO_FACTORS_CHECK)) {
            String sigResponse = httpRequest.getParameter(SIG_RESPONSE);
            String hashResponse = httpRequest.getParameter(HASHCODE);
            String response = DuoWeb.verifyResponse(IKEY, SKEY, AKEY, sigResponse);
            userIdent = credentials.getIfPresent(hashResponse);
            session.setAttribute(TWO_FACTORS_CHECK, response != null ? Boolean.TRUE : Boolean.FALSE);
            if (response == null) {
                return null;
            }
            return userIdent;
        }
        return userIdent;
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {
        if (parameters.get("IKEY") != null) {
            IKEY = parameters.get("IKEY");
        }
        if (parameters.get("SKEY") != null) {
            SKEY = parameters.get("SKEY");
        }
        if (parameters.get("AKEY") != null) {
            AKEY = parameters.get("AKEY");
        }
        if (parameters.get("HOST") != null) {
            HOST = parameters.get("HOST");
        }
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    public NuxeoPrincipal createIdentity(String username) throws LoginException {
        UserManager manager = Framework.getService(UserManager.class);
        log.debug("createIdentity: " + username);
        try {
            NuxeoPrincipal principal;
            if (manager == null) {
                principal = new NuxeoPrincipalImpl(username);
            } else {
                principal = Framework.doPrivileged(() -> manager.getPrincipal(username));
                if (principal == null) {
                    throw new LoginException(String.format("principal %s does not exist", username));
                }
            }
            String principalId = String.valueOf(RANDOM.nextLong());
            principal.setPrincipalId(principalId);
            return principal;
        } catch (LoginException e) {
            log.error("createIdentity failed", e);
            LoginException le = new LoginException("createIdentity failed for" + " user " + username);
            le.initCause(e);
            throw le;
        }
    }

    protected NuxeoPrincipal validateUserIdentity() throws LoginException {
        UserManager manager = Framework.getService(UserManager.class);
        final RuntimeService runtime = Framework.getRuntime();
        LoginPluginRegistry loginPluginManager = (LoginPluginRegistry) runtime.getComponent(LoginPluginRegistry.NAME);
        String loginPluginName = userIdent.getLoginPluginName();
        if (loginPluginName == null) {
            // we don't use a specific plugin
            if (manager.checkUsernamePassword(userIdent.getUserName(), userIdent.getPassword())) {
                return createIdentity(userIdent.getUserName());
            } else {
                return null;
            }
        } else {
            LoginPlugin lp = loginPluginManager.getPlugin(loginPluginName);
            if (lp == null) {
                log.error("Can't authenticate against a null loginModule " + "plugin");
                return null;
            }
            // set the parameters and reinit if needed
            LoginPluginDescriptor lpd = loginPluginManager.getPluginDescriptor(loginPluginName);
            if (!lpd.getInitialized()) {
                Map<String, String> existingParams = lp.getParameters();
                if (existingParams == null) {
                    existingParams = new HashMap<>();
                }
                Map<String, String> loginParams = userIdent.getLoginParameters();
                if (loginParams != null) {
                    existingParams.putAll(loginParams);
                }
                boolean init = lp.initLoginModule();
                if (init) {
                    lpd.setInitialized(true);
                } else {
                    log.error("Unable to initialize LoginModulePlugin " + lp.getName());
                    return null;
                }
            }

            String username = lp.validatedUserIdentity(userIdent);
            if (username == null) {
                return null;
            } else {
                return createIdentity(username);
            }
        }
    }

    protected String getRequestedUrl(HttpServletRequest httpRequest) {
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
}

