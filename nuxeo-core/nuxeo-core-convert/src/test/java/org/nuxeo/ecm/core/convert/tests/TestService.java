/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */
package org.nuxeo.ecm.core.convert.tests;

import java.util.List;

import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.api.ConverterNotAvailable;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistred;
import org.nuxeo.ecm.core.convert.extension.ChainedConverter;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
    }

    public void testServiceRegistration() {
        ConversionService cs = Framework.getLocalService(ConversionService.class);
        assertNotNull(cs);
    }

    public void testServiceContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/converters-test-contrib1.xml");
        ConversionService cs = Framework.getLocalService(ConversionService.class);

        Converter cv1 = ConversionServiceImpl.getConverter("dummy1");
        assertNotNull(cv1);

        ConverterDescriptor desc1 = ConversionServiceImpl.getConverterDesciptor("dummy1");
        assertNotNull(desc1);

        assertEquals("test/me", desc1.getDestinationMimeType());
        assertSame(2, desc1.getSourceMimeTypes().size());
        assertTrue(desc1.getSourceMimeTypes().contains("text/plain"));
        assertTrue(desc1.getSourceMimeTypes().contains("text/xml"));
    }

    public void testConverterLookup() throws Exception {
        deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/converters-test-contrib1.xml");
        ConversionService cs = Framework.getLocalService(ConversionService.class);

        String converterName = cs.getConverterName("text/plain", "test/me");
        assertEquals("dummy1", converterName);

        converterName = cs.getConverterName("text/plain2", "test/me");
        assertNull(converterName);

        deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/converters-test-contrib2.xml");

        if (true) {
            return;
        }

        converterName = cs.getConverterName("test/me", "foo/bar");
        assertEquals("dummy2", converterName);

        converterName = cs.getConverterName("text/plain", "foo/bar");
        assertEquals("dummyChain", converterName);

        Converter cv = ConversionServiceImpl.getConverter("dummyChain");
        assertNotNull(cv);
        boolean isChain = false;
        if (cv instanceof ChainedConverter) {
            ChainedConverter ccv = (ChainedConverter) cv;
            List<String> steps = ccv.getSteps();
            assertNotNull(steps);
            assertSame(2, steps.size());
            assertTrue(steps.contains("test/me"));
            assertTrue(steps.contains("foo/bar"));
            isChain = true;

        }
        assertTrue(isChain);

        converterName = cs.getConverterName("something", "somethingelse");
        assertEquals("custom", converterName);

        converterName = cs.getConverterName("any", "somethingelse");
        assertEquals("wildcard", converterName);

        converterName = cs.getConverterName("text/plain", "jacky/chan");
        assertEquals("dummyChain2", converterName);
        Converter cv2 = ConversionServiceImpl.getConverter("dummyChain2");
        assertNotNull(cv2);
        isChain = false;
        if (cv2 instanceof ChainedConverter) {
            ChainedConverter ccv = (ChainedConverter) cv2;
            List<String> steps = ccv.getSteps();
            assertNull(steps);
            isChain = true;

        }
        assertTrue(isChain);
    }

    public void testAvailability() throws Exception {
        deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/converters-test-contrib2.xml");
        deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/converters-test-contrib4.xml");
        ConversionService cs = Framework.getLocalService(ConversionService.class);

        ConverterCheckResult result = null;

        // ** not existing converter
        // check registration check
        boolean notRegistred = false;

        try {
            result = cs.isConverterAvailable("toto");
        } catch (ConverterNotRegistred e) {
            notRegistred = true;
        }
        assertTrue(notRegistred);

        // check call
        notRegistred = false;
        try {
            cs.convert("toto", new SimpleBlobHolder(new StringBlob("")), null);
        } catch (ConverterNotRegistred e) {
            notRegistred = true;
        }
        assertTrue(notRegistred);

        // not available converter

        notRegistred = false;
        try {
            result = cs.isConverterAvailable("NotAvailableConverter");
        } catch (ConverterNotRegistred e) {
            notRegistred = true;
        }
        assertFalse(notRegistred);
        assertFalse(result.isAvailable());
        assertNotNull(result.getErrorMessage());
        assertNotNull(result.getInstallationMessage());

        notRegistred = false;
        boolean notAvailable = false;
        try {
            cs.convert("NotAvailableConverter", new SimpleBlobHolder(
                    new StringBlob("")), null);
        } catch (ConverterNotRegistred e) {
            notRegistred = true;
        } catch (ConverterNotAvailable e) {
            notAvailable = true;
        }
        assertFalse(notRegistred);
        assertTrue(notAvailable);

        // ** available converter
        notRegistred = false;
        notAvailable = false;
        try {
            result = cs.isConverterAvailable("dummy2");
        } catch (ConverterNotRegistred e) {
            notRegistred = true;
        }
        assertFalse(notRegistred);
        assertTrue(result.isAvailable());
        assertNull(result.getErrorMessage());
        assertNull(result.getInstallationMessage());
        assertSame(2, result.getSupportedInputMimeTypes().size());

        notRegistred = false;
        try {
            cs.convert("dummy2", new SimpleBlobHolder(new StringBlob("")), null);
        } catch (ConverterNotRegistred e) {
            notRegistred = true;
        } catch (ConverterNotAvailable e) {
            notAvailable = true;
        }
        assertFalse(notRegistred);
        assertFalse(notAvailable);
    }

    public void testServiceConfig() throws Exception {
        deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/convert-service-config-test.xml");
        ConversionService cs = Framework.getLocalService(ConversionService.class);

        assertEquals(12, ConversionServiceImpl.getGCIntervalInMinutes());
        assertEquals(132, ConversionServiceImpl.getMaxCacheSizeInKB());
        assertFalse(ConversionServiceImpl.isCacheEnabled());
    }

}
