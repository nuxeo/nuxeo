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

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

public class NuxeoSecuredRequestWrapper extends HttpServletRequestWrapper {

    protected final Principal principal;

    public NuxeoSecuredRequestWrapper(HttpServletRequest request) {
        this(request, null);
    }

    public NuxeoSecuredRequestWrapper(HttpServletRequest request, Principal principal) {
        super(request);
        this.principal = principal;
    }

    @Override
    public Principal getUserPrincipal() {
        if (principal != null) {
            return principal;
        } else {
            HttpSession session = getSession(false);
            if (session == null) {
                return null;
            } else {
                return (Principal) session.getAttribute(NXAuthConstants.PRINCIPAL_KEY);
            }
        }
    }

    /*public boolean isUserInRole(String arg0)
     {
         principal.
     }*/
}
