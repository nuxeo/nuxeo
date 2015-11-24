/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;

import jcifs.Config;
import jcifs.UniAddress;
import jcifs.http.NtlmSsp;
import jcifs.smb.NtlmChallenge;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbSession;

public class NTLMAuthenticator implements NuxeoAuthenticationPlugin {

    protected String defaultDomain;

    protected String domainController;

    protected boolean loadBalance;

    private static final Log log = LogFactory.getLog(NTLMAuthenticator.class);

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {

        log.debug("Handle NTLM login prompt");
        NtlmPasswordAuthentication ntlm=null;
        HttpSession ssn = httpRequest.getSession(false);

        if (ssn !=null)
        {
            ntlm = (NtlmPasswordAuthentication) ssn.getAttribute("NtlmHttpAuth");
        }

        if (ssn == null || ntlm == null) {
            log.debug("Sending NTLM Chanllenge/Response request to browser");
            httpResponse.setHeader("WWW-Authenticate", "NTLM");
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentLength(0);
            try {
                httpResponse.flushBuffer();
            } catch (IOException e) {
                log.error("Error while flushing buffer:" + e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else
        {
            log.debug("No NTLM Prompt done !!!");
            return false;
        }
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {


        log.debug("NTML handleRetrieveIdentity");
        NtlmPasswordAuthentication ntlm;

        try {
            ntlm = negotiate(httpRequest, httpResponse, false);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("NTLM negociation failed : " + e.getMessage());
            return null;
        } catch (ServletException e) {
            e.printStackTrace();
            log.error("NTLM negociation failed : " + e.getMessage());
            return null;
        }

        if (ntlm==null)
        {
            log.debug("Negociation returned a null NTLM token");
            return null;
        }
        else
        {
            log.debug("Negociation succeed and returned a NTLM token, creating UserIdentificationInfo");
            String userName = ntlm.getUsername();
            log.debug("ntlm.getUsername() = " + userName);
            if (userName.startsWith(ntlm.getDomain()))
            {
                userName = userName.replace(ntlm.getDomain() + "/", "");
            }
            log.debug("userName = " + userName);
            String password = ntlm.getPassword();
            if (password==null)
            {
            	// we don't get the NTLM password, so we have to trust NTLM auth
            	UserIdentificationInfo userInfo =new UserIdentificationInfo(ntlm.getUsername(), "ITrustNTLM");
            	userInfo.setLoginPluginName("Trusting_LM");
            	return 	userInfo;
            }
            else
            	return new UserIdentificationInfo(ntlm.getUsername(), ntlm.getPassword());
        }
    }

    public void initPlugin(Map<String, String> parameters) {

        Config.setProperty("jcifs.smb.client.soTimeout", "300000");
        Config.setProperty("jcifs.netbios.cachePolicy", "1200");

        // init CIFS from parameters
        for (String name : parameters.keySet()) {
            if (name.startsWith("jcifs.")) {
                Config.setProperty(name, parameters.get(name));
            }
        }

        // get params from CIFS config
        defaultDomain = Config.getProperty("jcifs.smb.client.domain");
        domainController = Config.getProperty("jcifs.http.domainController");
        if (domainController == null) {
            domainController = defaultDomain;
            loadBalance = Config.getBoolean("jcifs.http.loadBalance", true);
        }

    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        String useragent = httpRequest.getHeader("User-Agent").toLowerCase();

        // only prompt IE on windows platform

        if (!useragent.contains("windows"))
        {
            log.debug("No NTLM LoginPrompt : User does not use Win32");
            return false;
        }
        if (!useragent.contains("msie"))
        {
            log.debug("No NTLM LoginPrompt : User does not use MSIE");
            return false;
        }

        log.debug("NTLM LoginPrompt Needed");
        return true;
    }

    protected NtlmPasswordAuthentication negotiate(HttpServletRequest req,
            HttpServletResponse resp, boolean skipAuthentication)
            throws IOException, ServletException {

        UniAddress dc;
        String msg;
        NtlmPasswordAuthentication ntlm = null;

        log.debug("NTLM negitiation starts");

        msg = req.getHeader("Authorization");

        log.debug("NTLM negitiation header = " + msg);
        if (msg != null && (msg.startsWith("NTLM "))) {
            HttpSession ssn = req.getSession();
            byte[] challenge;

            if (loadBalance) {
                NtlmChallenge chal = (NtlmChallenge) ssn
                        .getAttribute("NtlmHttpChal");
                if (chal == null) {
                    chal = SmbSession.getChallengeForDomain();
                    ssn.setAttribute("NtlmHttpChal", chal);
                }
                dc = chal.dc;
                challenge = chal.challenge;
            } else {
                dc = UniAddress.getByName(domainController, true);
                dc = UniAddress.getByName( dc.getHostAddress(), true );
                challenge = SmbSession.getChallenge(dc);
            }


            ntlm = NtlmSsp.authenticate(req, resp, challenge);
            if (ntlm  == null) {
                log.debug("NtlmSsp.authenticate returned null");
                return null;
            }

            log.debug("NtlmSsp.authenticate succeed");
            log.debug("Domain controler is " + dc.getHostName());
            if (ntlm.getDomain()!=null)
                log.debug("NtlmSsp.authenticate => domain = " + ntlm.getDomain());
            if (ntlm.getUsername()!=null)
                log.debug("NtlmSsp.authenticate => userName = " + ntlm.getUsername());
            if (ntlm.getPassword()!=null)
                log.debug("NtlmSsp.authenticate => password = " + ntlm.getPassword());

            /* negotiation complete, remove the challenge object */
            ssn.removeAttribute("NtlmHttpChal");
            try {

                log.debug("Trying to logon NTLM session on dc " + dc.toString());
                SmbSession.logon(dc, ntlm);
                log.debug(ntlm + " successfully authenticated against " + dc);

            } catch (SmbAuthException sae) {

                log.error(ntlm.getName() + ": 0x"
                        + jcifs.util.Hexdump.toHexString(sae.getNtStatus(), 8)
                        + ": " + sae);

                if (sae.getNtStatus() == sae.NT_STATUS_ACCESS_VIOLATION) {
                    /*
                     * Server challenge no longer valid for externally supplied
                     * password hashes.
                     */
                    ssn = req.getSession(false);
                    if (ssn != null) {
                        ssn.removeAttribute("NtlmHttpAuth");
                    }
                }
                resp.setHeader("WWW-Authenticate", "NTLM");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentLength(0);
                resp.flushBuffer();
                return null;
            }
            req.getSession().setAttribute("NtlmHttpAuth", ntlm);
        } else {
            log.debug("NTLM negociation header is null");
            return null;
        }
        return ntlm;
    }

}
