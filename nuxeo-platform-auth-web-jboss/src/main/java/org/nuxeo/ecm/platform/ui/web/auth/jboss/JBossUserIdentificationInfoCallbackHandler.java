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

package org.nuxeo.ecm.platform.ui.web.auth.jboss;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jboss.security.auth.callback.ObjectCallback;
import org.jboss.security.auth.callback.SecurityAssociationCallback;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfoCallbackHandler;

public class JBossUserIdentificationInfoCallbackHandler extends
        UserIdentificationInfoCallbackHandler {

    public JBossUserIdentificationInfoCallbackHandler(
            UserIdentificationInfo userIdent) {
        super(userIdent);
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {

        for (Callback c : callbacks) {
            if (c instanceof ObjectCallback) {
                ObjectCallback oc = (ObjectCallback) c;
                oc.setCredential(userIdent);
            } else if (c instanceof SecurityAssociationCallback) {
                SecurityAssociationCallback sac = (SecurityAssociationCallback) c;
                sac.setPrincipal(null);
                sac.setCredential(userIdent);
            } else {
                super.handle(callbacks);
            }
        }
    }

}
