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

package org.nuxeo.ecm.platform.ui.web.auth.proxy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;

public class ProxyAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(ProxyAuthenticator.class);

    private static final String HEADER_NAME_KEY = "ssoHeaderName";

    private static final String HEADER_NOREDIRECT_KEY = "ssoNeverRedirect";

    public static final String USERNAME_PARSE_EXPRESSION = "usernameParseExpression";

    protected String userIdHeaderName = "remote_user";
    
    protected String regexp = null;

    protected boolean noRedirect;

    public static final String HTTP_CREDENTIAL_DIRECTORY_FIELD_PROPERTY_NAME = "org.nuxeo.ecm.platform.login.mod_sso.credentialDirectoryField";

	private Pattern usernamePattern;


    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return false;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String userName = httpRequest.getHeader(userIdHeaderName);
        if (userName == null) {
            return null;
        }
        if (regexp != null) {
        	Matcher matcher = usernamePattern.matcher(userName);
        	userName = matcher.replaceAll("");
        }

        String credentialFieldName = Framework.getRuntime().getProperty(
                HTTP_CREDENTIAL_DIRECTORY_FIELD_PROPERTY_NAME);
        if (credentialFieldName != null) {
            // use custom directory field to find the user with the ID given in
            // the HTTP header
            Session userDir = null;
            try {
                String directoryName = Framework.getService(UserManager.class).getUserDirectoryName();
                userDir = Framework.getService(DirectoryService.class).open(
                        directoryName);
                Map<String, Serializable> queryFilters = new HashMap<String, Serializable>();
                queryFilters.put(credentialFieldName, userName);
                DocumentModelList result = userDir.query(queryFilters);
                if (result.isEmpty()) {
                    log.error(String.format(
                            "could not find any user with %s='%s' in directory %s",
                            credentialFieldName, userName, directoryName));
                    return null;
                }
                if (result.size() > 1) {
                    log.error(String.format(
                            "found more than one entry for  %s='%s' in directory %s",
                            credentialFieldName, userName, directoryName));
                    return null;
                }
                // use the ID of the found user entry as new identification for the principal
                userName = result.get(0).getId();
            } catch (Exception e) {
                log.error(String.format(
                        "could not retrieve user entry with %s='%s':  %s",
                        credentialFieldName, userName, e.getMessage()), e);
                return null;
            } finally {
                if (userDir != null) {
                    try {
                        userDir.close();
                    } catch (DirectoryException e) {
                        log.error("error while closing directory session: "
                                + e.getMessage(), e);
                    }
                }
            }
        }

        if (!noRedirect) {
            handleRedirectToValidStartPage(httpRequest, httpResponse);
        }
        return new UserIdentificationInfo(userName, userName);
    }

    /**
     * Handle redirection so that context is rebuilt correctly
     *
     * see NXP-2060 + NXP-2064
     */
    protected void handleRedirectToValidStartPage(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        boolean isStartPageValid = false;
        if (httpRequest.getMethod().equals("GET")
                || httpRequest.getMethod().equals("POST")) {
            // try to keep valid start page
            NuxeoAuthenticationFilter filter = new NuxeoAuthenticationFilter();
            isStartPageValid = filter.saveRequestedURLBeforeRedirect(
                    httpRequest, httpResponse);
        }
        HttpSession session;
        if (httpResponse.isCommitted()) {
            session = httpRequest.getSession(false);
        } else {
            session = httpRequest.getSession(true);
        }
        if (session != null && !isStartPageValid) {
            session.setAttribute(NXAuthConstants.START_PAGE_SAVE_KEY,
                    NuxeoAuthenticationFilter.DEFAULT_START_PAGE
                    + "?loginRedirection=true");
        }
    }

    public void initPlugin(Map<String, String> parameters) {
        if (parameters.containsKey(HEADER_NAME_KEY)) {
            userIdHeaderName = parameters.get(HEADER_NAME_KEY);
        }
        if (parameters.containsKey(HEADER_NOREDIRECT_KEY)) {
            noRedirect = Boolean.parseBoolean(parameters.get(HEADER_NOREDIRECT_KEY));
        }
        if (parameters.containsKey(USERNAME_PARSE_EXPRESSION)) {
        	regexp = parameters.get(USERNAME_PARSE_EXPRESSION);
        	usernamePattern = Pattern.compile(regexp);
        }
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return false;
    }

}
