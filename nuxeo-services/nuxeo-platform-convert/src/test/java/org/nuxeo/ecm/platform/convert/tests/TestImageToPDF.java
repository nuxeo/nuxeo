/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Miguel Nixo
 */

package org.nuxeo.ecm.platform.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * @since 5.2
 */
public class TestImageToPDF extends BaseConverterTest {

    protected static final String PDF_MIME_TYPE = "application/pdf";

    protected static final String IMAGE_TO_PDF_CONVERTER = "image2pdf";

    @Test
    public void testConverter() throws IOException {
        String converterName = cs.getConverterName("image/jpeg", PDF_MIME_TYPE);
        assertEquals(IMAGE_TO_PDF_CONVERTER, converterName);

        checkConverterAvailability(converterName);
        checkCommandAvailability("converter");

        BlobHolder pdfBH = getBlobFromPath("test-docs/hello.jpg");
        Map<String, Serializable> parameters = new HashMap<>();
        parameters.put("targetFilePath", "hello.pdf");

        BlobHolder result = cs.convert(converterName, pdfBH, parameters);
        assertNotNull(result);

        List<Blob> blobs = result.getBlobs();
        assertNotNull(blobs);
        assertEquals(1, blobs.size());

        Blob mainBlob = result.getBlob();
        assertEquals("hello.pdf", mainBlob.getFilename());
    }

    // NXP-30402
    @Test
    public void testSourceMimeTypes() {
        assertEquals(IMAGE_TO_PDF_CONVERTER, cs.getConverterName("image/jpeg", PDF_MIME_TYPE));
        assertEquals(IMAGE_TO_PDF_CONVERTER, cs.getConverterName("image/png", PDF_MIME_TYPE));
        assertEquals(IMAGE_TO_PDF_CONVERTER, cs.getConverterName("image/gif", PDF_MIME_TYPE));
        assertEquals(IMAGE_TO_PDF_CONVERTER, cs.getConverterName("image/svg+xml", PDF_MIME_TYPE));
        assertEquals(IMAGE_TO_PDF_CONVERTER, cs.getConverterName("image/tiff", PDF_MIME_TYPE));
        assertEquals(IMAGE_TO_PDF_CONVERTER, cs.getConverterName("application/photoshop", PDF_MIME_TYPE));
    }
}
