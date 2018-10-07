/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.ui.web.auth.simple;

import java.security.acl.Group;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.login.NuxeoAbstractServerLoginModule;

public class LoginModuleNo extends NuxeoAbstractServerLoginModule {

    protected NuxeoPrincipal identity;

    public boolean abort() throws LoginException {
        return true;
    }

    public boolean commit() throws LoginException {

        return false;
    }

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
    }

    public boolean login() throws LoginException {
        return false;
    }

    public boolean logout() throws LoginException {
        return true;
    }

    @Override
    protected NuxeoPrincipal getIdentity() {
        return null;
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        return null;
    }

    @Override
    protected NuxeoPrincipal createIdentity(String username) {
        return null;
    }

}
