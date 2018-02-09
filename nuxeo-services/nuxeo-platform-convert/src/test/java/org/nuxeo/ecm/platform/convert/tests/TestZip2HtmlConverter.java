/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Ignore("NXP-24309")
public class TestZip2HtmlConverter extends BaseConverterTest {

    @Test
    public void testZipConverter() throws Exception {
        testConvertToMimeType("application/zip");
        testConvertToMimeType("application/x-zip-compressed");
    }

    public void testConvertToMimeType(String mimeType) throws IOException {
        String converterName = cs.getConverterName(mimeType, "text/html");
        assertEquals("zip2html", converterName);

        checkConverterAvailability(converterName);

        BlobHolder htmlBH = getBlobFromPath("test-docs/hello.zip");
        htmlBH.getBlob().setMimeType(mimeType);
        Map<String, Serializable> parameters = Collections.emptyMap();

        BlobHolder result = cs.convert(converterName, htmlBH, parameters);
        assertNotNull(result);

        List<Blob> blobs = result.getBlobs();
        assertNotNull(blobs);
        assertEquals(3, blobs.size());

        String content = blobs.get(0).getString();
        assertTrue(content, content.contains("<li><a href=\"hello.xml\">hello.xml</a></li>"));
        assertTrue(content, content.contains("<li><a href=\"hello.txt\">hello.txt</a></li>"));

        content = DocumentUTUtils.readContent(blobs.get(1).getFile());
        assertTrue(content, content.contains("Hello from a xml <b>document</b>"));

        content = DocumentUTUtils.readContent(blobs.get(2).getFile());
        assertTrue(content, content.contains("Hello from a text document"));
    }
}
