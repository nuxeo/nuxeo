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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.platform.auth.saml.SAMLConfiguration.ENTITY_ID;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.UserManagerFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.opensaml.saml.common.SAMLObject;

import com.google.inject.Binder;

import net.shibboleth.utilities.java.support.codec.Base64Support;
import net.shibboleth.utilities.java.support.codec.DecodingException;
import net.shibboleth.utilities.java.support.codec.EncodingException;

/**
 * @since 2023.0
 */
@Features({ CoreFeature.class, DirectoryFeature.class, UserManagerFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.login.saml2")
@WithFrameworkProperty(name = ENTITY_ID, value = "http://localhost:8080/login")
public class SAMLFeature implements RunnerFeature {

    public static final String ALGORITHM_SIGNATURE_RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    protected SAMLAuthenticationProvider samlAuthenticationProvider;

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(SAMLAuthenticationProvider.class).toProvider(() -> samlAuthenticationProvider);
    }

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        String metadata = getClass().getResource("/idp-meta.xml").toURI().getPath();
        Map<String, String> params = Map.of("metadata", metadata);

        samlAuthenticationProvider = new SAMLAuthenticationProvider();
        samlAuthenticationProvider.initPlugin(params);
    }

    public static <O extends SAMLObject> void assertSAMLMessage(ExpectedSAMLMessage<O> expectedMessage,
            String rawSamlMessage) {
        // decode the saml message which is compressed and base64 encoded
        String decodedMessage = decodeCompressSAMLMessage(rawSamlMessage);
        // computes arguments contained in the expected message from the actual message
        String expected = expectedMessage.unmarshallThenFormatExpected(decodedMessage);

        assertEquals(expected, formatXML(decodedMessage));
    }

    public static String decodeCompressSAMLMessage(String message) {
        try {
            byte[] decodedBytes = Base64Support.decode(message);
            try (var is = new InflaterInputStream(new ByteArrayInputStream(decodedBytes), new Inflater(true))) {
                return IOUtils.toString(is, UTF_8);
            }
        } catch (IOException e) {
            throw new AssertionError("Unable to decompress the message", e);
        } catch (DecodingException e) {
            throw new AssertionError("Unable to Base64 decode message", e);
        }
    }

    public static String encodeSAMLMessage(String message) {
        try {
            return Base64Support.encodeURLSafe(message.getBytes());
        } catch (EncodingException e) {
            throw new AssertionError("Unable to Base64 encode message", e);
        }
    }

    public static String formatXML(String xml) {
        try (var is = IOUtils.toInputStream(xml, UTF_8)) {
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

            var transformerFactory = TransformerFactory.newInstance();
            var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            var out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            throw new AssertionError("Error occurs when pretty-printing xml:\n" + xml, e);
        }
    }

    public static String extractQueryParam(String url, String paramName) {
        return URLEncodedUtils.parse(URI.create(url), UTF_8)
                              .stream()
                              .filter(param -> paramName.equals(param.getName()))
                              .map(NameValuePair::getValue)
                              .findFirst()
                              .orElseThrow(() -> new AssertionError(
                                      String.format("Unable to find %s in the query parameter", paramName)));
    }
}
