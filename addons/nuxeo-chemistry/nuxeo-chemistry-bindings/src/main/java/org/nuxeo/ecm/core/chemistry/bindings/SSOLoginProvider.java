/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.bindings;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfoCallbackHandler;

/**
 * Login provider that does not check the password and just logs in the provided
 * user.
 */
public class SSOLoginProvider implements LoginProvider {

    public LoginContext login(String username, String password)
            throws LoginException {
        UserIdentificationInfo userIdent = new UserIdentificationInfo(username, "");
        userIdent.setLoginPluginName("Trusting_LM");
        CallbackHandler handler = new UserIdentificationInfoCallbackHandler(userIdent);
        LoginContext loginContext = new LoginContext("nuxeo-ecm-web", handler);
        loginContext.login();
        return loginContext;
    }

}
