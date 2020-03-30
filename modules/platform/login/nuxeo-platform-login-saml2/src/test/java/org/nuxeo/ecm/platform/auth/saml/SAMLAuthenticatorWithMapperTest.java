/*
 * (C) Copyright 2014-2019 Nuxeo (http://nuxeo.com/) and others.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.usermapper.test.UserMapperFeature;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.common.SAMLObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

@RunWith(FeaturesRunner.class)
@Features(UserMapperFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.login.saml2")
@Deploy("org.nuxeo.ecm.platform.login.saml2:OSGI-INF/usermapper-contribs.xml")
public class SAMLAuthenticatorWithMapperTest {

    @Inject
    protected UserManager userManager;

    private SAMLAuthenticationProvider samlAuth;

    protected void initAuthProvider(boolean disableUserResolver) throws URISyntaxException {
        String metadata = getClass().getResource("/idp-meta.xml").toURI().getPath();
        Map<String, String> params = new HashMap<>();
        params.put("metadata", metadata);
        if (disableUserResolver) {
            params.put("userResolverCreateIfNeeded", "false");
            params.put("userResolverUpdate", "false");
        }
        params = Collections.unmodifiableMap(params);

        samlAuth = new SAMLAuthenticationProvider();
        samlAuth.initPlugin(params);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.login.saml2:OSGI-INF/usermapper-contribs.xml")
    public void testRetrieveIdentityDefaultSettings() throws Exception {
        initAuthProvider(false);

        HttpServletRequest req = getMockRequest("/saml-response.xml", "POST", "http://localhost:8080/login",
                "text/html");
        HttpServletResponse resp = mock(HttpServletResponse.class);

        UserIdentificationInfo info = samlAuth.handleRetrieveIdentity(req, resp);
        assertEquals("user@dummy", info.getUserName());

        NuxeoPrincipal principal = userManager.getPrincipal("user@dummy");
        assertEquals("user@dummy", principal.getEmail());
    }

    @Test(expected = NullPointerException.class)
    @Deploy("org.nuxeo.ecm.platform.login.saml2:OSGI-INF/usermapper-contribs.xml")
    public void testUserDoesNotExistAndNoCreation() throws Exception {
        initAuthProvider(true);

        HttpServletRequest req = getMockRequest("/saml-response.xml", "POST", "http://localhost:8080/login",
                "text/html");
        HttpServletResponse resp = mock(HttpServletResponse.class);

        UserIdentificationInfo info = samlAuth.handleRetrieveIdentity(req, resp);
        assertEquals("user@dummy", info.getUserName());

        NuxeoPrincipal principal = userManager.getPrincipal("user@dummy");
        assertEquals("user@dummy", principal.getEmail());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.login.saml2:OSGI-INF/usermapper-readonly-contribs.xml")
    public void testUserExistsAndNoCreation() throws Exception {
        initAuthProvider(true);

        DocumentModel user = userManager.getUserModel("user");
        if (user == null) {
            user = userManager.getBareUserModel();
            user.setPropertyValue(userManager.getUserIdField(), "user@dummy");
            user.setPropertyValue(userManager.getUserEmailField(), "user@dummy");
            user = userManager.createUser(user);
        }

        HttpServletRequest req = getMockRequest("/saml-response.xml", "POST", "http://localhost:8080/login",
                "text/html");
        HttpServletResponse resp = mock(HttpServletResponse.class);

        UserIdentificationInfo info = samlAuth.handleRetrieveIdentity(req, resp);
        assertEquals("user@dummy", info.getUserName());

        NuxeoPrincipal principal = userManager.getPrincipal("user@dummy");
        assertEquals("user@dummy", principal.getEmail());
    }

    protected HttpServletRequest getMockRequest(String messageFile, String method, String url, String contentType)
            throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        URL urlP = new URL(url);
        File file = new File(getClass().getResource(messageFile).toURI());
        String message = Base64Support.encode(Files.readAllBytes(file.toPath()), Base64Support.UNCHUNKED);
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
        // when(request.getAttribute(SAMLConstants.LOCAL_ENTITY_ID)).thenReturn(null);
        return request;
    }

    protected SAMLObject decodeMessage(String message) {
        try {
            byte[] decodedBytes = Base64Support.decode(message);
            if (decodedBytes == null) {
                throw new MessageDecodingException("Unable to Base64 decode incoming message");
            }

            InputStream is = new ByteArrayInputStream(decodedBytes);
            is = new InflaterInputStream(is, new Inflater(true));

            Document messageDoc = new BasicParserPool().parse(is);
            Element messageElem = messageDoc.getDocumentElement();

            Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport.getUnmarshallerFactory().getUnmarshaller(messageElem);

            return (SAMLObject) unmarshaller.unmarshall(messageElem);
        } catch (MessageDecodingException | XMLParserException | UnmarshallingException e) {
            //
        }
        return null;
    }
}
