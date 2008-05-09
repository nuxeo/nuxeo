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

package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import java.io.IOException;
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
import org.xml.sax.SAXException;
import edu.yale.its.tp.cas.client.ServiceTicketValidator;

public class Cas2Authenticator implements NuxeoAuthenticationPlugin,
        NuxeoAuthenticationPluginLogoutExtension {

    protected String ticketKey = "ticket";
    protected String appURL = "http://127.0.0.1:8080/nuxeo/";
    protected String serviceLoginURL = "http://127.0.0.1:8080/cas/login";
    protected String serviceValidateURL = "http://127.0.0.1:8080/cas/validate";
    protected String serviceKey = "service";
    protected String logoutURL = "";
    protected String defaultCasServer = "";

    protected final static String CAS_SERVER_HEADER_KEY = "CasServer";
    protected final static String CAS_SERVER_PATTERN_KEY = "$CASSERVER";
    protected final static String NUXEO_SERVER_PATTERN_KEY = "$NUXEO";
    protected final static String LOGIN_ACTION = "Login";
    protected final static String LOGOUT_ACTION = "Logout";
    protected final static String VALIDATE_ACTION = "Valid";

    private static final Log log = LogFactory.getLog(Cas2Authenticator.class);

    public List<String> getUnAuthenticatedURLPrefix() {
        // CAS login screen is not part of Nuxeo5 Web App
        return null;
    }

    private String getServiceURL(HttpServletRequest httpRequest, String action) {
        String url = "";
        if (action.equals(LOGIN_ACTION)) {
            url = serviceLoginURL;
        } else if (action.equals(LOGOUT_ACTION)) {
            url = logoutURL;
        } else if (action.equals(VALIDATE_ACTION)) {
            url = serviceValidateURL;
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
        log.debug("serviceUrl=" + url);
        return url;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        // redirect to CAS Login screen
        // passing our application URL as service name
        String location = null;
        try {
            // httpResponse.sendRedirect(serviceLoginURL + "?" + serviceKey +
            // "=" + appURL);
            location = getServiceURL(httpRequest, LOGIN_ACTION) + "?"
                    + serviceKey + "=" + getAppURL(httpRequest);
            httpResponse.sendRedirect(location);
        } catch (IOException e) {
            log.error("Unable to redirect to CAS login screen to " + location,e);
            return false;
        }
        return true;
    }

    private String getAppURL(HttpServletRequest httpRequest) {
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

        if (casTicket == null) {
            // no ticket found
            return null;
        }

        String userName = checkCasTicket(casTicket, httpRequest);
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
        if (parameters.containsKey(CAS2Parameters.NUXEO_APP_URL_KEY)) {
            appURL = parameters.get(CAS2Parameters.NUXEO_APP_URL_KEY);
        }
        if (parameters.containsKey(CAS2Parameters.SERVICE_LOGIN_URL_KEY)) {
            serviceLoginURL = parameters.get(CAS2Parameters.SERVICE_LOGIN_URL_KEY);
        }
        if (parameters.containsKey(CAS2Parameters.SERVICE_VALIDATE_URL_KEY)) {
            serviceValidateURL = parameters.get(CAS2Parameters.SERVICE_VALIDATE_URL_KEY);
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
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
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

    // Cas2 Ticket management
    private String checkCasTicket(String ticket, HttpServletRequest httpRequest) {
        ServiceTicketValidator ticketValidator = new ServiceTicketValidator();
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
            log.error("checkCasTicket failed with ParserConfigurationException:",e);
            return null;
        }

        log.debug("checkCasTicket : valdiation executed without error");
        String username = ticketValidator.getUser();
        log.debug("checkCasTicket : valdiation returned username = " + username);
        return username;
    }

}
