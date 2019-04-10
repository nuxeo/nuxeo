/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
    public void testNormalizeMimeTypeAndEncoding() throws Exception {

        Blob blob = new StringBlob("Joe's blob content.", null, null);
        NuxeoDriveOperationHelper.normalizeMimeTypeAndEncoding(blob);
        assertNull(blob.getMimeType());
        assertNull(blob.getEncoding());

        blob.setMimeType("application/text");
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
