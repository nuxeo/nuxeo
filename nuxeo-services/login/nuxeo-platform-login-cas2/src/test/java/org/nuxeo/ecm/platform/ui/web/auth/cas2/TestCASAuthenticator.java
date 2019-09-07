/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Academie de Rennes - proxy CAS support
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.cas2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.security.Principal;

import javax.security.auth.login.LoginContext;
import javax.servlet.ServletException;

import org.junit.Test;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.simple.AbstractAuthenticator;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @author Benjamin JALON
 */
@Deploy("org.nuxeo.ecm.platform.login.cas2")
@Deploy("org.nuxeo.ecm.platform.login.cas2.test:OSGI-INF/login-cas-contrib.xml")
public class TestCASAuthenticator extends AbstractAuthenticator {

    protected static final String TICKET_KEY = "ticket";

    @Test
    public void testCASAuthentication() throws Exception {
        initRequest();
        doAuthenticationToCasServer(CAS_USER);

        naf.doFilter(request, response, chain);

        String loginError = (String) request.getAttribute(NXAuthConstants.LOGIN_ERROR);
        LoginContext loginContext = (LoginContext) request.getAttribute(NXAuthConstants.LOGINCONTEXT_KEY);
        assertNull(loginError);
        assertNotNull(loginContext);
        assertEquals(CAS_USER, ((Principal) loginContext.getSubject().getPrincipals().toArray()[0]).getName());
    }

    /**
     * TODO : create a random number for the ticket, add it to the MockServiceValidators and associate this ticket to
     * the username
     */
    protected void doAuthenticationToCasServer(String username) throws ServletException {
        String casTicket = username;
        when(request.getParameter(eq(TICKET_KEY))).thenReturn(casTicket);
    }

}
