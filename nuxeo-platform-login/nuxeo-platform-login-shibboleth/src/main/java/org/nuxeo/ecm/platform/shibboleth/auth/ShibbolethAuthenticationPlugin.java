/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.shibboleth.auth;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.shibboleth.service.ShibbolethAuthenticationConfig;
import org.nuxeo.ecm.platform.shibboleth.service.ShibbolethAuthenticationService;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

public class ShibbolethAuthenticationPlugin implements
        NuxeoAuthenticationPlugin, NuxeoAuthenticationPluginLogoutExtension {

    private static final Log log = LogFactory.getLog(ShibbolethAuthenticationPlugin.class);

    protected ShibbolethAuthenticationConfig config;

    protected ShibbolethAuthenticationConfig getConfig() {
        if (config == null) {
            try {
                ShibbolethAuthenticationService service = Framework.getService(ShibbolethAuthenticationService.class);
                config = service.getConfig();
            } catch (Exception e) {
                log.error(
                        "Failed to load Shibboleth authentication configuration",
                        e);
            }
        }
        return config;
    }

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        ShibbolethAuthenticationConfig config = getConfig();
        if (config == null) {
            return false;
        }
        String loginURL = config.getLoginURL();
        try {
            if (loginURL == null) {
                log.error("Unable to handle Shibboleth login, no loginURL registered");
                return false;
            }
            loginURL = loginURL + "?target="
                    + VirtualHostHelper.getBaseURL(httpRequest);
            httpResponse.sendRedirect(loginURL);
        } catch (IOException e) {
            String errorMessage = String.format(
                    "Unable to handle Shibboleth login on %s", loginURL);
            log.error(errorMessage, e);
            return false;
        }
        return true;
    }

    @Override
    public Boolean handleLogout(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        ShibbolethAuthenticationConfig config = getConfig();
        if (config == null) {
            return false;
        }
        String logoutURL = config.getLogoutURL();
        if (logoutURL == null) {
            return false;
        }
        try {
            logoutURL = logoutURL + "?return="
                    + VirtualHostHelper.getBaseURL(httpRequest);
            httpResponse.sendRedirect(logoutURL);
        } catch (IOException e) {
            log.error("Unable to handle Shibboleth logout", e);
            return false;
        }
        return true;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        ShibbolethAuthenticationConfig config = getConfig();
        if (config == null) {
            return null;
        }

        String username = httpRequest.getHeader(config.getUidHeader());
        if (username == null || "".equals(username)) {
            return null;
        }
        Session userDir = null;
        try {
            UserManager userManager = Framework.getService(UserManager.class);
            userDir = Framework.getService(DirectoryService.class).open(
                    userManager.getUserDirectoryName());
            Map<String, Object> fieldMap = new HashMap<String, Object>();
            for (String key : config.getFieldMapping().keySet()) {
                fieldMap.put(config.getFieldMapping().get(key),
                        httpRequest.getHeader(key));
            }
            DocumentModel entry = userDir.getEntry(username);
            if (entry == null) {
                userDir.createEntry(fieldMap);
            } else {
                entry.getDataModel(userManager.getUserSchemaName()).setMap(
                        fieldMap);
                userDir.updateEntry(entry);
            }
            userDir.commit();
        } catch (Exception e) {
            log.error("Failed to get or create user entry", e);
        } finally {
            if (userDir != null) {
                try {
                    userDir.close();
                } catch (DirectoryException e) {
                    log.error("Error while closing directory session", e);
                }
            }
        }

        return new UserIdentificationInfo(username, username);
    }

    public void initPlugin(Map<String, String> parameters) {
    }

    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

}
