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

package org.nuxeo.ecm.platform.ui.web.auth.shib;

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
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

public class ShibbolethAuthenticationPlugin implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(ShibbolethAuthenticationPlugin.class);

    protected ShibbolethAuthenticationConfig config;

    public List<String> getUnAuthenticatedURLPrefix() {
        return null;
    }

    public Boolean handleLoginPrompt(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String baseURL) {
        return false;
    }

    public UserIdentificationInfo handleRetrieveIdentity(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (config == null) {
            try {
                ShibbolethAuthenticationService service = Framework.getService(ShibbolethAuthenticationService.class);
                config = service.getConfig();
            } catch (Exception e) {
                log.error("Failed to load Shibboleth authentication configuration", e);
            }
        }
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
                fieldMap.put(config.getFieldMapping().get(key), httpRequest.getHeader(key));
            }
            DocumentModel entry = userDir.getEntry(username);
            if (entry == null) {
                userDir.createEntry(fieldMap);
            } else {
                entry.getDataModel(userManager.getUserSchemaName()).setMap(fieldMap);
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
        return false;
    }

}
