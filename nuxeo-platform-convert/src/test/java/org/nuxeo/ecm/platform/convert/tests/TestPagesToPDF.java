/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestPagesToPDF extends NXRuntimeTestCase {

    protected ConversionService cs;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployBundle("org.nuxeo.ecm.platform.convert");

        cs = Framework.getLocalService(ConversionService.class);
        assertNotNull(cs);
    }

    protected static BlobHolder getBlobFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return new SimpleBlobHolder(new FileBlob(file));
    }

    @Test
    public void testPDFConverter() throws Exception {
        String converterName = cs.getConverterName(
                "application/vnd.apple.pages", "application/pdf");
        assertEquals("pages2pdf", converterName);

        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        BlobHolder pagesBH = getBlobFromPath("test-docs/hello.pages");
        pagesBH.getBlob().setMimeType("application/vnd.apple.pages");
        BlobHolder result = cs.convert(converterName, pagesBH, null);
        assertNotNull(result);

        List<Blob> blobs = result.getBlobs();
        assertNotNull(blobs);
        assertEquals(1, blobs.size());

        File pdfFile = File.createTempFile("testingPDFConverter", ".pdf");
        try {
            result.getBlob().transferTo(pdfFile);
            String text = BaseConverterTest.readPdfText(pdfFile);
            assertTrue(text.contains("hello"));
        } finally {
            pdfFile.delete();
        }
    }

    @Test
    public void testHTMLConverter() throws Exception {
        String converterName = cs.getConverterName(
                "application/vnd.apple.pages", "text/html");
        assertEquals("pages2html", converterName);

        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        BlobHolder pagesBH = getBlobFromPath("test-docs/hello.pages");
        pagesBH.getBlob().setMimeType("application/vnd.apple.pages");
        BlobHolder result = cs.convert(converterName, pagesBH, null);
        assertNotNull(result);

        List<Blob> blobs = result.getBlobs();
        assertNotNull(blobs);
        assertEquals(2, blobs.size());

        Blob mainBlob = result.getBlob();
        assertEquals("index.html", mainBlob.getFilename());

        Blob subBlob = blobs.get(1);
        assertTrue(subBlob.getFilename().startsWith("index001"));

        String htmlContent = mainBlob.getString();
        assertTrue(htmlContent.contains("hello"));
    }

    @Test
    public void testPagesWithoutPreviewConverter() throws ClientException {
        String converterName = cs.getConverterName(
                "application/vnd.apple.pages", "application/pdf");
        assertEquals("pages2pdf", converterName);

        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        BlobHolder pagesBH = getBlobFromPath("test-docs/hello-without-preview.pages");
        pagesBH.getBlob().setMimeType("application/vnd.apple.pages");
        try {
            BlobHolder result = cs.convert(converterName, pagesBH, null);
            fail("pdf preview isn't available");
        } catch (ConversionException e) {
            // ok
        }
    }
}
