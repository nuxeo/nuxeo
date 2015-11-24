/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Academie de Rennes - proxy CAS support
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.runtime.api.Framework;
import org.xml.sax.SAXException;

import edu.yale.its.tp.cas.client.ProxyTicketValidator;
import edu.yale.its.tp.cas.client.ServiceTicketValidator;

/**
 * @author Thierry Delprat
 * @author Olivier Adam
 * @author M.-A. Darche
 * @author Benjamin Jalon
 */
public class Cas2Authenticator implements NuxeoAuthenticationPlugin,
        NuxeoAuthenticationPluginLogoutExtension {

    protected static final Log log = LogFactory.getLog(Cas2Authenticator.class);

    protected static final String EXCLUDE_PROMPT_KEY = "excludePromptURL";

    protected String ticketKey = "ticket";

    protected String proxyKey = "proxy";

    protected String appURL = "http://127.0.0.1:8080/nuxeo/";

    protected String serviceLoginURL = "http://127.0.0.1:8080/cas/login";

    protected String serviceValidateURL = "http://127.0.0.1:8080/cas/serviceValidate";

    /**
     * We tell the CAS server whether we want a plain text (CAS 1.0) or XML (CAS
     * 2.0) response by making the request either to the '.../validate' or
     * '.../serviceValidate' URL. The older protocol supports only the CAS 1.0
     * functionality, which is left around as the legacy '.../validate' URL.
     */
    protected String proxyValidateURL = "http://127.0.0.1:8080/cas/proxyValidate";

    protected String serviceKey = "service";

    protected String logoutURL = "";

    protected String defaultCasServer = "";

    protected String ticketValidatorClassName = "edu.yale.its.tp.cas.client.ServiceTicketValidator";

    protected String proxyValidatorClassName = "edu.yale.its.tp.cas.client.ProxyTicketValidator";

    protected ProxyTicketValidator proxyValidator;

    protected ServiceTicketValidator ticketValidator;

    protected boolean promptLogin = true;

    protected final static String CAS_SERVER_HEADER_KEY = "CasServer";

    protected final static String CAS_SERVER_PATTERN_KEY = "$CASSERVER";

    protected final static String NUXEO_SERVER_PATTERN_KEY = "$NUXEO";

    protected final static String LOGIN_ACTION = "Login";

    protected final static String LOGOUT_ACTION = "Logout";

    protected final static String VALIDATE_ACTION = "Valid";

    protected final static String PROXY_VALIDATE_ACTION = "ProxyValid";

    protected List<String> excludePromptURLs;

    public List<String> getUnAuthenticatedURLPrefix() {
        // CAS login screen is not part of Nuxeo5 Web App
        return null;
    }

    protected String getServiceURL(HttpServletRequest httpRequest, String action) {
        String url = "";
        if (action.equals(LOGIN_ACTION)) {
            url = serviceLoginURL;
        } else if (action.equals(LOGOUT_ACTION)) {
            url = logoutURL;
        } else if (action.equals(VALIDATE_ACTION)) {
            url = serviceValidateURL;
        } else if (action.equals(PROXY_VALIDATE_ACTION)) {
            url = proxyValidateURL;
        }

        if (url.contains(CAS_SERVER_PATTERN_KEY)) {
            String serverURL = httpRequest.getHeader(CAS_SERVER_HEADER_KEY);
            if (serverURL != null)
                url = url.replace(CAS_SERVER_PATTERN_KEY, serverURL);
            else {
                if (url.contains(CAS_SERVER_PATTERN_KEY)) {
                    url = url.replace(CAS_SERVER_PATTERN_KEY, defaultCasServer);
                }
            }
        }
        log.debug("serviceUrl: " + url);
        return url;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        // Redirect to CAS Login screen
        // assing our application URL as service name
        String location = null;
        try {
            // httpResponse.sendRedirect(serviceLoginURL + "?" + serviceKey +
            // "=" + appURL);
            location = getServiceURL(httpRequest, LOGIN_ACTION) + "?"
                    + serviceKey + "=" + getAppURL(httpRequest);
            httpResponse.sendRedirect(location);
        } catch (IOException e) {
            log.error("Unable to redirect to CAS login screen to " + location,
                    e);
            return false;
        }
        return true;
    }

    protected String getAppURL(HttpServletRequest httpRequest) {
        if ((appURL == null) || (appURL.equals(""))) {
            appURL = NUXEO_SERVER_PATTERN_KEY;
        }
        if (appURL.contains(NUXEO_SERVER_PATTERN_KEY)) {
            String nxurl = BaseURL.getBaseURL(httpRequest);
            return appURL.replace(NUXEO_SERVER_PATTERN_KEY, nxurl);
        } else
            return appURL;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String casTicket = httpRequest.getParameter(ticketKey);

        // Retrieve the proxy parameter for knowing if the caller is à proxy
        // CAS
        String proxy = httpRequest.getParameter(proxyKey);

        if (casTicket == null) {
            log.debug("No ticket found");
            return null;
        }

        String userName;

        if (proxy == null) {
            // no ticket found
            userName = checkCasTicket(casTicket, httpRequest);
        } else {
            userName = checkProxyCasTicket(casTicket, httpRequest);
        }

        if (userName == null) {
            return null;
        }

        UserIdentificationInfo uui = new UserIdentificationInfo(userName,
                casTicket);
        uui.setToken(casTicket);

        return uui;
    }

    public void initPlugin(Map<String, String> parameters) {
        if (parameters.containsKey(CAS2Parameters.TICKET_NAME_KEY)) {
            ticketKey = parameters.get(CAS2Parameters.TICKET_NAME_KEY);
        }
        if (parameters.containsKey(CAS2Parameters.PROXY_NAME_KEY)) {
            proxyKey = parameters.get(CAS2Parameters.PROXY_NAME_KEY);
        }
        if (parameters.containsKey(CAS2Parameters.NUXEO_APP_URL_KEY)) {
            appURL = parameters.get(CAS2Parameters.NUXEO_APP_URL_KEY);
        }
        if (parameters.containsKey(CAS2Parameters.SERVICE_LOGIN_URL_KEY)) {
            serviceLoginURL = parameters.get(CAS2Parameters.SERVICE_LOGIN_URL_KEY);
        }
        if (parameters.containsKey(CAS2Parameters.SERVICE_VALIDATE_URL_KEY)) {
            serviceValidateURL = parameters.get(CAS2Parameters.SERVICE_VALIDATE_URL_KEY);
        }
        if (parameters.containsKey(CAS2Parameters.PROXY_VALIDATE_URL_KEY)) {
            proxyValidateURL = parameters.get(CAS2Parameters.PROXY_VALIDATE_URL_KEY);
        }
        if (parameters.containsKey(CAS2Parameters.SERVICE_NAME_KEY)) {
            serviceKey = parameters.get(CAS2Parameters.SERVICE_NAME_KEY);
        }
        if (parameters.containsKey(CAS2Parameters.LOGOUT_URL_KEY)) {
            logoutURL = parameters.get(CAS2Parameters.LOGOUT_URL_KEY);
        }
        if (parameters.containsKey(CAS2Parameters.DEFAULT_CAS_SERVER_KEY)) {
            defaultCasServer = parameters.get(CAS2Parameters.DEFAULT_CAS_SERVER_KEY);
        }
        if (parameters.containsKey(CAS2Parameters.SERVICE_VALIDATOR_CLASS)) {
            ticketValidatorClassName = parameters.get(CAS2Parameters.SERVICE_VALIDATOR_CLASS);
        }
        if (parameters.containsKey(CAS2Parameters.PROXY_VALIDATOR_CLASS)) {
            proxyValidatorClassName = parameters.get(CAS2Parameters.PROXY_VALIDATOR_CLASS);
        }
        if (parameters.containsKey(CAS2Parameters.PROMPT_LOGIN)) {
            promptLogin = Boolean.parseBoolean(parameters.get(CAS2Parameters.PROMPT_LOGIN));
        }
        excludePromptURLs = new ArrayList<String>();
        for (String key : parameters.keySet()) {
            if (key.startsWith(EXCLUDE_PROMPT_KEY)) {
                excludePromptURLs.add(parameters.get(key));
            }
        }
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        String requestedURI = httpRequest.getRequestURI();
        String context = httpRequest.getContextPath() + '/';
        requestedURI = requestedURI.substring(context.length());
        for (String prefixURL : excludePromptURLs) {
            if (requestedURI.startsWith(prefixURL)) {
                return false;
            }
        }
        return promptLogin;
    }

    public Boolean handleLogout(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (logoutURL == null || logoutURL.equals("")) {
            log.debug("No CAS logout params, skipping CAS2Logout");
            return false;
        }
        try {
            httpResponse.sendRedirect(getServiceURL(httpRequest, LOGOUT_ACTION));
        } catch (IOException e) {
            log.error("Unable to redirect to CAS logout screen:", e);
            return false;
        }
        return true;
    }

    protected String checkProxyCasTicket(String ticket,
            HttpServletRequest httpRequest) {
        // Get the service passed by the portlet
        String service = httpRequest.getParameter(serviceKey);
        if (service == null) {
            log.error("checkProxyCasTicket: no service name in the URL");
            return null;
        }

        try {
            proxyValidator = (ProxyTicketValidator) Framework.getRuntime().getContext().loadClass(
                    proxyValidatorClassName).newInstance();
        } catch (InstantiationException e) {
            log.error(
                    "checkProxyCasTicket during the ProxyTicketValidator initialization with InstantiationException:",
                    e);
            return null;
        } catch (IllegalAccessException e) {
            log.error(
                    "checkProxyCasTicket during the ProxyTicketValidator initialization with IllegalAccessException:",
                    e);
            return null;
        } catch (ClassNotFoundException e) {
            log.error(
                    "checkProxyCasTicket during the ProxyTicketValidator initialization with ClassNotFoundException:",
                    e);
            return null;
        }

        proxyValidator.setCasValidateUrl(getServiceURL(httpRequest,
                PROXY_VALIDATE_ACTION));
        proxyValidator.setService(service);
        proxyValidator.setServiceTicket(ticket);
        try {
            proxyValidator.validate();
        } catch (IOException e) {
            log.error("checkProxyCasTicket failed with IOException:", e);
            return null;
        } catch (SAXException e) {
            log.error("checkProxyCasTicket failed with SAXException:", e);
            return null;
        } catch (ParserConfigurationException e) {
            log.error(
                    "checkProxyCasTicket failed with ParserConfigurationException:",
                    e);
            return null;
        }
        log.debug("checkProxyCasTicket: validation executed without error");
        String username = proxyValidator.getUser();
        log.debug("checkProxyCasTicket: validation returned username = "
                + username);

        return username;
    }

    // Cas2 Ticket management
    protected String checkCasTicket(String ticket,
            HttpServletRequest httpRequest) {
        try {
            ticketValidator = (ServiceTicketValidator) Framework.getRuntime().getContext().loadClass(
                    ticketValidatorClassName).newInstance();
        } catch (InstantiationException e) {
            log.error(
                    "checkCasTicket during the ServiceTicketValidator initialization with InstantiationException:",
                    e);
            return null;
        } catch (IllegalAccessException e) {
            log.error(
                    "checkCasTicket during the ServiceTicketValidator initialization with IllegalAccessException:",
                    e);
            return null;
        } catch (ClassNotFoundException e) {
            log.error(
                    "checkCasTicket during the ServiceTicketValidator initialization with ClassNotFoundException:",
                    e);
            return null;
        }

        ticketValidator.setCasValidateUrl(getServiceURL(httpRequest,
                VALIDATE_ACTION));
        ticketValidator.setService(getAppURL(httpRequest));
        ticketValidator.setServiceTicket(ticket);
        try {
            ticketValidator.validate();
        } catch (IOException e) {
            log.error("checkCasTicket failed with IOException:", e);
            return null;
        } catch (SAXException e) {
            log.error("checkCasTicket failed with SAXException:", e);
            return null;
        } catch (ParserConfigurationException e) {
            log.error(
                    "checkCasTicket failed with ParserConfigurationException:",
                    e);
            return null;
        }
        log.debug("checkCasTicket : validation executed without error");
        String username = ticketValidator.getUser();
        log.debug("checkCasTicket: validation returned username = " + username);
        return username;
    }

}
