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
import org.nuxeo.ecm.platform.ui.web.auth.LoginScreenHelper;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class ProxyAuthenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(ProxyAuthenticator.class);

    private static final String HEADER_NAME_KEY = "ssoHeaderName";

    private static final String HEADER_NOREDIRECT_KEY = "ssoNeverRedirect";

    public static final String USERNAME_REMOVE_EXPRESSION = "usernameUnwantedPartExpression";

    protected String userIdHeaderName = "remote_user";

    protected String regexp = null;

    protected boolean noRedirect;

    public static final String HTTP_CREDENTIAL_DIRECTORY_FIELD_PROPERTY_NAME = "org.nuxeo.ecm.platform.login.mod_sso.credentialDirectoryField";

    private Pattern usernamePartRemovalPattern;

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        return false;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String userName = httpRequest.getHeader(userIdHeaderName);
        if (userName == null) {
            return null;
        }
        if (regexp != null && usernamePartRemovalPattern != null) {
            String tmpUsername = userName;
            Matcher matcher = usernamePartRemovalPattern.matcher(userName);
            // Remove all instance of regexp from username string
            userName = matcher.replaceAll("");
            log.debug(String.format("userName changed from '%s' to '%s'", tmpUsername, userName));
        }

        String credentialFieldName = Framework.getRuntime().getProperty(HTTP_CREDENTIAL_DIRECTORY_FIELD_PROPERTY_NAME);
        if (credentialFieldName != null) {
            // use custom directory field to find the user with the ID given in
            // the HTTP header
            String directoryName = Framework.getService(UserManager.class).getUserDirectoryName();
            try (Session userDir = Framework.getService(DirectoryService.class).open(directoryName)) {
                Map<String, Serializable> queryFilters = new HashMap<>();
                queryFilters.put(credentialFieldName, userName);
                DocumentModelList result = userDir.query(queryFilters);
                if (result.isEmpty()) {
                    log.error(String.format("could not find any user with %s='%s' in directory %s", credentialFieldName,
                            userName, directoryName));
                    return null;
                }
                if (result.size() > 1) {
                    log.error(String.format("found more than one entry for  %s='%s' in directory %s",
                            credentialFieldName, userName, directoryName));
                    return null;
                }
                // use the ID of the found user entry as new identification for
                // the principal
                userName = result.get(0).getId();
            } catch (DirectoryException e) {
                log.error(String.format("could not retrieve user entry with %s='%s':  %s", credentialFieldName,
                        userName, e.getMessage()), e);
                return null;
            }
        }

        if (!noRedirect) {
            handleRedirectToValidStartPage(httpRequest, httpResponse);
        }
        return new UserIdentificationInfo(userName);
    }

    /**
     * Handle redirection so that context is rebuilt correctly see NXP-2060 + NXP-2064
     */
    protected void handleRedirectToValidStartPage(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        boolean isStartPageValid = false;
        if (httpRequest.getMethod().equals("GET") || httpRequest.getMethod().equals("POST")) {
            // try to keep valid start page
            NuxeoAuthenticationFilter filter = new NuxeoAuthenticationFilter();
            isStartPageValid = filter.saveRequestedURLBeforeRedirect(httpRequest, httpResponse);
        }
        HttpSession session;
        if (httpResponse.isCommitted()) {
            session = httpRequest.getSession(false);
        } else {
            session = httpRequest.getSession(true);
        }
        if (session != null && !isStartPageValid) {
            session.setAttribute(NXAuthConstants.START_PAGE_SAVE_KEY,
                    LoginScreenHelper.getStartupPagePath() + "?loginRedirection=true");
        }
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {
        if (parameters.containsKey(HEADER_NAME_KEY)) {
            userIdHeaderName = parameters.get(HEADER_NAME_KEY);
        }
        if (parameters.containsKey(HEADER_NOREDIRECT_KEY)) {
            noRedirect = Boolean.parseBoolean(parameters.get(HEADER_NOREDIRECT_KEY));
        }
        if (parameters.containsKey(USERNAME_REMOVE_EXPRESSION)) {
            regexp = parameters.get(USERNAME_REMOVE_EXPRESSION);
            log.debug(String.format("Will remove all instances of '%s' from userName string.", regexp));
            usernamePartRemovalPattern = Pattern.compile(regexp);
        }
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return false;
    }

}
