/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
