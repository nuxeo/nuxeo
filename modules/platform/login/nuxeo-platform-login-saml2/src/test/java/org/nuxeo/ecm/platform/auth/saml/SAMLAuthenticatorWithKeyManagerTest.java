/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.auth.saml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.auth.saml.SAMLAuthenticationProvider.SAML_SESSION_KEY;
import static org.nuxeo.ecm.platform.auth.saml.SAMLFeature.ALGORITHM_SIGNATURE_RSA_SHA1;
import static org.nuxeo.ecm.platform.auth.saml.SAMLFeature.assertSAMLMessage;
import static org.nuxeo.ecm.platform.auth.saml.SAMLFeature.encodeSAMLMessage;
import static org.nuxeo.ecm.platform.auth.saml.SAMLFeature.extractQueryParam;
import static org.nuxeo.ecm.platform.auth.saml.binding.HTTPRedirectBinding.SAML_REQUEST;
import static org.nuxeo.ecm.platform.auth.saml.binding.HTTPRedirectBinding.SAML_RESPONSE;

import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.auth.saml.key.KeyManagerFeature;
import org.nuxeo.ecm.platform.auth.saml.mock.MockHttpServletRequest;
import org.nuxeo.ecm.platform.auth.saml.mock.MockHttpServletResponse;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;

/**
 * @since 2023.0
 */
@RunWith(FeaturesRunner.class)
@Features({ SAMLFeature.class, KeyManagerFeature.class })
public class SAMLAuthenticatorWithKeyManagerTest {

    @Inject
    protected SAMLAuthenticationProvider samlAuth;

    @Inject
    protected UserManager userManager;

    protected DocumentModel user;

    @Before
    public void doBefore() {
        user = userManager.getUserModel("user");

        if (user == null) {
            user = userManager.getBareUserModel();
            user.setPropertyValue(userManager.getUserIdField(), "user");
            user.setPropertyValue(userManager.getUserEmailField(), "user@dummy");
            user = userManager.createUser(user);
        }
    }

    @Test
    public void testLoginPrompt() {
        var requestHandler = MockHttpServletRequest.init();
        var responseHandler = MockHttpServletResponse.init();

        samlAuth.handleLoginPrompt(requestHandler.mock(), responseHandler.mock(), "/");

        var redirectURL = responseHandler.getRedirect();
        assertTrue(redirectURL.startsWith("http://dummy/SSORedirect"));

        var expected = new ExpectedSAMLMessage<>(
                """
                        <saml2p:AuthnRequest xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol" AssertionConsumerServiceURL="null://null/nuxeo/home.html" Destination="http://dummy/SSORedirect" ID="%s" IssueInstant="%s" Version="2.0">
                          <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">http://localhost:8080/login</saml2:Issuer>
                          <saml2p:NameIDPolicy Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"/>
                        </saml2p:AuthnRequest>
                        """,
                AuthnRequest::getID, AuthnRequest::getIssueInstant);
        var actual = extractQueryParam(redirectURL, SAML_REQUEST);
        assertSAMLMessage(expected, actual);
    }

    @Test
    public void testAuthRequest() {
        var requestHandler = MockHttpServletRequest.init();

        String loginURL = samlAuth.computeUrl(requestHandler.mock(), null);

        assertTrue(loginURL.startsWith("http://dummy/SSORedirect"));
        var expected = new ExpectedSAMLMessage<>(
                """
                        <saml2p:AuthnRequest xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol" AssertionConsumerServiceURL="null://null/nuxeo/home.html" Destination="http://dummy/SSORedirect" ID="%s" IssueInstant="%s" Version="2.0">
                          <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">http://localhost:8080/login</saml2:Issuer>
                          <saml2p:NameIDPolicy Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"/>
                        </saml2p:AuthnRequest>
                        """,
                AuthnRequest::getID, AuthnRequest::getIssueInstant);
        var actual = extractQueryParam(loginURL, SAML_REQUEST);
        assertSAMLMessage(expected, actual);
    }

