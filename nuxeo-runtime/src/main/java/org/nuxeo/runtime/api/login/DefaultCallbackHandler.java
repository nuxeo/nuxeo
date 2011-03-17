/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Copied from jbossx.
 *
 * @author Scott.Stark@jboss.org
 */
public class DefaultCallbackHandler implements CallbackHandler {

    private final String username;

    private char[] password;

    private final Object credential;

    /**
     * Initializes the UsernamePasswordHandler with the username and password to
     * use.
     *
     * @param username the user name
     * @param password the password for this user
     */
    public DefaultCallbackHandler(String username, char[] password) {
        this.username = username;
        this.password = password;
        credential = password;
    }

    public DefaultCallbackHandler(String username, Object credential) {
        this.username = username;
        this.credential = credential;
        if (credential instanceof char[]) {
            password = (char[]) credential;
        } else if (credential instanceof CharSequence) {
            password = credential.toString().toCharArray();
        }
    }

    /**
     * Sets any NameCallback name property to the instance username, sets any
     * PasswordCallback password property to the instance, and any password.
     *
     * @exception UnsupportedCallbackException,
     *                thrown if any callback of type other than NameCallback or
     *                PasswordCallback are seen.
     */
    @Override
    public void handle(Callback[] callbacks)
            throws UnsupportedCallbackException {
        for (Callback c : callbacks) {
            if (c instanceof NameCallback) {
                NameCallback nc = (NameCallback) c;
                nc.setName(username);
            } else if (c instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) c;
                if (password == null) {
                    // We were given an opaque Object credential but a char[] is
                    // requested?
                    if (credential != null) {
                        String tmp = credential.toString();
                        password = tmp.toCharArray();
                    }
                }
                pc.setPassword(password);
            } else if (c instanceof DefaultCallback) {
                DefaultCallback oc = (DefaultCallback) c;
                oc.setCredential(credential);
            } else {
                throw new UnsupportedCallbackException(c,
                        "Unrecognized Callback");
            }
        }
    }

}
