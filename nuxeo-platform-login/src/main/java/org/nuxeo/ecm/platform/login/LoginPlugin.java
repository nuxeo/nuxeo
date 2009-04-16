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

package org.nuxeo.ecm.platform.login;

import java.util.Map;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;

public interface LoginPlugin {

    //public String getLoginPage();

    //public void setLoginPage(String loginPage);

    Boolean initLoginModule();

    /**
     * Gets the plugin name.
     *
     * @return the plugin name.
     */
    String getName();

    /**
     * Sets the plugin name.
     *
     * @param pluginName the plugin name.
     */
    // Not used. Remove?
    void setName(String pluginName);

    Map<String, String> getParameters();

    void setParameters(Map<String, String> parameters);

    String getParameter(String parameterName);

    //public String validateUsernamePassword(String username, String password) throws Exception;

    String validatedUserIdentity(UserIdentificationInfo userIdent);

}
