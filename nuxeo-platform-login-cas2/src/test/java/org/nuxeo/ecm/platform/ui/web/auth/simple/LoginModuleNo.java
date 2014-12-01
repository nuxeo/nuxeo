/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.ui.web.auth.simple;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.nuxeo.ecm.platform.login.NuxeoAbstractServerLoginModule;

public class LoginModuleNo extends NuxeoAbstractServerLoginModule {

    protected Principal identity;

    public boolean abort() throws LoginException {
        return true;
    }

    public boolean commit() throws LoginException {

        return false;
    }

    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
    }

    public boolean login() throws LoginException {
        return false;
    }

    public boolean logout() throws LoginException {
        return true;
    }

    @Override
    protected Principal getIdentity() {
        return null;
    }

    @Override
    protected Group[] getRoleSets() throws LoginException {
        return null;
    }

    @Override
    protected Principal createIdentity(String username) {
        return null;
    }

}
