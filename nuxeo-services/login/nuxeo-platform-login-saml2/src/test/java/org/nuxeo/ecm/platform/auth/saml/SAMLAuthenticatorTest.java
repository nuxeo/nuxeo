/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.auth.saml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.auth.saml.binding.HTTPRedirectBinding;
import org.nuxeo.ecm.platform.auth.saml.binding.SAMLBinding;
import org.nuxeo.ecm.platform.auth.saml.mock.MockHttpSession;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.binding.BasicSAMLMessageContext;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.NameIDType;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableMap;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.web.common")
@Deploy("org.nuxeo.ecm.platform.login.saml2")
@Deploy("org.nuxeo.ecm.platform.login.saml2:OSGI-INF/test-sql-directory.xml")
public class SAMLAuthenticatorTest {

    @Inject
    protected UserManager userManager;

    private DocumentModel user;

    private SAMLAuthenticationProvider samlAuth;

    @Before
    public void doBefore() throws URISyntaxException {
        samlAuth = new SAMLAuthenticationProvider();

        String metadata = getClass().getResource("/idp-meta.xml").toURI().getPath();

        Map<String, String> params = new ImmutableMap.Builder<String, String>() //
                                                                                .put("metadata", metadata)
                                                                                .build();

        samlAuth.initPlugin(params);

        user = userManager.getUserModel("user");

        if (user == null) {
            user = userManager.getBareUserModel();
            user.setPropertyValue(userManager.getUserIdField(), "user");
            user.setPropertyValue(userManager.getUserEmailField(), "user@dummy");
            user = userManager.createUser(user);
        }
    }

    @Test
    public void testLoginPrompt() throws Exception {

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        samlAuth.handleLoginPrompt(req, resp, "/");

        verify(resp).sendRedirect(startsWith("http://dummy/SSORedirect"));
    }

    @Test
    public void testAuthRequest() throws Exception {

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        String loginURL = samlAuth.getSSOUrl(req, resp);
        String query = URI.create(loginURL).getQuery();

        assertTrue(loginURL.startsWith("http://dummy/SSORedirect"));
        assertTrue(query.startsWith(HTTPRedirectBinding.SAML_REQUEST));

        String samlRequest = query.replaceFirst(HTTPRedirectBinding.SAML_REQUEST + "=", "");

        SAMLObject message = decodeMessage(samlRequest);

        // Validate type
        assertTrue(message instanceof AuthnRequest);

        AuthnRequest auth = (AuthnRequest) message;
        assertEquals(SAMLVersion.VERSION_20, auth.getVersion());
        assertNotNull(auth.getID());
    }

    @Test
    public void testRetrieveIdentity() throws Exception {

        HttpServletRequest req = getMockRequest("/saml-response.xml", "POST", "http://localhost:8080/login",
                "text/html", "/relay");

        HttpServletResponse resp = mock(HttpServletResponse.class);

        UserIdentificationInfo info = samlAuth.handleRetrieveIdentity(req, resp);
        assertEquals(info.getUserName(), user.getId());

        final ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);

        verify(resp).addCookie(captor.capture());

        final List<Cookie> cookies = captor.getAllValues();

        assertTrue(!cookies.isEmpty());

        String redirectUri = (String) req.getSession(true).getAttribute(NXAuthConstants.START_PAGE_SAVE_KEY);
        assertEquals("/relay", redirectUri);
        
        Cookie cookie = cookies.stream()
                               .filter(c -> c.getName().equals(SAMLAuthenticationProvider.SAML_SESSION_KEY))
                               .findFirst()
                               .orElseThrow(() -> new AssertionError("SAML session cookie not found"));
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    public void testLogoutRequest() throws Exception {

        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        Cookie[] cookies = new Cookie[] { new Cookie(SAMLAuthenticationProvider.SAML_SESSION_KEY,
                "sessionId|user@dummy|format") };
        when(req.getCookies()).thenReturn(cookies);
        String logoutURL = samlAuth.getSLOUrl(req, resp);

        assertTrue(logoutURL.startsWith("http://dummy/SLORedirect"));

        List<NameValuePair> params = URLEncodedUtils.parse(new URI(logoutURL), "UTF-8");
        assertEquals(HTTPRedirectBinding.SAML_REQUEST, params.get(0).getName());
        String samlRequest = params.get(0).getValue();
        SAMLObject message = decodeMessage(samlRequest);

        // Validate type
        assertTrue(message instanceof LogoutRequest);

        LogoutRequest logout = (LogoutRequest) message;
        assertEquals("http://dummy/SLORedirect", logout.getDestination());
    }

