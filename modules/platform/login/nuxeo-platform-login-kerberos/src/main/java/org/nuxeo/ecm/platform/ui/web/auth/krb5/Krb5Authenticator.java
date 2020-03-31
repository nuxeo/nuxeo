/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sylvain Chambon
 */
package org.nuxeo.ecm.platform.ui.web.auth.krb5;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * Kerberos v5 in SPNEGO authentication. TODO handle NTLMSSP as a fallback position.
 *
 * @author schambon
 */
public class Krb5Authenticator implements NuxeoAuthenticationPlugin {

    private static final String CONTEXT_ATTRIBUTE = "Krb5Authenticator_context";

    private static final Log logger = LogFactory.getLog(Krb5Authenticator.class);

    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String AUTHORIZATION = "Authorization";

    private static final String NEGOTIATE = "Negotiate";

    private static final String SKIP_KERBEROS = "X-Skip-Kerberos"; // magic header used by the reverse proxy to skip
                                                                   // this authenticator

    private static final GSSManager MANAGER = GSSManager.getInstance();

    private GSSCredential serverCredential = null;

    private boolean disabled = false;

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest req, HttpServletResponse res, String baseURL) {

        logger.debug("Sending login prompt...");
        if (res.getHeader(WWW_AUTHENTICATE) == null) {
            res.setHeader(WWW_AUTHENTICATE, NEGOTIATE);
        }
        // hack to support fallback to form auth in case the
        // client does not answer the SPNEGO challenge.
        // This will obviously break if form auth is disabled; but this isn't
        // much of an issue since other sso filters will not work nicely after
        // this one (as this one takes over the response and flushes it to start
        // negotiation).
        String refresh = String.format("1;url=/%s/login.jsp", VirtualHostHelper.getWebAppName(req));
        res.setHeader("Refresh", refresh);
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentLength(0);
        try {
            res.flushBuffer();

        } catch (IOException e) {
            logger.warn("Cannot flush response", e);
        }
        return true;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest req, HttpServletResponse res) {
        String authorization = req.getHeader(AUTHORIZATION);
        if (authorization == null) {
            return null; // no auth
        }

        if (!authorization.startsWith(NEGOTIATE)) {
            logger.warn(
                    "Received invalid Authorization header (expected: Negotiate then SPNEGO blob): " + authorization);
            // ignore invalid authorization headers.
            return null;
        }

        byte[] token = Base64.decodeBase64(authorization.substring(NEGOTIATE.length() + 1));
        byte[] respToken;

        GSSContext context;

        try {
            synchronized (this) {
                context = (GSSContext) req.getSession().getAttribute(CONTEXT_ATTRIBUTE);
                if (context == null) {
                    context = MANAGER.createContext(serverCredential);
                }
                respToken = context.acceptSecContext(token, 0, token.length);

            }
            if (context.isEstablished()) {
                String principal = context.getSrcName().toString();
                String username = principal.split("@")[0]; // throw away the realm
                UserIdentificationInfo info = new UserIdentificationInfo(username);
                req.getSession().removeAttribute(CONTEXT_ATTRIBUTE);
                return info;
            } else {
                // save context in the HTTP session to be reused after client response
                req.getSession().setAttribute(CONTEXT_ATTRIBUTE, context);
                // need another roundtrip
                res.setHeader(WWW_AUTHENTICATE, NEGOTIATE + " " + Base64.encodeBase64String(respToken));
                return null;
            }

        } catch (GSSException ge) {
            req.getSession().removeAttribute(CONTEXT_ATTRIBUTE);
            logger.error("Cannot accept provided security token", ge);
            return null;
        }

    }

    @Override
    public void initPlugin(Map<String, String> parameters) {

        try {
            LoginContext loginContext = new LoginContext("Nuxeo");
            // note: we assume that all configuration is done in loginconfig, so there are NO parameters here
            loginContext.login();
            serverCredential = Subject.doAs(loginContext.getSubject(), getServerCredential);
            logger.debug("Successfully initialized Kerberos auth module");
        } catch (LoginException le) {
            logger.warn("Cannot create LoginContext, disabling Kerberos module", le);
            this.disabled = true;
        } catch (PrivilegedActionException pae) {
            logger.warn("Cannot get server credentials, disabling Kerberos module", pae);
            this.disabled = true;
        }

    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest req) {
        return !disabled && (req.getHeader(SKIP_KERBEROS) == null);
    }

    private PrivilegedExceptionAction<GSSCredential> getServerCredential = () -> MANAGER.createCredential(null,
            GSSCredential.DEFAULT_LIFETIME, new Oid[] { new Oid("1.3.6.1.5.5.2") /* Oid for Kerberos */,
                    new Oid("1.2.840.113554.1.2.2") /* Oid for SPNEGO */ },
            GSSCredential.ACCEPT_ONLY);
}
