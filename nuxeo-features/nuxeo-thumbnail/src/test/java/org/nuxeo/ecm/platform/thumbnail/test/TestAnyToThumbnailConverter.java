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
package org.nuxeo.ecm.platform.thumbnail.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.ecm.platform.thumbnail.converter.AnyToThumbnailConverter;

/**
 * @since 11.5
 */
public class TestAnyToThumbnailConverter {

    // NXP-30122
    @Test
    public void testIsPDFMimeType() {
        // none PDF mime type
        assertFalse(AnyToThumbnailConverter.isPDFMimeType("application/json"));
        // PDF mime types
        assertTrue(AnyToThumbnailConverter.isPDFMimeType("application/pdf"));
        assertTrue(AnyToThumbnailConverter.isPDFMimeType("application/pdf; charset=UTF-8"));
    }

}