    // NXP17044: strips scheme to fix validity check with reverse proxies
    @Test
    public void testUriComparator() {
        assertTrue(SAMLBinding.uriComparator.compare("https://dummy", "http://dummy"));
    }

    protected HttpServletRequest getMockRequest(String messageFile, String method, String url, String contentType,
            String relayState)
            throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = new MockHttpSession();
        when(request.getSession(anyBoolean())).thenReturn(session);
        URL urlP = new URL(url);
        File file = new File(getClass().getResource(messageFile).toURI());
        String message = Base64.encodeFromFile(file.getAbsolutePath());
        when(request.getMethod()).thenReturn(method);
        when(request.getContentLength()).thenReturn(message.length());
        when(request.getContentType()).thenReturn(contentType);
        when(request.getParameter("SAMLart")).thenReturn(null);
        when(request.getParameter("SAMLRequest")).thenReturn(null);
        when(request.getParameter("SAMLResponse")).thenReturn(message);
        when(request.getParameter("RelayState")).thenReturn(relayState);
        when(request.getParameter("Signature")).thenReturn("");
        when(request.getRequestURI()).thenReturn(urlP.getPath());
        when(request.getRequestURL()).thenReturn(new StringBuffer(url));
        when(request.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(null);
        when(request.isSecure()).thenReturn(false);
        return request;
    }

    @Test
    public void testNotOnOrAfterTimeSkew() {
        AbstractSAMLProfile profile = new AbstractSAMLProfile(null) {
            @Override
            public String getProfileIdentifier() {
                return "test";
            }
        };

        Conditions conditions = mock(Conditions.class);

        Issuer issuer = mock(Issuer.class);
        when(issuer.getFormat()).thenReturn(NameIDType.ENTITY);

        Assertion assertion = mock(Assertion.class);
        when(assertion.getIssuer()).thenReturn(issuer);
        when(assertion.getConditions()).thenReturn(conditions);

        // Set message expiration 1ms ago
        when(conditions.getNotOnOrAfter()).thenReturn(DateTime.now().minusMillis(1));

        // Validation passes with default time skew
        SAMLMessageContext context = new BasicSAMLMessageContext();
        try {
            profile.validateAssertion(assertion, context);
        } catch (SAMLException e) {
            fail("Validation should have passed");;
        }

        // Disabling time skew makes validation fail
        profile.setSkewTimeMillis(0);

        try {
            profile.validateAssertion(assertion, context);
            fail("Expected validation to fail");
        } catch (SAMLException e) {
            assertEquals("Conditions have expired", e.getMessage());
        }

    }

    @Test
    public void testNotBeforeTimeSkew() {
        AbstractSAMLProfile profile = new AbstractSAMLProfile(null) {
            @Override
            public String getProfileIdentifier() {
                return "test";
            }
        };

        Conditions conditions = mock(Conditions.class);

        Issuer issuer = mock(Issuer.class);
        when(issuer.getFormat()).thenReturn(NameIDType.ENTITY);

        Assertion assertion = mock(Assertion.class);
        when(assertion.getIssuer()).thenReturn(issuer);
        when(assertion.getConditions()).thenReturn(conditions);

        // Set message active in 1ms
        when(conditions.getNotBefore()).thenReturn(DateTime.now().plusMillis(1));

        // Validation passes with default time skew
        SAMLMessageContext context = new BasicSAMLMessageContext();
        try {
            profile.validateAssertion(assertion, context);
        } catch (SAMLException e) {
            fail("Validation should have passed");;
        }

        // Disabling time skew makes validation fail
        profile.setSkewTimeMillis(0);

        try {
            profile.validateAssertion(assertion, context);
            fail("Expected validation to fail");
        } catch (SAMLException e) {
            assertEquals("Conditions are not yet active", e.getMessage());
        }

    }

    protected SAMLObject decodeMessage(String message) {
        try {
            byte[] decodedBytes = Base64.decode(message);
            if (decodedBytes == null) {
                throw new MessageDecodingException("Unable to Base64 decode incoming message");
            }

            InputStream is = new ByteArrayInputStream(decodedBytes);
            is = new InflaterInputStream(is, new Inflater(true));

            Document messageDoc = new BasicParserPool().parse(is);
            Element messageElem = messageDoc.getDocumentElement();

            Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(messageElem);

            return (SAMLObject) unmarshaller.unmarshall(messageElem);
        } catch (MessageDecodingException | XMLParserException | UnmarshallingException e) {
            //
        }
        return null;
    }
}
