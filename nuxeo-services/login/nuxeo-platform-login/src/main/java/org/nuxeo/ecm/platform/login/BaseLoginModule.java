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
