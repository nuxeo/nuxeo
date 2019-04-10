/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.security.Principal;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.opencmis.bindings.LoginProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Login provider that does not check the password and just logs in the provided user.
 */
public class TrustingLoginProvider implements LoginProvider {

    @Override
    public LoginContext login(String username, String password) throws LoginException {
        LoginContext loginContext = Framework.loginAsUser(username);
        Object[] principals = loginContext.getSubject().getPrincipals().toArray();
        if (principals.length > 0) {
            Principal principal = (Principal) principals[0];
            TrustingNuxeoAuthFilter.maybeMakeAdministrator(principal);
        }
        return loginContext;
    }

}
