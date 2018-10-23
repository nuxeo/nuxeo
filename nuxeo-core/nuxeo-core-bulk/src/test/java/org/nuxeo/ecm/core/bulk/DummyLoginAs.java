/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.bulk;

import java.security.Principal;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.runtime.api.login.LoginAs;

/**
 * Computation rely on Framework.loginAsUser(String username).
 * <p>
 * This Framework wrapper method relies on the LoginAs service, thus we have to provide an implementation that allows
 * tests to be executed.
 *
 * @since 10.3
 */
public class DummyLoginAs implements LoginAs {

    @Override
    public LoginContext loginAs(String username) throws LoginException {
        Principal principal = new UserPrincipal(username, null, false, false);
        ClientLoginModule.getThreadLocalLogin().push(principal, null, null);
        return new LoginContext("nuxeo-client-login") {
            @Override
            public void logout() throws LoginException {
                ClientLoginModule.getThreadLocalLogin().pop();
            }
        };
    }
};
