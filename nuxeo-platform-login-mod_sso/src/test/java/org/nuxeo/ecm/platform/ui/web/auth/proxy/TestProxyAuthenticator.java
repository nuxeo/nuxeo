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

package org.nuxeo.ecm.platform.ui.web.auth.proxy;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.mock.MockHttpServletRequest;
import org.jboss.seam.mock.MockHttpServletResponse;
import org.jboss.seam.mock.MockHttpSession;
import org.jboss.seam.mock.MockServletContext;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Patrick Turcotte
 */
public class TestProxyAuthenticator extends NXRuntimeTestCase {

	@Override
	public void setUp() throws Exception {
		super.setUp();

		deployBundle("org.nuxeo.ecm.platform.login.mod_sso.test");
		deployContrib("org.nuxeo.ecm.platform.login.mod_sso.test", "OSGI-INF/mock-usermanager-framework.xml");
	}

	public void testProxyAuthenticationWithReplacement() throws Exception {

		ProxyAuthenticator proxyAuth = new ProxyAuthenticator();

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("ssoHeaderName", "remote_user");
		parameters.put("ssoNeverRedirect", "true");
		String regexp = "@EXAMPLE.COM";
		parameters.put(ProxyAuthenticator.USERNAME_REMOVE_EXPRESSION, regexp);
		proxyAuth.initPlugin(parameters);
		
		String username = "test";
		String usernameAndUnwantedPart = username + "@EXAMPLE.COM";

		MockServletContext context = new MockServletContext();
		MockHttpSession session = new MockHttpSession(context);
		MockHttpServletRequest httpRequest = new MockHttpServletRequest(session, null, null, null, "GET");

		httpRequest.getHeaders().put("remote_user", new String[] {usernameAndUnwantedPart});

		HttpServletResponse httpResponse = new MockHttpServletResponse();

		UserIdentificationInfo identity = proxyAuth.handleRetrieveIdentity(httpRequest, httpResponse);
		
		assertNotNull(identity);
		assertEquals(username, identity.getUserName());
	}

}