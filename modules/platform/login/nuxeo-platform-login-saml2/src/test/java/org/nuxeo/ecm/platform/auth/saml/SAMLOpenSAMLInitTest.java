/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <antoine.taillefer@hyland.com>
 */
package org.nuxeo.ecm.platform.auth.saml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;

/**
 * Regression test to verify that {@link SAMLAuthenticationProvider#initOpenSAML()} succeeds even when Apache
 * {@code xerces:xercesImpl} is on the classpath, by temporarily forcing the JDK built-in JAXP
 * {@link DocumentBuilderFactory} implementation for the duration of init.
 * <p>
 * Without the workaround, OpenSAML 5.2.2+ {@code GlobalParserPoolInitializer.init()} would propagate the
 * JDK-JAXP-specific {@code jdk.xml.maxElementDepth} attribute to the Apache Xerces factory, which would throw
 * {@link IllegalArgumentException}.
 *
 * @since 2025.22
 */
public class SAMLOpenSAMLInitTest {

    @Test
    public void initOpenSAMLShouldSucceedWithApacheXercesOnClasspath() {
        String savedDbfProp = System.getProperty(SAMLAuthenticationProvider.JAXP_DBF_PROP);
        try {
            // Ensure JAXP default points to Apache Xerces on the test classpath, otherwise this test isn't actually
            // exercising the workaround scenario.
            System.clearProperty(SAMLAuthenticationProvider.JAXP_DBF_PROP);
            String defaultFactoryClass = DocumentBuilderFactory.newInstance().getClass().getName();
            assertEquals("Test classpath should select Apache Xerces as JAXP default",
                    "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl", defaultFactoryClass);

            // Exercise the workaround: this must not throw IllegalArgumentException.
            SAMLAuthenticationProvider.initOpenSAML();

            // The override must be restored to its prior (unset) state.
            assertNull("javax.xml.parsers.DocumentBuilderFactory must be cleared after init",
                    System.getProperty(SAMLAuthenticationProvider.JAXP_DBF_PROP));

            // The parser pool must have been registered.
            XMLObjectProviderRegistry registry = ConfigurationService.get(XMLObjectProviderRegistry.class);
            assertNotNull("OpenSAML XMLObjectProviderRegistry should be configured", registry);
            assertNotNull("ParserPool should be registered after init", registry.getParserPool());
        } finally {
            if (savedDbfProp == null) {
                System.clearProperty(SAMLAuthenticationProvider.JAXP_DBF_PROP);
            } else {
                System.setProperty(SAMLAuthenticationProvider.JAXP_DBF_PROP, savedDbfProp);
            }
        }
    }
}
