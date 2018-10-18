/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.convert.tests;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.api.ConverterNotAvailable;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.core.convert.extension.ChainedConverter;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

@RunWith(FeaturesRunner.class)
@Features(ConvertFeature.class)
@Deploy("org.nuxeo.ecm.core.mimetype")
public class TestService {

    @Inject
    protected ConversionService cs;

    @Inject
    protected HotDeployer deployer;

    @Test
    public void testServiceRegistration() {
        assertNotNull(cs);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib1.xml")
    public void testServiceContrib() {
        assertNotNull(cs);

        Converter cv1 = ConversionServiceImpl.getConverter("dummy1");
        assertNotNull(cv1);

        ConverterDescriptor desc1 = ConversionServiceImpl.getConverterDescriptor("dummy1");
        assertNotNull(desc1);

        assertEquals("test/me", desc1.getDestinationMimeType());
        assertEquals(2, desc1.getSourceMimeTypes().size());
        assertTrue(desc1.getSourceMimeTypes().contains("text/plain"));
        assertTrue(desc1.getSourceMimeTypes().contains("text/xml"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib1.xml")
    public void testConverterLookup() throws Exception {

        String converterName = cs.getConverterName("text/plain", "test/me");
        assertEquals("dummy1", converterName);

        converterName = cs.getConverterName("text/plain2", "test/me");
        assertNull(converterName);

        deployer.deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib2.xml");

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
            assertEquals(2, steps.size());
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

    @Test
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib1.xml")
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib2.xml")
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib4.xml")
    public void testAvailability() {

        ConverterCheckResult result = null;

        // ** not existing converter
        // check registration check
        boolean notRegistred = false;

        try {
            result = cs.isConverterAvailable("toto");
        } catch (ConverterNotRegistered e) {
            notRegistred = true;
        }
        assertTrue(notRegistred);

        // check call
        notRegistred = false;
        try {
            cs.convert("toto", new SimpleBlobHolder(Blobs.createBlob("")), null);
        } catch (ConverterNotRegistered e) {
            notRegistred = true;
        }
        assertTrue(notRegistred);

        // not available converter
        notRegistred = false;
        try {
            result = cs.isConverterAvailable("NotAvailableConverter");
        } catch (ConverterNotRegistered e) {
            notRegistred = true;
        }
        assertFalse(notRegistred);
        assertFalse(result.isAvailable());
        assertNotNull(result.getErrorMessage());
        assertNotNull(result.getInstallationMessage());

        notRegistred = false;
        boolean notAvailable = false;
        try {
            cs.convert("NotAvailableConverter", new SimpleBlobHolder(Blobs.createBlob("")), null);
        } catch (ConverterNotRegistered e) {
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
        } catch (ConverterNotRegistered e) {
            notRegistred = true;
        }
        assertFalse(notRegistred);
        assertTrue(result.isAvailable());
        assertNull(result.getErrorMessage());
        assertNull(result.getInstallationMessage());
        assertEquals(3, result.getSupportedInputMimeTypes().size());

        notRegistred = false;
        try {
            cs.convert("dummy2", new SimpleBlobHolder(Blobs.createBlob("")), null);
        } catch (ConverterNotRegistered e) {
            notRegistred = true;
        } catch (ConverterNotAvailable e) {
            notAvailable = true;
        }
        assertFalse(notRegistred);
        assertFalse(notAvailable);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib3.xml")
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib4.xml")
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib5.xml")
    public void testChainConverterAvailability() {

        ConverterCheckResult result = cs.isConverterAvailable("chainAvailable");
        assertNotNull(result);
        assertTrue(result.isAvailable());

        result = cs.isConverterAvailable("chainNotAvailable");
        assertNotNull(result);
        assertFalse(result.isAvailable());
        assertNotNull(result.getErrorMessage());
        assertNotNull(result.getInstallationMessage());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/convert-service-config-test.xml")
    public void testServiceConfig() throws Exception {
        assertNotNull(cs);

        assertEquals(12, ConversionServiceImpl.getGCIntervalInMinutes());
        assertEquals(132, ConversionServiceImpl.getMaxCacheSizeInKB());
        assertFalse(ConversionServiceImpl.isCacheEnabled());

        // override
        deployer.deploy("org.nuxeo.ecm.core.convert:OSGI-INF/convert-service-config-override.xml");

        assertEquals(34, ConversionServiceImpl.getGCIntervalInMinutes());
        assertEquals(10, ConversionServiceImpl.getMaxCacheSizeInKB());
        assertTrue(ConversionServiceImpl.isCacheEnabled());
        assertEquals("/tmp/nosuchdirforcache", ConversionServiceImpl.getCacheBasePath());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib1.xml")
    public void testSupportedSourceMimeType() {
        assertTrue(cs.isSourceMimeTypeSupported("dummy1", "text/plain"));
        assertTrue(cs.isSourceMimeTypeSupported("dummy1", "text/xml"));
        assertFalse(cs.isSourceMimeTypeSupported("dummy1", "application/pdf"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-pdf-contrib.xml")
    public void testUpdateMimeTypeAndFileName() {
        Map<String, Serializable> parameters = new HashMap<>();
        Blob blob = Blobs.createBlob("dummy text", "text/plain");
        BlobHolder result = cs.convert("dummyPdf", new SimpleBlobHolder(blob), parameters);
        Blob resultBlob = result.getBlob();
        assertNotNull(resultBlob);
        assertEquals("application/pdf", resultBlob.getMimeType());
        // cannot compute any filename
        assertNull(resultBlob.getFilename());

        blob.setFilename("dummy.txt");
        result = cs.convert("dummyPdf", new SimpleBlobHolder(blob), parameters);
        resultBlob = result.getBlob();
        assertNotNull(resultBlob);
        assertEquals("application/pdf", resultBlob.getMimeType());
        assertEquals("dummy.pdf", resultBlob.getFilename());

        parameters.put("setMimeType", FALSE);
        result = cs.convert("dummyPdf", new SimpleBlobHolder(blob), parameters);
        resultBlob = result.getBlob();
        assertNotNull(resultBlob);
        assertEquals("application/pdf", resultBlob.getMimeType());
        assertEquals("dummy.pdf", resultBlob.getFilename());

        parameters = new HashMap<>();
        parameters.put("tempFilename", TRUE);
        result = cs.convert("dummyPdf", new SimpleBlobHolder(blob), parameters);
        resultBlob = result.getBlob();
        assertNotNull(resultBlob);
        assertEquals("application/pdf", resultBlob.getMimeType());
        assertEquals("dummy.pdf", resultBlob.getFilename());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-multi-blob-contrib.xml")
    public void testListBlobCaching() {
        Map<String, Serializable> parameters = new HashMap<>();
        Blob blob = Blobs.createBlob("dummy text", "text/plain");
        BlobHolder result = cs.convert("dummyMulti", new SimpleBlobHolder(blob), parameters);
        assertListBlobCachingResult(result);

        result = cs.convert("dummyMulti", new SimpleBlobHolder(blob), parameters);
        assertListBlobCachingResult(result);
    }

    protected void assertListBlobCachingResult(BlobHolder result) {
        assertNotNull(result);

        Set<String> resultFilenames = result.getBlobs().stream().map(Blob::getFilename).collect(toSet());
        assertTrue("file1 was not found in result", resultFilenames.remove("file1"));
        assertTrue("file2 was not found in result", resultFilenames.remove("file2"));
        assertTrue("There's unexpected blobs in result", resultFilenames.isEmpty());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-pdf-contrib.xml")
    public void testEnforceSourceMimeTypeCheck() {
        Map<String, Serializable> parameters = new HashMap<>();
        Blob blob = Blobs.createBlob("dummy", "application/octet-stream");
        try {
            cs.convert("dummyPdf", new SimpleBlobHolder(blob), parameters);
            fail();
        } catch (ConversionException e) {
            assertEquals("application/octet-stream mime type not supported by dummyPdf converter", e.getMessage());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-pdf-contrib.xml")
    @Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/test-properties-contrib.xml")
    public void testNoEnforceSourceMimeTypeCheck() {
        Map<String, Serializable> parameters = new HashMap<>();
        Blob blob = Blobs.createBlob("dummy", "application/octet-stream", null, "dummy");
        BlobHolder result = cs.convert("dummyPdf", new SimpleBlobHolder(blob), parameters);
        Blob resultBlob = result.getBlob();
        assertNotNull(resultBlob);
        assertEquals("application/pdf", resultBlob.getMimeType());
        assertEquals("dummy.pdf", resultBlob.getFilename());
    }

}
