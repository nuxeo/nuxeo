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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

@Deprecated
public class LoginHelper {

    private static final Log log = LogFactory.getLog(LoginHelper.class);

    private LoginPluginRegistry loginPluginManager;

    public LoginHelper() {
        try {
            loginPluginManager = (LoginPluginRegistry) Framework.getRuntime()
                    .getComponent(LoginPluginRegistry.NAME);

        } catch (Throwable t) {
            log.error(t.getMessage());
        }
    }

    public Boolean useCustomLoginPlugin() {
        return loginPluginManager.useCustomLoginPlugin();
    }

    public String getLoginPage() {
        return null; //loginPluginManager.getPlugin().getLoginPage();
    }

    public LoginPlugin getActivePlugin() {
        return loginPluginManager.getPlugin();
    }

}