    @Test
    public void testRetrieveIdentity() {
        Instant now = Instant.now();
        var samlResponse = """
                <samlp:Response xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                                ID="%s" Destination="http://localhost:8080/login" IssueInstant="%s"
                                InResponseTo="_a5947jig4cb55ii746a2963a67bf65" Version="2.0">
                  <saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">http://dummy</saml:Issuer>
                  <samlp:Status xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol">
                    <samlp:StatusCode xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                                      Value="urn:oasis:names:tc:SAML:2.0:status:Success">
                    </samlp:StatusCode>
                  </samlp:Status>
                  <saml:Assertion xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                                  ID="%s" IssueInstant="%s"
                                  Version="2.0">
                    <saml:Issuer>http://dummy</saml:Issuer>
                    <saml:Subject>
                      <saml:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"
                                   NameQualifier="http://dummy">user@dummy</saml:NameID>
                      <saml:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                        <saml:SubjectConfirmationData InResponseTo="_a5947jig4cb55ii746a2963a67bf65"
                                                      Recipient="http://localhost:8080/login"/>
                      </saml:SubjectConfirmation>
                    </saml:Subject>
                    <saml:Conditions NotBefore="%s" NotOnOrAfter="%s">
                      <saml:AudienceRestriction>
                        <saml:Audience>http://localhost:8080/login</saml:Audience>
                      </saml:AudienceRestriction>
                    </saml:Conditions>
                    <saml:AuthnStatement AuthnInstant=""
                                         SessionIndex="s2008f616d6f2b777082bbf1a8a135d1a9f3d53501">
                      <saml:AuthnContext>
                        <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
                        </saml:AuthnContextClassRef>
                      </saml:AuthnContext>
                    </saml:AuthnStatement>
                  </saml:Assertion>
                </samlp:Response>
                """.formatted("_" + UUID.randomUUID(), now, "_" + UUID.randomUUID(), now, now, now);
        var encodedSamlResponse = encodeSAMLMessage(samlResponse);

        var requestHandler = MockHttpServletRequest.init("POST", "http://localhost:8080/login")
                                                   .withAttributes()
                                                   .whenGetParameterThenReturn(SAML_RESPONSE, encodedSamlResponse)
                                                   .whenGetParameterThenReturn("RelayState", "/relay");
        var responseHandler = MockHttpServletResponse.init();

        UserIdentificationInfo info = samlAuth.handleRetrieveIdentity(requestHandler.mock(), responseHandler.mock());

        assertEquals(info.getUserName(), user.getId());

        var redirectUri = requestHandler.getSessionAttributeValue(NXAuthConstants.START_PAGE_SAVE_KEY);
        assertEquals("/relay", redirectUri);

        Cookie cookie = responseHandler.getCookie(SAML_SESSION_KEY);
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    public void testRetrieveIdentityOnSingleLogoutRequest() {
        var samlRequest = """
                <saml2p:LogoutRequest xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol" Destination="http://localhost:8080/login" ID="%s" IssueInstant="%s" Version="2.0">
                  <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">http://dummy</saml2:Issuer>
                  <saml2:NameID xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" Format="format">user@dummy</saml2:NameID>
                  <saml2p:SessionIndex>sessionId</saml2p:SessionIndex>
                </saml2p:LogoutRequest>
                """.formatted(
                "_" + UUID.randomUUID(), Instant.now());
        var encodedSamlRequest = encodeSAMLMessage(samlRequest);

        var requestHandler = MockHttpServletRequest.init("POST", "http://localhost:8080/login")
                                                   .whenGetParameterThenReturn(SAML_REQUEST, encodedSamlRequest)
                                                   .withGetCookieThenReturn(SAML_SESSION_KEY,
                                                           "sessionId|user@dummy|format");
        var responseHandler = MockHttpServletResponse.init();

        var info = samlAuth.handleRetrieveIdentity(requestHandler.mock(), responseHandler.mock());
        assertNull(info);
    }

    @Test
    public void testLogoutRequest() {
        var requestHandler = MockHttpServletRequest.init()
                                                   .withGetCookieThenReturn(SAML_SESSION_KEY,
                                                           "sessionId|user@dummy|format");
        var responseHandler = MockHttpServletResponse.init();

        String logoutURL = samlAuth.getSLOUrl(requestHandler.mock(), responseHandler.mock());

        assertTrue(logoutURL.startsWith("http://dummy/SLORedirect"));

        var expected = new ExpectedSAMLMessage<>(
                """
                        <saml2p:LogoutRequest xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol" Destination="http://dummy/SLORedirect" ID="%s" IssueInstant="%s" Version="2.0">
                          <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">http://localhost:8080/login</saml2:Issuer>
                          <saml2:NameID xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" Format="format">user@dummy</saml2:NameID>
                          <saml2p:SessionIndex>sessionId</saml2p:SessionIndex>
                        </saml2p:LogoutRequest>
                        """,
                LogoutRequest::getID, LogoutRequest::getIssueInstant);
        var actual = extractQueryParam(logoutURL, SAML_REQUEST);
        assertSAMLMessage(expected, actual);

        var signatureAlgorithm = extractQueryParam(logoutURL, "SigAlg");
        assertEquals(ALGORITHM_SIGNATURE_RSA_SHA1, signatureAlgorithm);

        var signature = extractQueryParam(logoutURL, "Signature");
        assertNotNull(signature);
    }
}
