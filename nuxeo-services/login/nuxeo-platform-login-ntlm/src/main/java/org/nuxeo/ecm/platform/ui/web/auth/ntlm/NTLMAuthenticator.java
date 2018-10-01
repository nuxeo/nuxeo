/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.ntlm;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jcifs.Config;
import jcifs.UniAddress;
import jcifs.http.NtlmSsp;

import jcifs.smb.NtlmChallenge;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

import static jcifs.smb.NtStatus.NT_STATUS_ACCESS_VIOLATION;

public class NTLMAuthenticator implements NuxeoAuthenticationPlugin {

    private static final String JCIFS_PREFIX = "jcifs.";

    public static final String JCIFS_NETBIOS_CACHE_POLICY = "jcifs.netbios.cachePolicy";

    public static final String JCIFS_SMB_CLIENT_SO_TIMEOUT = "jcifs.smb.client.soTimeout";

    public static final String JCIFS_HTTP_LOAD_BALANCE = "jcifs.http.loadBalance";

    public static final String JCIFS_HTTP_DOMAIN_CONTROLLER = "jcifs.http.domainController";

    public static final String JCIFS_SMB_CLIENT_DOMAIN = "jcifs.smb.client.domain";

    public static final boolean FORCE_SESSION_CREATION = true;

    public static final String NTLM_HTTP_AUTH_SESSION_KEY = "NtlmHttpAuth";

    public static final String NTLM_HTTP_CHAL_SESSION_KEY = "NtlmHttpChal";

    protected static String defaultDomain;

    protected static String domainController;

    protected static boolean loadBalance;

    private static final Log log = LogFactory.getLog(NTLMAuthenticator.class);

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {

        log.debug("Handle NTLM login prompt");
        NtlmPasswordAuthentication ntlm = null;
        HttpSession httpSession = httpRequest.getSession(FORCE_SESSION_CREATION);

        if (httpSession != null) {
            ntlm = (NtlmPasswordAuthentication) httpSession.getAttribute(NTLM_HTTP_AUTH_SESSION_KEY);
        }

        if (httpSession == null || ntlm == null) {
            log.debug("Sending NTLM Challenge/Response request to browser");
            httpResponse.setHeader("WWW-Authenticate", "NTLM");
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentLength(0);
            try {
                httpResponse.flushBuffer();
            } catch (IOException e) {
                log.error("Error while flushing buffer:" + e.getMessage(), e);
            }
            return true;
        } else {
            log.debug("No NTLM Prompt done since NTLM Auth was found :" + ntlm.getUsername());
            return false;
        }
    }

    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.debug("NTML handleRetrieveIdentity");
        NtlmPasswordAuthentication ntlm;

        try {
            ntlm = negotiate(httpRequest, httpResponse, false);
        } catch (IOException | ServletException e) {
            log.error("NTLM negotiation failed : " + e.getMessage(), e);
            return null;
        }

