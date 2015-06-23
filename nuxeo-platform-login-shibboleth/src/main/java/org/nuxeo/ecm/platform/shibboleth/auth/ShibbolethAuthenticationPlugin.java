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
import org.nuxeo.ecm.platform.shibboleth.service.ShibbolethAuthenticationService;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPluginLogoutExtension;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class ShibbolethAuthenticationPlugin implements NuxeoAuthenticationPlugin,
        NuxeoAuthenticationPluginLogoutExtension {

    private static final Log log = LogFactory.getLog(ShibbolethAuthenticationPlugin.class);

    protected ShibbolethAuthenticationService service;

    protected ShibbolethAuthenticationService getService() {
        if (service == null) {
            try {
                service = Framework.getService(ShibbolethAuthenticationService.class);
            } catch (Exception e) {
                log.error("Failed to get Shibboleth authentication service", e);
            }
        }
        return service;
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        if (getService() == null) {
            return false;
        }
        String loginURL = getService().getLoginURL(httpRequest);
        if (loginURL == null) {
            log.error("Unable to handle Shibboleth login, no loginURL registered");
            return false;
        }
        try {
            httpResponse.sendRedirect(loginURL);
        } catch (IOException e) {
            String errorMessage = String.format("Unable to handle Shibboleth login on %s", loginURL);
            log.error(errorMessage, e);
            return false;
        }
        return true;
    }

    @Override
    public Boolean handleLogout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (getService() == null) {
            return false;
        }
        String logoutURL = getService().getLogoutURL(httpRequest);
        if (logoutURL == null) {
            return false;
        }
        try {
            httpResponse.sendRedirect(logoutURL);
        } catch (IOException e) {
            log.error("Unable to handle Shibboleth logout", e);
            return false;
        }
        return true;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (getService() == null) {
            return null;
        }

        String userId = getService().getUserID(httpRequest);
        if (userId == null || "".equals(userId)) {
            return null;
        }
        UserManager userManager = Framework.getService(UserManager.class);
        try (Session userDir = Framework.getService(DirectoryService.class).open(userManager.getUserDirectoryName())){
            Map<String, Object> fieldMap = getService().getUserMetadata(userManager.getUserIdField(), httpRequest);
            DocumentModel entry = userDir.getEntry(userId);
            if (entry == null) {
                userDir.createEntry(fieldMap);
            } else {
                entry.getDataModel(userManager.getUserSchemaName()).setMap(fieldMap);
                userDir.updateEntry(entry);
            }
        } catch (Exception e) {
            log.error("Failed to get or create user entry", e);
        }

        return new UserIdentificationInfo(userId, userId);
    }

    @Override
    public void initPlugin(Map<String, String> parameters) {
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return true;
    }

}
