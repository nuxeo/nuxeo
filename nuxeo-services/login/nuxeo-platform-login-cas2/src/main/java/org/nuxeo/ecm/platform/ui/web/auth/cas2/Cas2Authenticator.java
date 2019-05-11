/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Academie de Rennes - proxy CAS support
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.LoginResponseHandler;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.xml.sax.SAXException;

import edu.yale.its.tp.cas.client.ProxyTicketValidator;
import edu.yale.its.tp.cas.client.ServiceTicketValidator;

/**
 * @author Thierry Delprat
 * @author Olivier Adam
 * @author M.-A. Darche
 * @author Benjamin Jalon
 * @author Thierry Martins
 */
public class Cas2Authenticator
        implements NuxeoAuthenticationPlugin, NuxeoAuthenticationPluginLogoutExtension, LoginResponseHandler {

    protected static final String CAS_SERVER_HEADER_KEY = "CasServer";

    protected static final String CAS_SERVER_PATTERN_KEY = "$CASSERVER";

    protected static final String NUXEO_SERVER_PATTERN_KEY = "$NUXEO";

    protected static final String LOGIN_ACTION = "Login";

    protected static final String LOGOUT_ACTION = "Logout";

    protected static final String VALIDATE_ACTION = "Valid";

    protected static final String PROXY_VALIDATE_ACTION = "ProxyValid";

    protected static final Log log = LogFactory.getLog(Cas2Authenticator.class);

    protected static final String EXCLUDE_PROMPT_KEY = "excludePromptURL";

    protected static final String ALTERNATIVE_AUTH_PLUGIN_COOKIE_NAME = "org.nuxeo.auth.plugin.alternative";

    protected String ticketKey = "ticket";

    protected String proxyKey = "proxy";

    protected String appURL = "http://127.0.0.1:8080/nuxeo/";

    protected String serviceLoginURL = "http://127.0.0.1:8080/cas/login";

    protected String serviceValidateURL = "http://127.0.0.1:8080/cas/serviceValidate";

    /**
     * We tell the CAS server whether we want a plain text (CAS 1.0) or XML (CAS 2.0) response by making the request
     * either to the '.../validate' or '.../serviceValidate' URL. The older protocol supports only the CAS 1.0
     * functionality, which is left around as the legacy '.../validate' URL.
     */
    protected String proxyValidateURL = "http://127.0.0.1:8080/cas/proxyValidate";

    protected String serviceKey = "service";

    protected String logoutURL = "";

    protected String defaultCasServer = "";

    protected String ticketValidatorClassName = "edu.yale.its.tp.cas.client.ServiceTicketValidator";

    protected String proxyValidatorClassName = "edu.yale.its.tp.cas.client.ProxyTicketValidator";

    protected boolean promptLogin = true;

    protected List<String> excludePromptURLs;

    protected String errorPage;

    @Override
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
            if (serverURL != null) {
                url = url.replace(CAS_SERVER_PATTERN_KEY, serverURL);
            } else {
                if (url.contains(CAS_SERVER_PATTERN_KEY)) {
                    url = url.replace(CAS_SERVER_PATTERN_KEY, defaultCasServer);
                }
            }
        }
        log.debug("serviceUrl: " + url);
        return url;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {

        // Check for an alternative authentication plugin in request cookies
        NuxeoAuthenticationPlugin alternativeAuthPlugin = getAlternativeAuthPlugin(httpRequest, httpResponse);
        if (alternativeAuthPlugin != null) {
            log.debug(String.format("Found alternative authentication plugin %s, using it to handle login prompt.",
                    alternativeAuthPlugin));
            return alternativeAuthPlugin.handleLoginPrompt(httpRequest, httpResponse, baseURL);
        }

        // Redirect to CAS Login screen
        // passing our application URL as service name
        String location = null;
        try {
            Map<String, String> urlParameters = new HashMap<>();
            urlParameters.put("service", getAppURL(httpRequest));
            location = URIUtils.addParametersToURIQuery(getServiceURL(httpRequest, LOGIN_ACTION), urlParameters);
            httpResponse.sendRedirect(location);
        } catch (IOException e) {
            log.error("Unable to redirect to CAS login screen to " + location, e);
            return false;
        }
        return true;
    }

    protected String getAppURL(HttpServletRequest httpRequest) {
        if (isValidStartupPage(httpRequest)) {
            StringBuilder sb = new StringBuilder(VirtualHostHelper.getServerURL(httpRequest));
            if (VirtualHostHelper.getServerURL(httpRequest).endsWith("/")) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append(httpRequest.getRequestURI());
            if (httpRequest.getQueryString() != null) {
                sb.append("?");
                sb.append(httpRequest.getQueryString());

                // remove ticket parameter from URL to correctly validate the
                // service
                int indexTicketKey = sb.lastIndexOf(ticketKey + "=");
                if (indexTicketKey != -1) {
                    sb.delete(indexTicketKey - 1, sb.length());
                }
            }

            return sb.toString();
        }
        if (appURL == null || appURL.equals("")) {
            appURL = NUXEO_SERVER_PATTERN_KEY;
        }
        if (appURL.contains(NUXEO_SERVER_PATTERN_KEY)) {
            String nxurl = VirtualHostHelper.getBaseURL(httpRequest);
            return appURL.replace(NUXEO_SERVER_PATTERN_KEY, nxurl);
        } else {
            return appURL;
        }
    }

    private boolean isValidStartupPage(HttpServletRequest httpRequest) {
        if (httpRequest.getRequestURI() == null) {
            return false;
        }
        PluggableAuthenticationService service = Framework.getService(PluggableAuthenticationService.class);
        if (service == null) {
            return false;
        }
        String startPage = httpRequest.getRequestURI().replace(VirtualHostHelper.getContextPath(httpRequest) + "/", "");
        for (String prefix : service.getStartURLPatterns()) {
            if (startPage.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
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

        UserIdentificationInfo uui = new UserIdentificationInfo(userName, casTicket);
        uui.setToken(casTicket);

        return uui;
    }

    @Override
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
        excludePromptURLs = new ArrayList<>();
        for (String key : parameters.keySet()) {
            if (key.startsWith(EXCLUDE_PROMPT_KEY)) {
                excludePromptURLs.add(parameters.get(key));
            }
        }
        if (parameters.containsKey(CAS2Parameters.ERROR_PAGE)) {
            errorPage = parameters.get(CAS2Parameters.ERROR_PAGE);
        }
    }

    @Override
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

    @Override
    public Boolean handleLogout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        // Check for an alternative authentication plugin in request cookies
        NuxeoAuthenticationPlugin alternativeAuthPlugin = getAlternativeAuthPlugin(httpRequest, httpResponse);
        if (alternativeAuthPlugin != null) {
            if (alternativeAuthPlugin instanceof NuxeoAuthenticationPluginLogoutExtension) {
                log.debug(String.format("Found alternative authentication plugin %s, using it to handle logout.",
                        alternativeAuthPlugin));
                return ((NuxeoAuthenticationPluginLogoutExtension) alternativeAuthPlugin).handleLogout(httpRequest,
                        httpResponse);
            } else {
                log.debug(String.format(
                        "Found alternative authentication plugin %s which cannot handle logout, letting authentication filter handle it.",
                        alternativeAuthPlugin));
                return false;
            }
        }

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

    protected String checkProxyCasTicket(String ticket, HttpServletRequest httpRequest) {
        // Get the service passed by the portlet
        String service = httpRequest.getParameter(serviceKey);
        if (service == null) {
            log.error("checkProxyCasTicket: no service name in the URL");
            return null;
        }

        ProxyTicketValidator proxyValidator;
        try {
            proxyValidator = (ProxyTicketValidator) Framework.getRuntime()
                                                             .getContext()
                                                             .loadClass(proxyValidatorClassName)
                                                             .getDeclaredConstructor()
                                                             .newInstance();
        } catch (ReflectiveOperationException e) {
            log.error("checkProxyCasTicket during the ProxyTicketValidator initialization", e);
            return null;
        }

        proxyValidator.setCasValidateUrl(getServiceURL(httpRequest, PROXY_VALIDATE_ACTION));
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
            log.error("checkProxyCasTicket failed with ParserConfigurationException:", e);
            return null;
        }
        log.debug("checkProxyCasTicket: validation executed without error");
        String username = proxyValidator.getUser();
        log.debug("checkProxyCasTicket: validation returned username = " + username);

        return username;
    }

    // Cas2 Ticket management
    protected String checkCasTicket(String ticket, HttpServletRequest httpRequest) {
        ServiceTicketValidator ticketValidator;
        try {
            ticketValidator = (ServiceTicketValidator) Framework.getRuntime()
                                                                .getContext()
                                                                .loadClass(ticketValidatorClassName)
                                                                .getDeclaredConstructor()
                                                                .newInstance();
        } catch (ReflectiveOperationException e) {
            log.error("checkCasTicket during the ServiceTicketValidator initialization", e);
            return null;
        }

        ticketValidator.setCasValidateUrl(getServiceURL(httpRequest, VALIDATE_ACTION));
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
            log.error("checkCasTicket failed with ParserConfigurationException:", e);
            return null;
        }
        log.debug("checkCasTicket : validation executed without error");
        String username = ticketValidator.getUser();
        log.debug("checkCasTicket: validation returned username = " + username);
        return username;
    }

    @Override
    public boolean onError(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            if (errorPage != null) {
                response.sendRedirect(errorPage);
            }
        } catch (IOException e) {
            log.error(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean onSuccess(HttpServletRequest arg0, HttpServletResponse arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    protected NuxeoAuthenticationPlugin getAlternativeAuthPlugin(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        Cookie alternativeAuthPluginCookie = getCookie(httpRequest, ALTERNATIVE_AUTH_PLUGIN_COOKIE_NAME);
        if (alternativeAuthPluginCookie != null) {
            String alternativeAuthPluginName = alternativeAuthPluginCookie.getValue();
            PluggableAuthenticationService authService = Framework.getService(PluggableAuthenticationService.class);
            NuxeoAuthenticationPlugin alternativeAuthPlugin = authService.getPlugin(alternativeAuthPluginName);
            if (alternativeAuthPlugin == null) {
                log.error(String.format("No alternative authentication plugin named %s, will remove cookie %s.",
                        alternativeAuthPluginName, ALTERNATIVE_AUTH_PLUGIN_COOKIE_NAME));
                removeCookie(httpRequest, httpResponse, alternativeAuthPluginCookie);
            } else {
                return alternativeAuthPlugin;
            }
        }
        return null;
    }

    protected Cookie getCookie(HttpServletRequest httpRequest, String cookieName) {
        Cookie cookies[] = httpRequest.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookieName.equals(cookies[i].getName())) {
                    return cookies[i];
                }
            }
        }
        return null;
    }

    protected void removeCookie(HttpServletRequest httpRequest, HttpServletResponse httpResponse, Cookie cookie) {
        log.debug(String.format("Removing cookie %s.", cookie.getName()));
        cookie.setMaxAge(0);
        cookie.setValue("");
        cookie.setPath(httpRequest.getContextPath());
        httpResponse.addCookie(cookie);
    }
}
