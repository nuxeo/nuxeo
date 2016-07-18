/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.convert.tests;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 5.2
 */
public class TestPDFToText extends BaseConverterTest {

    @Test
    public void testConverter() throws Exception {
        String converterName = cs.getConverterName("application/pdf", "text/plain");
        assertEquals("pdf2text", converterName);

        checkConverterAvailability(converterName);
        checkCommandAvailability("pdftotext");

        BlobHolder pdfBH = getBlobFromPath("test-docs/hello.pdf");

        BlobHolder result = cs.convert(converterName, pdfBH, null);
        assertNotNull(result);

        List<Blob> blobs = result.getBlobs();
        assertNotNull(blobs);
        assertEquals(1, blobs.size());

        Blob blob = result.getBlob();
        assertNotNull(blob);
        assertEquals("text/plain", blob.getMimeType());
        assertTrue(blob.getLength() > 0);
        assertEquals("Hello from a PDF Document!\n\n\f", blob.getString());
    }

}