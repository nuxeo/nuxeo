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

import java.util.HashMap;
import java.util.Map;

public abstract class BaseLoginModule implements LoginPlugin {

    protected String loginPage;

    protected String pluginName;

    protected Map<String, String> parameters = new HashMap<String, String>();


    // Not used.
    public String getLoginPage() {
        return loginPage;
    }

    // Not used.
    public void setLoginPage(String loginPage) {
        this.loginPage = loginPage;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getName() {
        return pluginName;
    }

    public void setName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getParameter(String parameterName) {
        return parameters.get(parameterName);
    }

}
