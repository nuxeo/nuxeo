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

public class StatelessUserSession extends UserSession {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public StatelessUserSession(Principal principal) {
        super(principal);
    }

    public StatelessUserSession(Principal principal, String password) {
        super(principal, password);
    }

    public StatelessUserSession(Principal principal, Object credentials) {
        super(principal, credentials);
    }


    @Override
    public void terminateRequest(HttpServletRequest resuest) {
        uninstall();
    }

}
