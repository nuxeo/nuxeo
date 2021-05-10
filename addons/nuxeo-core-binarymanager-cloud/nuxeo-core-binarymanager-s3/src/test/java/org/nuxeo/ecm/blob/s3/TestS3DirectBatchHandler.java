/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.sql.S3DirectBatchHandler;

/**
 * @since 11.5
 */
public class TestS3DirectBatchHandler {

    // NXP-30357
    @Test
    public void testContentTypeHelpers() {
        // text content types
        assertEquals("text/plain", S3DirectBatchHandler.getMimeType("text/plain")); // NOSONAR
        assertNull(S3DirectBatchHandler.getCharset("text/plain"));

        assertEquals("text/plain", S3DirectBatchHandler.getMimeType("text/plain; charset=UTF-8"));
        assertEquals("UTF-8", S3DirectBatchHandler.getCharset("text/plain; charset=UTF-8")); // NOSONAR

        assertEquals("text/plain", S3DirectBatchHandler.getMimeType("text/plain ; CHARSET = UTF-8 ; foo=bar"));
        assertEquals("UTF-8", S3DirectBatchHandler.getCharset("text/plain ; CHARSET = UTF-8 ; foo=bar"));

        // PDF content types
        assertEquals("application/pdf", S3DirectBatchHandler.getMimeType("application/pdf")); // NOSONAR
        assertNull(S3DirectBatchHandler.getCharset("application/pdf"));

        assertEquals("application/pdf", S3DirectBatchHandler.getMimeType("application/pdf; charset=UTF-8"));
        assertEquals("UTF-8", S3DirectBatchHandler.getCharset("application/pdf; charset=UTF-8"));
    }

}