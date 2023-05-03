/*
 * (C) Copyright 2014-2023 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.platform.auth.saml.SAMLFeature.encodeSAMLMessage;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.auth.saml.mock.MockHttpServletRequest;
import org.nuxeo.ecm.platform.auth.saml.mock.MockHttpServletResponse;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.usermapper.test.UserMapperFeature;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ SAMLFeature.class, UserMapperFeature.class })
@Deploy("org.nuxeo.ecm.platform.login.saml2.test:OSGI-INF/usermapper-contribs.xml")
public class SAMLAuthenticatorWithMapperTest {

    @Inject
    protected UserManager userManager;

    protected MockHttpServletRequest requestHandler;

    protected MockHttpServletResponse responseHandler;

    @Before
    public void before() {
        String url = "http://localhost:8080/login";
        Instant now = Instant.now();
        var samlResponse = """
                <samlp:Response xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                                ID="%s" Destination="%s" IssueInstant="%s"
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
                                                      Recipient="%s"/>
                      </saml:SubjectConfirmation>
                    </saml:Subject>
                    <saml:Conditions NotBefore="%s" NotOnOrAfter="%s">
                      <saml:AudienceRestriction>
                        <saml:Audience>%s</saml:Audience>
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
                """.formatted("_" + UUID.randomUUID(), url, now, "_" + UUID.randomUUID(), now, url, now, now, url);
        var encodedSamlResponse = encodeSAMLMessage(samlResponse);

        requestHandler = MockHttpServletRequest.init("POST", url)
                                               .withAttributes()
                                               .whenGetParameterThenReturn("SAMLResponse", encodedSamlResponse)
                                               .whenGetParameterThenReturn("RelayState", "/relay");
        responseHandler = MockHttpServletResponse.init();
    }

    @Test
    public void testRetrieveIdentityDefaultSettings() throws Exception {
        var samlAuth = initAuthProvider(true);

        UserIdentificationInfo info = samlAuth.handleRetrieveIdentity(requestHandler.mock(), responseHandler.mock());
        assertEquals("user@dummy", info.getUserName());

        NuxeoPrincipal principal = userManager.getPrincipal("user@dummy");
        assertEquals("user@dummy", principal.getEmail());
    }

    @Test
    public void testUserDoesNotExistAndNoCreation() throws Exception {
        var samlAuth = initAuthProvider(false);

        UserIdentificationInfo info = samlAuth.handleRetrieveIdentity(requestHandler.mock(), responseHandler.mock());
        assertNull(info);

        NuxeoPrincipal principal = userManager.getPrincipal("user@dummy");
        assertNull(principal);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.login.saml2.test:OSGI-INF/usermapper-readonly-contribs.xml")
    public void testUserExistsAndNoCreation() throws Exception {
        var samlAuth = initAuthProvider(false);

        DocumentModel user = userManager.getUserModel("user");
        if (user == null) {
            user = userManager.getBareUserModel();
            user.setPropertyValue(userManager.getUserIdField(), "user@dummy");
            user.setPropertyValue(userManager.getUserEmailField(), "user@dummy");
            user = userManager.createUser(user);
        }

        UserIdentificationInfo info = samlAuth.handleRetrieveIdentity(requestHandler.mock(), responseHandler.mock());
        assertEquals(user.getId(), info.getUserName());

        NuxeoPrincipal principal = userManager.getPrincipal("user@dummy");
        assertEquals(user.getId(), principal.getEmail());
    }

    protected SAMLAuthenticationProvider initAuthProvider(boolean createOrUpdate) throws URISyntaxException {
        String metadata = getClass().getResource("/idp-meta.xml").toURI().getPath();
        String createOrUpdateString = String.valueOf(createOrUpdate);
        Map<String, String> params = Map.of("metadata", metadata, //
                "userResolverCreateIfNeeded", createOrUpdateString, //
                "userResolverUpdate", createOrUpdateString);

        var samlAuth = new SAMLAuthenticationProvider();
        samlAuth.initPlugin(params);
        return samlAuth;
    }
}
