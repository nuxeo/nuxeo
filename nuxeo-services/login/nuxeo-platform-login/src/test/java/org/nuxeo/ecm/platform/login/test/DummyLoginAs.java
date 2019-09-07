/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.login.test;

import javax.security.auth.login.LoginException;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginAs;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;

/**
 * @since 10.3
 */
public class DummyLoginAs implements LoginAs {

    @Override
    public NuxeoLoginContext loginAs(String username) throws LoginException {
        return Framework.loginUser(username);
    }
}
