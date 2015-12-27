/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.api.login;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * The default callback handler used by Framework.login methods.
 * <p>
 * This callback handler supports 3 types of callbacks:
 * <ul>
 * <li>the standard name callback
 * <li>the standard password callback
 * <li>a custom credentials callback that can be used to pass specific login information.
 * </ul>
 *
 * @author eionica@nuxeo.com
 */
public class CredentialsCallbackHandler implements CallbackHandler {

    private final String name;

    private final Object credentials;

    public CredentialsCallbackHandler(String username, Object credentials) {
        this.name = username;
        this.credentials = credentials;
    }

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        for (Callback cb : callbacks) {
            if (cb instanceof NameCallback) {
                ((NameCallback) cb).setName(name);
            } else if (cb instanceof PasswordCallback) {
                if (credentials instanceof CharSequence) {
                    // TODO cache the password to avoid recomputing it?
                    ((PasswordCallback) cb).setPassword(credentials.toString().toCharArray());
                } else if (credentials instanceof char[]) {
                    ((PasswordCallback) cb).setPassword((char[]) credentials);
                } else {
                    // the credentials are not in a password format. use null
                    ((PasswordCallback) cb).setPassword(null);
                }
            } else if (cb instanceof CredentialsCallback) {
                // if neither name or password callback are given use the generic credentials callback
                ((CredentialsCallback) cb).setCredentials(credentials);
            } else {
                throw new UnsupportedCallbackException(cb, "Unsupported callback " + cb + ". "
                        + "Only NameCallback, PasswordCallback and CredentialsCallback are supported by this handler.");
            }
        }
    }

}
