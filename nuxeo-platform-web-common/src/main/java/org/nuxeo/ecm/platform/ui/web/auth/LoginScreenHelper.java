/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.ui.web.auth;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginProviderLinkComputer;
import org.nuxeo.ecm.platform.ui.web.auth.service.LoginScreenConfig;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple helper class for easy access form the login.jsp page
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public class LoginScreenHelper {

    public static LoginScreenConfig getConfig() {
        PluggableAuthenticationService authService = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                PluggableAuthenticationService.NAME);
        return authService.getLoginScreenConfig();
    }

    public static void registerLoginProvider(String name, String iconUrl,
            String link, String label, String description,
            LoginProviderLinkComputer computer) throws ClientException {

        LoginScreenConfig config = getConfig();
        if (config != null) {
            config.registerLoginProvider(name, iconUrl, link, label,
                    description, computer);
        } else {
            throw new ClientException(
                    "There is no available LoginScreen config");
        }
    }

    public static String getValueWithDefault(String value, String defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

}
