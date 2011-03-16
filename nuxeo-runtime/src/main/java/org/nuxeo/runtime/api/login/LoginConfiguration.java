/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.runtime.api.login;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * Custom login configuration.
 * <p>
 * This configuration reads login-modules configuration from extensions
 * contributed to the extension point exposed by the component LoginComponent.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LoginConfiguration extends Configuration {

    private final LoginComponent login;
    private final Configuration parent;

    LoginConfiguration(LoginComponent login) {
        this(login, null);
    }

    LoginConfiguration(LoginComponent login, Configuration parent) {
        this.login = login;
        this.parent = parent;
    }

    public Configuration getParent() {
        return parent;
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        AppConfigurationEntry[] appConfig = login.getAppConfigurationEntry(name);
        if (appConfig == null && parent != null) { // delegate to parent config
            appConfig = parent.getAppConfigurationEntry(name);
        }
        return appConfig;
    }

    @Override
    public void refresh() {
        // do nothing for our configuration (it refreshes itself each time the
        // config is modified)
        // refresh the parent if any
        if (parent != null) {
            parent.refresh();
        }
    }

}
