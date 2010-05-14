/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Academie de Rennes - proxy CAS support
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.simple;

import javax.security.auth.login.LoginContext;

import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;

import java.security.Principal;

/**
 * @author Benjamin JALON
 */
public class TestAuthenticator extends AbstractAuthenticator {

    public void testAuthentication() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.login.cas2.test",
                "OSGI-INF/login-yes-contrib.xml");

        initRequest();
        setLoginPasswordInHeader("Administrator", "Administrator", request);

        naf.doFilter(request, response, chain);

        String loginError = (String) request.getAttribute(NXAuthConstants.LOGIN_ERROR);
        LoginContext loginContext = (LoginContext) request.getAttribute("org.nuxeo.ecm.login.context");
        assertNull(loginError);
        assertNotNull(loginContext);
        assertEquals("Administrator", ((Principal) loginContext.getSubject().getPrincipals().toArray()[0]).getName());
    }

    public void testNoAuthentication() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.login.cas2.test",
                "OSGI-INF/login-no-contrib.xml");
        
        initRequest();
        setLoginPasswordInHeader("Administrator", "Administrator", request);

        naf.doFilter(request, response, chain);

        String loginError = (String) request.getAttribute(NXAuthConstants.LOGIN_ERROR);
        LoginContext loginContext = (LoginContext) request.getAttribute("org.nuxeo.ecm.login.context");
        assertEquals("authentication.failed", loginError);
        assertNull(loginContext);
    }

}
