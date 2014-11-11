/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public UserIdentificationInfoCallbackHandler(
            UserIdentificationInfo userIdent) {
        this.userIdent = userIdent;
    }

    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {

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
            } else if (c.getClass().getName().equals(
                    "org.jboss.security.auth.callback.SecurityAssociationCallback")) {
                // we do nothing but do not raise error

            } else {
                throw new UnsupportedCallbackException(c,
                        "Unrecognized Callback:" + c.getClass().getName());
            }
        }
    }

}
