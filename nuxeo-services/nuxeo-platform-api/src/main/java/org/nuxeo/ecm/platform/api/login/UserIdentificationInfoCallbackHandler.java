/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.api.login;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class UserIdentificationInfoCallbackHandler implements CallbackHandler {

    protected final UserIdentificationInfo userIdent;

    public UserIdentificationInfoCallbackHandler(UserIdentificationInfo userIdent) {
        this.userIdent = userIdent;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

        for (Callback c : callbacks) {
            if (c instanceof NameCallback) {
                String username = userIdent.getUserName();
                NameCallback nc = (NameCallback) c;
                nc.setName(username);
            } else if (c instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) c;
                char[] password = userIdent.getPassword().toCharArray();
                pc.setPassword(password);
            } else if (c instanceof UserIdentificationInfoCallback) {
                UserIdentificationInfoCallback uic = (UserIdentificationInfoCallback) c;
                uic.setUserInfo(userIdent);
            } else if (c.getClass().getName().equals("org.jboss.security.auth.callback.SecurityAssociationCallback")) {
                // we do nothing but do not raise error

            } else {
                throw new UnsupportedCallbackException(c, "Unrecognized Callback:" + c.getClass().getName());
            }
        }
    }

}
