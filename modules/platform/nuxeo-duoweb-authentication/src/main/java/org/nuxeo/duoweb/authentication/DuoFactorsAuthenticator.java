/*
 * (C) Copyright 2015-2025 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.START_PAGE_SAVE_KEY;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.FormAuthenticator;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;

import com.duosecurity.Client;
import com.duosecurity.exception.DuoException;
import com.duosecurity.model.Token;

/**
 * Authentication filter handles two factors authentication via Duo
 *
 * @since 5.9.5
 */
public class DuoFactorsAuthenticator extends FormAuthenticator {

    protected static final Logger log = LogManager.getLogger(DuoFactorsAuthenticator.class);

    protected static final long STATE_TTL_SECONDS = Duration.ofMinutes(4).toSeconds();

    protected static final String KV_NAME = "duo-state";

    protected static final Random RANDOM = new SecureRandom();

    protected static final String NX_USER_FIRST_FACTOR_CHECKED = "nxFirstFactorChecked";

    protected Client duoClient;

    @Override
    public void initPlugin(Map<String, String> parameters) {
        var clientId = Objects.requireNonNull(parameters.get("IKEY"), "Missing clientId");
        var clientSecret = Objects.requireNonNull(parameters.get("SKEY"), "Missing secretKey");
        var apiHost = Objects.requireNonNull(parameters.get("HOST"), "Missing host");
        var appUrl = Objects.requireNonNull(parameters.get("appUrl"), "Missing app url");
        var proxyHost = parameters.get("proxyHost");
        var skipHealthCheck = Boolean.parseBoolean(parameters.get("skipHealthCheck"));
        try {
            Client.Builder builder;
            int proxyPort;
            if (Strings.isNotBlank(proxyHost)) {
                proxyPort = Integer.parseInt(Objects.requireNonNull(parameters.get("proxyPort"), "Missing proxy port"));
                builder = new Client.Builder(clientId, clientSecret, proxyHost, proxyPort, apiHost,
                        appUrl + "/" + LoginScreenHelper.getStartupPagePath());
            } else {
                builder = new Client.Builder(clientId, clientSecret, apiHost,
                        appUrl + "/" + LoginScreenHelper.getStartupPagePath());
            }
            var userAgentInfo = parameters.get("userAgentInfo");
            if (Strings.isNotBlank(userAgentInfo)) {
                builder.appendUserAgentInfo(userAgentInfo);
            }
            var caCerts = parameters.get("caCerts");
            if (Strings.isNotBlank(caCerts)) {
                builder.setCACerts(caCerts.split(","));
            }
            duoClient = builder.build();
        } catch (DuoException e) {
            throw new NuxeoException(e);
        }
        if (!Boolean.TRUE.equals(skipHealthCheck)) {
            try {
                getClient().healthCheck();
            } catch (DuoException e) {
                throw new NuxeoException("Cannot reach Duo", e);
            }
        }

    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(NX_USER_FIRST_FACTOR_CHECKED) == null) {
            if (session != null) {
                session.setAttribute(START_PAGE_SAVE_KEY, getRequestedUrl(httpRequest));
            }
            return super.handleLoginPrompt(httpRequest, httpResponse, baseURL);
        } else if (session.getAttribute(NX_USER_FIRST_FACTOR_CHECKED) != null
                && Strings.isBlank(httpRequest.getParameter("state"))) {
            try {
                var username = String.valueOf(session.getAttribute(NX_USER_FIRST_FACTOR_CHECKED));
                session.removeAttribute(NX_USER_FIRST_FACTOR_CHECKED);
                var state = getClient().generateState();
                getKeyValueStore().put(state, username, STATE_TTL_SECONDS);
                var authUrl = getClient().createAuthUrl(username, state);
                httpResponse.sendRedirect(authUrl);
            } catch (DuoException e) {
                throw new NuxeoException(e);
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
        var state = httpRequest.getParameter("state");
        if (Strings.isBlank(state)) {
            UserIdentificationInfo userIdentity = super.handleRetrieveIdentity(httpRequest, httpResponse);
            if (userIdentity != null) {
                try {
                    NuxeoPrincipal principal = validateUserIdentity(userIdentity);
                    if (principal != null) {
                        session.setAttribute(NX_USER_FIRST_FACTOR_CHECKED, userIdentity.getUserName());
                    } else {
                        httpRequest.setAttribute(NXAuthConstants.LOGIN_ERROR, NXAuthConstants.LOGIN_FAILED);
                    }
                } catch (LoginException e) {
                    log.error(e, e);
                }
            }
            return null;
        } else {
            String username = getKeyValueStore().removeString(state);
            try {
                if (username != null) {
                    var duoCode = httpRequest.getParameter("duo_code");
                    Token token = getClient().exchangeAuthorizationCodeFor2FAResult(duoCode, username);
                    if (authWasSuccessful(token)) {
                        return new UserIdentificationInfo(username);
                    }
                }
                return null;
            } catch (DuoException e) {
                log.error("Duo 2FA check failed", e);
                return null;
            }
        }
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    public NuxeoPrincipal createIdentity(String username) throws LoginException {
        UserManager manager = Framework.getService(UserManager.class);
        log.debug("createIdentity: {}", username);
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

    protected boolean authWasSuccessful(Token token) {
        if (token != null && token.getAuth_result() != null) {
            return "ALLOW".equalsIgnoreCase(token.getAuth_result().getStatus());
        }
        return false;
    }

    protected Client getClient() {
        return duoClient;
    }

    protected KeyValueStoreProvider getKeyValueStore() {
        return (KeyValueStoreProvider) Framework.getService(KeyValueService.class).getKeyValueStore(KV_NAME);
    }

    protected NuxeoPrincipal validateUserIdentity(UserIdentificationInfo userIdentity) throws LoginException {
        UserManager manager = Framework.getService(UserManager.class);
        if (manager.checkUsernamePassword(userIdentity.getUserName(), userIdentity.getPassword())) {
            return createIdentity(userIdentity.getUserName());
        } else {
            return null;
        }
    }

    protected String getRequestedUrl(HttpServletRequest httpRequest) {
        String completeURI = httpRequest.getRequestURI();
        String qs = httpRequest.getQueryString();
        String context = httpRequest.getContextPath() + '/';
        String requestPage = completeURI.substring(context.length());
        if (qs != null && !qs.isEmpty()) {
            requestPage = requestPage + '?' + qs;
        }
        return requestPage;
    }
}