        if (ntlm == null) {
            log.debug("Negotiation returned a null NTLM token");
            return null;
        } else {
            log.debug("Negotiation succeed and returned a NTLM token, creating UserIdentificationInfo");
            String userName = ntlm.getUsername();
            log.debug("ntlm.getUsername() = " + userName);
            if (userName.startsWith(ntlm.getDomain())) {
                userName = userName.replace(ntlm.getDomain() + "/", "");
            }
            log.debug("userName = " + userName);
            String password = ntlm.getPassword();
            if (password == null || "".equals(password)) {
                // we don't get the NTLM password, so we have to trust NTLM auth
                UserIdentificationInfo userInfo = new UserIdentificationInfo(ntlm.getUsername(), "ITrustNTLM");
                userInfo.setLoginPluginName("Trusting_LM");
                return userInfo;
            } else {
                return new UserIdentificationInfo(ntlm.getUsername(), ntlm.getPassword());
            }
        }
    }

    public void initPlugin(Map<String, String> parameters) {

        Config.setProperty(JCIFS_SMB_CLIENT_SO_TIMEOUT, "300000");
        Config.setProperty(JCIFS_NETBIOS_CACHE_POLICY, "1200");

        // init CIFS from parameters
        for (String name : parameters.keySet()) {
            if (name.startsWith(JCIFS_PREFIX)) {
                Config.setProperty(name, parameters.get(name));
            }
        }

        // get params from CIFS config
        defaultDomain = Config.getProperty(JCIFS_SMB_CLIENT_DOMAIN);
        domainController = Config.getProperty(JCIFS_HTTP_DOMAIN_CONTROLLER);
        if (domainController == null) {
            domainController = defaultDomain;
            loadBalance = Config.getBoolean(JCIFS_HTTP_LOAD_BALANCE, true);
        }
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        String useragent = httpRequest.getHeader("User-Agent").toLowerCase();

        // only prompt on windows platform

        if (!useragent.contains("windows")) {
            log.debug("No NTLM LoginPrompt : User does not use Win32");
            return false;
        }

        log.debug("NTLM LoginPrompt Needed");
        return true;
    }

    public static NtlmPasswordAuthentication negotiate(HttpServletRequest req, HttpServletResponse resp,
            boolean skipAuthentication) throws IOException, ServletException {
        log.debug("NTLM negotiation starts");

        String msg = req.getHeader("Authorization");

        log.debug("NTLM negotiation header = " + msg);
        NtlmPasswordAuthentication ntlm;
        if (msg != null && msg.startsWith("NTLM ")) {
            HttpSession ssn = req.getSession();
            byte[] challenge;

            UniAddress dc;
            if (loadBalance) {
                NtlmChallenge chal = (NtlmChallenge) ssn.getAttribute(NTLM_HTTP_CHAL_SESSION_KEY);
                if (chal == null) {
                    chal = SmbSession.getChallengeForDomain();
                    ssn.setAttribute(NTLM_HTTP_CHAL_SESSION_KEY, chal);
                }
                dc = chal.dc;
                challenge = chal.challenge;
            } else {
                dc = UniAddress.getByName(domainController, true);
                dc = UniAddress.getByName(dc.getHostAddress(), true);
                challenge = SmbSession.getChallenge(dc);
            }

            ntlm = NtlmSsp.authenticate(req, resp, challenge);
            if (ntlm == null) {
                log.debug("NtlmSsp.authenticate returned null");
                return null;
            }

            log.debug("NtlmSsp.authenticate succeed");
            log.debug("Domain controller is " + dc.getHostName());
            if (ntlm.getDomain() != null) {
                log.debug("NtlmSsp.authenticate => domain = " + ntlm.getDomain());
            } else {
                log.debug("NtlmSsp.authenticate => null domain");
            }
            if (ntlm.getUsername() != null) {
                log.debug("NtlmSsp.authenticate => userName = " + ntlm.getUsername());
            } else {
                log.debug("NtlmSsp.authenticate => userName = null");
            }
            if (ntlm.getPassword() != null) {
                log.debug("NtlmSsp.authenticate => password = " + ntlm.getPassword());
            } else {
                log.debug("NtlmSsp.authenticate => password = null");
            }

            /* negotiation complete, remove the challenge object */
            ssn.removeAttribute(NTLM_HTTP_CHAL_SESSION_KEY);
            if (!skipAuthentication) {
                try {
                    log.debug("Trying to logon NTLM session on dc " + dc.toString());
                    SmbSession.logon(dc, ntlm);
                    log.debug(ntlm + " successfully authenticated against " + dc);

                } catch (SmbAuthException sae) {

                    log.error(ntlm.getName() + ": 0x" + jcifs.util.Hexdump.toHexString(sae.getNtStatus(), 8) + ": "
                            + sae);

                    if (sae.getNtStatus() == NT_STATUS_ACCESS_VIOLATION) {
                        /*
                         * Server challenge no longer valid for externally supplied password hashes.
                         */
                        ssn = req.getSession(false);
                        if (ssn != null) {
                            ssn.removeAttribute(NTLM_HTTP_AUTH_SESSION_KEY);
                        }
                    }
                    resp.setHeader("WWW-Authenticate", "NTLM");
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.setContentLength(0);
                    resp.flushBuffer();
                    return null;
                }
                req.getSession().setAttribute(NTLM_HTTP_AUTH_SESSION_KEY, ntlm);
            }
        } else {
            log.debug("NTLM negotiation header is null");
            return null;
        }
        return ntlm;
    }

}
