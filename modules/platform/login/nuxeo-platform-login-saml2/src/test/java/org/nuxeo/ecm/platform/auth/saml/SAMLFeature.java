/*
 * (C) Copyright 2023-2025 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.platform.auth.saml.SAMLFeature.SAML_SECONDARY_ENTITY_ID_PARAMETER;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jakarta.inject.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.nuxeo.ecm.platform.test.UserManagerFeature;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.web.common.WebCommonFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.common.SAMLObject;
import org.w3c.dom.Node;

import com.google.inject.Binder;
import com.google.inject.name.Names;

import net.shibboleth.shared.codec.Base64Support;
import net.shibboleth.shared.codec.DecodingException;
import net.shibboleth.shared.codec.EncodingException;

/**
 * @since 2023.0
 */
@Deploy("org.nuxeo.ecm.platform.login.saml2")
@Features({ UserManagerFeature.class, WebCommonFeature.class })
// Primary SP configuration
@WithFrameworkProperty(name = ENTITY_ID, value = "http://localhost:8080/login")
// Secondary SP configuration
@WithFrameworkProperty(name = SAML_SECONDARY_ENTITY_ID_PARAMETER, value = "http://localhost:8080/secondary")
public class SAMLFeature implements RunnerFeature {

    public static final String ALGORITHM_SIGNATURE_RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    /** @since 2025.7 */
    public static final String SAML_SECONDARY_ENTITY_ID_PARAMETER = "nuxeo.test.saml.secondary.entity.id";

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        // compute metadata file path
        String metadata;
        if (runner.getFeature(IdpKeyStoreFeature.class) != null) {
            metadata = getClass().getResource("/idp-meta-with-certificate.xml").toURI().getPath();
        } else {
            metadata = getClass().getResource("/idp-meta.xml").toURI().getPath();
        }
        Framework.getProperties().put("nuxeo.test.saml.authenticator.metadata", metadata);
        String secondaryMetadata = getClass().getResource("/secondary-idp-meta.xml").toURI().getPath();
        Framework.getProperties().put("nuxeo.test.saml.secondary.authenticator.metadata", secondaryMetadata);
        // deploy saml authenticator contrib
        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        harness.deployContrib("org.nuxeo.ecm.platform.login.saml2.test",
                "OSGI-INF/saml-authenticator-test-contrib.xml");
        // deploy saml secondary authenticator contrib
        harness.deployContrib("org.nuxeo.ecm.platform.login.saml2.test",
                "OSGI-INF/secondary-saml-authenticator-test-contrib.xml");
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        var authenticationService = Framework.getService(PluggableAuthenticationService.class);
        bindNamedProvider(binder, "SAML_AUTH", authenticationService::getPlugin, true);
        bindNamedProvider(binder, "SAML_SECONDARY_AUTH", authenticationService::getPlugin, false);
    }

    protected void bindNamedProvider(Binder binder, String name, Function<String, NuxeoAuthenticationPlugin> provider,
            boolean isDefault) {
        Provider<SAMLAuthenticationProvider> finalProvider = () -> (SAMLAuthenticationProvider) provider.apply(name);
        if (isDefault) {
            binder.bind(SAMLAuthenticationProvider.class).toProvider(finalProvider);
        }
        binder.bind(SAMLAuthenticationProvider.class).annotatedWith(Names.named(name)).toProvider(finalProvider);
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
        try (var is = IOUtils.toInputStream(xml, UTF_8); var out = new ByteArrayOutputStream()) {
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            formatXML(document, out);
            return out.toString();
        } catch (Exception e) {
            throw new AssertionError("Error occurs when pretty-printing xml:\n" + xml, e);
        }
    }

    public static void formatXML(SAMLObject object, OutputStream out) {
        try {
            Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(object);
            formatXML(marshaller.marshall(object), out);
        } catch (MarshallingException e) {
            throw new AssertionError("Error occurs when marshalling object: " + object, e);
        }
    }

    public static void formatXML(Node node, OutputStream out) {
        try {
            var transformerFactory = TransformerFactory.newInstance();
            var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            transformer.transform(new DOMSource(node), new StreamResult(out));
        } catch (TransformerException e) {
            throw new AssertionError("Error occurs when formatting node: " + node, e);
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

    public static <T> Function<T, Object> format(Function<T, Instant> f) {
        Function<Instant, Object> format = i -> {
            var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
            return formatter.format(i);
        };
        return format.compose(f);
    }
}
