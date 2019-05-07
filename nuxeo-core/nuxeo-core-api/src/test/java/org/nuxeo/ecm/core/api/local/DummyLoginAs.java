/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.api.local;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.runtime.api.login.LoginAs;

/**
 * Dummy {@link LoginAs} implementation which logs the given user into the application.
 *
 * @since 11.1
 */
public class DummyLoginAs implements LoginAs {

    @Override
    @SuppressWarnings("deprecation")
    public LoginContext loginAs(String username) throws LoginException {
        // do a dummy anonymous/administrator control based on username
        boolean isAnonymous = SecurityConstants.ANONYMOUS.equals(username);
        boolean isAdministrator = SecurityConstants.ADMINISTRATOR.equals(username);
        Principal principal = new UserPrincipal(username, null, isAnonymous, isAdministrator);
        // push it to the login stack and create context
        Subject subject = new Subject(false, Set.of(principal), Set.of(), Set.of());
        ClientLoginModule.getThreadLocalLogin().push(principal, null, subject);
        return new LoginContext("nuxeo-client-login", subject) {
            @Override
            public void logout() {
                ClientLoginModule.getThreadLocalLogin().pop();
            }
        };
    }
}