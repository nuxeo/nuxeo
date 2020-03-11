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

import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADMINISTRATOR;

import java.security.Principal;

import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.runtime.api.login.LoginAs;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;

/**
 * Dummy {@link LoginAs} implementation which logs the given user into the application.
 *
 * @since 11.1
 */
public class DummyLoginAs implements LoginAs {

    @Override
    public NuxeoLoginContext loginAs(String username) throws LoginException {
        boolean administrator = ADMINISTRATOR.equals(username);
        Principal principal = new UserPrincipal(username, null, false, administrator);
        NuxeoLoginContext loginContext = NuxeoLoginContext.create(principal);
        loginContext.login();
        return loginContext;
    }
}
