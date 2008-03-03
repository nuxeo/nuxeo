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

package org.nuxeo.ecm.platform.ui.web.auth;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.jboss.security.auth.callback.SecurityAssociationCallback;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;

@Deprecated
public class JbossSecurityPropagationCallbackHandler implements CallbackHandler {

    protected final UserIdentificationInfo userIdent;

    public JbossSecurityPropagationCallbackHandler(UserIdentificationInfo userIdent) {
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
            } else if (c instanceof SecurityAssociationCallback) {
                SecurityAssociationCallback sa = (SecurityAssociationCallback) c;
                //sa.setPrincipal(userIdent.getPrincipal());
            } else {
                throw new UnsupportedCallbackException(c, "Unrecognized Callback");
            }
        }
    }

}
