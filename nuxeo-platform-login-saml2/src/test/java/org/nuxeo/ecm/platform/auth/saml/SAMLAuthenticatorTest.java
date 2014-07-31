/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Matchers.notNull;

import static junit.framework.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.opensaml.xml.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.directory.api",
          "org.nuxeo.ecm.directory",
          "org.nuxeo.ecm.directory.sql",
          "org.nuxeo.ecm.directory.types.contrib",
          "org.nuxeo.ecm.platform.usermanager",
          "org.nuxeo.ecm.platform.login.saml2"})
@LocalDeploy("org.nuxeo.ecm.platform.auth.saml:OSGI-INF/test-sql-directory.xml")
public class SAMLAuthenticatorTest {

    @Inject
    protected UserManager userManager;

    private DocumentModel user;

    private SAMLAuthenticationProvider samlAuth;

    @Before
    public void doBefore() throws URISyntaxException {
        samlAuth = new SAMLAuthenticationProvider();

        String metadata = getClass().getResource("/idp-meta.xml").toURI().toString();

        Map<String, String> params = new ImmutableMap.Builder<String, String>() //
            .put("metadata", metadata)
            .build();

        samlAuth.initPlugin(params);

        user = userManager.getUserModel("user");

        if (user == null) {
            DocumentModel user = userManager.getBareUserModel();
            user.setPropertyValue(userManager.getUserIdField(), "user");
            user.setPropertyValue(userManager.getUserEmailField(), "user@dummy");
            user = userManager.createUser(user);
        }
    }

    @Test
    public void testLoginPrompt()
            throws Exception {

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        samlAuth.handleLoginPrompt(req, resp, "/");

        verify(resp).sendRedirect(startsWith("http://dummy/SSOPOST"));
    }

    @Test
    public void testRetrieveIdentity() throws Exception {

        HttpServletRequest req = getMockRequest("/saml-response.xml", "POST", "http://localhost:8080/login", "text/html");

        HttpServletResponse resp = mock(HttpServletResponse.class);

        UserIdentificationInfo info = samlAuth.handleRetrieveIdentity(req, resp);
        assertEquals(info.getUserName(), user.getId());

        verify(req).setAttribute(eq(SAMLAuthenticationProvider.SAML_ATTRIBUTES), notNull());
    }

    protected HttpServletRequest getMockRequest(String messageFile, String method, String url, String contentType) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        URL urlP = new URL(url);
        File file = new File(getClass().getResource(messageFile).toURI());
        String message = Base64.encodeFromFile(file.getAbsolutePath());
        when(request.getMethod()).thenReturn(method);
        when(request.getContentLength()).thenReturn(message.length());
        when(request.getContentType()).thenReturn(contentType);
        when(request.getParameter("SAMLart")).thenReturn(null);
        when(request.getParameter("SAMLRequest")).thenReturn(null);
        when(request.getParameter("SAMLResponse")).thenReturn(message);
        when(request.getParameter("RelayState")).thenReturn("");
        when(request.getParameter("Signature")).thenReturn("");
        when(request.getRequestURI()).thenReturn(urlP.getPath());
        when(request.getRequestURL()).thenReturn(new StringBuffer(url));
        when(request.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(null);
        when(request.isSecure()).thenReturn(false);
        //when(request.getAttribute(SAMLConstants.LOCAL_ENTITY_ID)).thenReturn(null);
        return request;
    }
}
