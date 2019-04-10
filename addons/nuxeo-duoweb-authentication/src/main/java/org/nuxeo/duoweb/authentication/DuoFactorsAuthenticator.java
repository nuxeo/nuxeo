/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     vpasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.duoweb.authentication;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.login.LoginPlugin;
import org.nuxeo.ecm.platform.login.LoginPluginDescriptor;
import org.nuxeo.ecm.platform.login.LoginPluginRegistry;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.plugins.FormAuthenticator;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;

import com.duosecurity.DuoWeb;

/**
 * Authentication filter handles two factors authentication via Duo
 *
 * @since 5.9.5
 */
public class DuoFactorsAuthenticator extends FormAuthenticator {

    private static final Log log = LogFactory.getLog(FormAuthenticator.class);

    private static final String DUO_FACTOR_PAGE = "duofactors.jsp";

    private static final String POST_URL = "nxstartup.faces";

    private static final String SIG_REQUEST = "sig_request";

    private static final String SIG_RESPONSE = "sig_response";

    private static final String HOST_REQUEST = "host";

    private static final String POST_ACTION = "post_action";

    private static final String ONE_FACTOR_CHECK = "oneFactorCheck";

    private static final String TWO_FACTORS_CHECK = "twoFactorsCheck";

    private UserIdentificationInfo userIdent;

    private String IKEY;

    private String SKEY;

    private String AKEY;

    private String HOST;

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(ONE_FACTOR_CHECK) == null
                || !(Boolean) session.getAttribute(ONE_FACTOR_CHECK)) {
            super.handleLoginPrompt(httpRequest, httpResponse, baseURL);
            return Boolean.TRUE;
        } else if ((Boolean) session.getAttribute(ONE_FACTOR_CHECK)
                && (session.getAttribute(TWO_FACTORS_CHECK) == null || !(Boolean) session.getAttribute(TWO_FACTORS_CHECK))) {
            String redirectUrl = baseURL + DUO_FACTOR_PAGE;
            String postUrl = baseURL + POST_URL;
            Map<String, String> parameters = new HashMap<>();
            try {
                String userName = httpRequest.getParameter(usernameKey);
                if (userName == null) {
                    session.setAttribute(ONE_FACTOR_CHECK, Boolean.FALSE);
                    return Boolean.FALSE;
                }
                String request_sig = DuoWeb.signRequest(IKEY, SKEY, AKEY,
                        userName);
                parameters.put(SIG_REQUEST, request_sig);
                parameters.put(HOST_REQUEST, HOST);
                parameters.put(POST_ACTION, postUrl);
                redirectUrl = URIUtils.addParametersToURIQuery(redirectUrl,
                        parameters);
                httpResponse.sendRedirect(redirectUrl);
            } catch (IOException e) {
                log.error(e, e);
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            return null;
        }
        if (session.getAttribute(ONE_FACTOR_CHECK) == null
                || !(Boolean) session.getAttribute(ONE_FACTOR_CHECK)) {
            userIdent = super.handleRetrieveIdentity(httpRequest, httpResponse);
            if (userIdent != null) {
                try {
                    NuxeoPrincipal principal = validateUserIdentity();
                    if (principal != null) {
                        session.setAttribute(ONE_FACTOR_CHECK, Boolean.TRUE);
                        return null;
                    } else {
                        httpRequest.setAttribute(NXAuthConstants.LOGIN_ERROR,
                                NXAuthConstants.LOGIN_FAILED);
                        return null;
                    }
                } catch (LoginException | ClientException e) {
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
            String response = DuoWeb.verifyResponse(IKEY, SKEY, AKEY,
                    sigResponse);
            session.setAttribute(TWO_FACTORS_CHECK,
                    response != null ? Boolean.TRUE : Boolean.FALSE);
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

    public Principal createIdentity(String username) throws LoginException {
        UserManager manager = Framework.getService(UserManager.class);
        Random random = new Random(System.currentTimeMillis());
        log.debug("createIdentity: " + username);
        try {
            NuxeoPrincipal principal;
            if (manager == null) {
                principal = new NuxeoPrincipalImpl(username);
            } else {
                principal = manager.getPrincipal(username);
                if (principal == null) {
                    throw new LoginException(String.format(
                            "principal %s does not exist", username));
                }
            }
            String principalId = String.valueOf(random.nextLong());
            principal.setPrincipalId(principalId);
            return principal;
        } catch (LoginException | ClientException e) {
            log.error("createIdentity failed", e);
            LoginException le = new LoginException("createIdentity failed for"
                    + " user " + username);
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
            if (manager.checkUsernamePassword(userIdent.getUserName(),
                    userIdent.getPassword())) {
                return (NuxeoPrincipal) createIdentity(userIdent.getUserName());
            } else {
                return null;
            }
        } else {
            LoginPlugin lp = loginPluginManager.getPlugin(loginPluginName);
            if (lp == null) {
                log.error("Can't authenticate against a null loginModule "
                        + "plugin");
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
                    log.error("Unable to initialize LoginModulePlugin "
                            + lp.getName());
                    return null;
                }
            }

            String username = lp.validatedUserIdentity(userIdent);
            if (username == null) {
                return null;
            } else {
                return (NuxeoPrincipal) createIdentity(username);
            }
        }
    }

}
