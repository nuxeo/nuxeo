/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.mail.internet.ParseException;

import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 * Tests the {@link NuxeoDriveOperationHelper}.
 *
 * @author Antoine Taillefer
 */
public class TestOperationHelper {

    @Test
    public void testNormalizeMimeTypeAndEncoding() throws ParseException {

        Blob blob = new StringBlob("Joe's blob content.", null, null);
        NuxeoDriveOperationHelper.normalizeMimeTypeAndEncoding(blob);
        assertNull(blob.getMimeType());
        assertNull(blob.getEncoding());

        blob.setMimeType("application/text"); // NOSONAR
        NuxeoDriveOperationHelper.normalizeMimeTypeAndEncoding(blob);
        assertEquals("application/text", blob.getMimeType());
        assertNull(blob.getEncoding());

        blob.setMimeType("application/text; charset=utf-8");
        NuxeoDriveOperationHelper.normalizeMimeTypeAndEncoding(blob);
        assertEquals("application/text", blob.getMimeType());
        assertEquals("utf-8", blob.getEncoding());

        blob.setMimeType("application/text; charset=iso8859_1");
        NuxeoDriveOperationHelper.normalizeMimeTypeAndEncoding(blob);
        assertEquals("application/text", blob.getMimeType());
        assertEquals("utf-8", blob.getEncoding());
    }

}
