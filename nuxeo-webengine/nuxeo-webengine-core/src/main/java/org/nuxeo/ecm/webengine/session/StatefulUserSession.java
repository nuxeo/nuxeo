/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.webengine.session;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

public class StatefulUserSession extends UserSession implements HttpSessionBindingListener {

    public StatefulUserSession(Principal principal) {
        super(principal);
    }

    public StatefulUserSession(Principal principal, String password) {
        super(principal, password);
    }

    public StatefulUserSession(Principal principal, Object credentials) {
        super(principal, credentials);
    }

    private static final long serialVersionUID = 1L;

    public void valueBound(HttpSessionBindingEvent event) {
        // the user session was bound to the HTTP session
        install();
    }

    public void valueUnbound(HttpSessionBindingEvent event) {
        // the user session was removed from the HTTP session
        uninstall();
    }

    @Override
    public void terminateRequest(HttpServletRequest request) {
        // TODO Auto-generated method stub

    }

}
