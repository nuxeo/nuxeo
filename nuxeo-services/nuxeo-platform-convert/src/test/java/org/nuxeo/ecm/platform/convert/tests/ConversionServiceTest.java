/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.platform.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;

/**
 * @since 11.1
 */
public class ConversionServiceTest extends BaseConverterTest {

    @Test
    public void shouldFailWithoutAdditionalInfosWhenMimeTypeIsNotSupportedByConverter() throws IOException {
        String converterName = cs.getConverterName("image/jpeg", "application/pdf");
        BlobHolder blobHolder = getBlobFromPath("test-docs/hello.xls", "application/vnd.ms-excel");

        try {
            cs.convert(converterName, blobHolder, Collections.emptyMap());
            fail("Should have raised a ConversionException");
        } catch (ConversionException e) {
            assertTrue(e.getInfos().isEmpty());
        }
    }

    @Test
    public void shouldFailWithAdditionalInfosWhenMimeTypeIsNotSupportedByConverter() throws IOException {
        String converterName = cs.getConverterName("image/jpeg", "application/pdf");
        DocumentBlobHolder documentBlobHolder = getDocumentBlob("test-docs/hello.xls", "application/vnd.ms-excel");

        try {
            cs.convert(converterName, documentBlobHolder, Collections.emptyMap());
            fail("Should have raised a ConversionException");
        } catch (ConversionException e) {
            assertEquals(1, e.getInfos().size());
            assertTrue(e.getInfos().get(0).contains(documentBlobHolder.getDocument().getId()));
        }
    }

    @Test
    public void shouldFailWithAdditionalInfosWhenReadingDocumentBlobContent() throws IOException {
        SimpleManagedBlob mock = Mockito.mock(SimpleManagedBlob.class);

        when(mock.getEncoding()).thenReturn("UTF-8");
        when(mock.getKey()).thenReturn("anyKey:anyKey");
        when(mock.getProviderId()).thenReturn("anyProvider:anyProvider");
        when(mock.getMimeType()).thenReturn("text/html");
        when(mock.getStream()).thenThrow(IOException.class);

        DocumentBlobHolder documentBlobHolder = createDocumentBlob(mock);

        try {
            cs.convert("html2text", documentBlobHolder, Collections.emptyMap());
            fail("Should have raised a ConversionException");
        } catch (ConversionException e) {
            assertEquals(2, e.getInfos().size());
            assertTrue(e.getInfos().get(0).contains("anyProvider:anyProvider"));
            assertTrue(e.getInfos().get(0).contains("anyKey:anyKey"));
            assertTrue(e.getInfos().get(1).contains(documentBlobHolder.getDocument().getId()));
        }
    }

    @Test
    public void shouldFailWithAdditionalInfosWhenReadingDBlobContent() throws IOException {
        SimpleManagedBlob mock = Mockito.mock(SimpleManagedBlob.class);

        when(mock.getEncoding()).thenReturn("UTF-8");
        when(mock.getKey()).thenReturn("anyKey:anyKey");
        when(mock.getProviderId()).thenReturn("anyProvider:anyProvider");
        when(mock.getMimeType()).thenReturn("text/html");
        when(mock.getDigest()).thenReturn("anyDigest");
        when(mock.getStream()).thenThrow(IOException.class);

        try {
            cs.convert("html2text", new SimpleBlobHolder(mock), Collections.emptyMap());
            fail("Should have raised a ConversionException");
        } catch (ConversionException e) {
            assertEquals(1, e.getInfos().size());
            assertTrue(e.getInfos().get(0).contains("anyProvider:anyProvider"));
            assertTrue(e.getInfos().get(0).contains("anyKey:anyKey"));
        }
    }
}
