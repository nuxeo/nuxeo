/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ricardo Dias
 */

package org.nuxeo.ecm.platform.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * @since 8.4
 */
public class TestSOfficeConverter extends BaseConverterTest {

    @Test
    public void testConverter() throws Exception {
        String converterName = cs.getConverterName("text/plain", "application/pdf");
        assertEquals("any2pdf", converterName);

        checkConverterAvailability(converterName);
        checkCommandAvailability("soffice");

        BlobHolder pdfBH = getBlobFromPath("test-docs/hello.txt");
        Map<String, Serializable> parameters = new HashMap<>();

        BlobHolder result = cs.convert(converterName, pdfBH, parameters);
        assertNotNull(result);

        List<Blob> blobs = result.getBlobs();
        assertNotNull(blobs);
        assertEquals(1, blobs.size());

        Blob mainBlob = result.getBlob();
        String text = DocumentUTUtils.readPdfText(mainBlob.getFile());
        assertTrue(text.contains("Hello") || text.contains("hello"));
    }
}
