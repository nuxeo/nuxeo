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

package org.nuxeo.ecm.core.convert.plugins.tests;

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 */
public class TestMailConverter extends BaseConverterTest {

    private static final String CONVERTER_NAME = "rfc822totext";

    protected ConversionService cs;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        cs = Framework.getLocalService(ConversionService.class);
        assertNotNull(cs);
    }

    private Blob getTestBlob(String filePath) {
        File file = FileUtils.getResourceFileFromContext(filePath);
        return new FileBlob(file);
    }

    public void testTextEmailTransformation() throws Exception {
        BlobHolder bh = cs.convert(CONVERTER_NAME,
                getBlobFromPath("test-docs/email/text.eml"), null);
        assertNotNull(bh);
        Blob result = bh.getBlob();
        assertNotNull(result);
        assertEquals("text/plain", result.getMimeType());
        Blob expected = getTestBlob("test-docs/email/text.txt");
        assertEquals(expected.getString(), result.getString());
    }

    protected boolean textEquals(String txt1, String txt2) {
        txt1 = txt1.replaceAll("\n\r", " ");
        txt1 = txt1.replaceAll("\n", " ");

        txt2 = txt2.replaceAll("\n\r", " ");
        txt2 = txt2.replaceAll("\n", " ");

        txt1 = txt1.trim();
        txt2 = txt2.trim();

        return txt1.equals(txt2);
    }

    public void testTextAndHtmlEmailTransformation() throws Exception {
        BlobHolder bh = cs.convert(
                CONVERTER_NAME,
                getBlobFromPath("test-docs/email/text_and_html_with_attachments.eml"),
                null);
        assertNotNull(bh);
        Blob result = bh.getBlob();
        assertNotNull(result);
        assertEquals("text/plain", result.getMimeType());
        Blob expected = getTestBlob("test-docs/email/text_and_html_with_attachments.txt");

        assertTrue(textEquals(expected.getString(), result.getString()));
    }

    public void testOnlyHtmlEmailTransformation() throws Exception {
        BlobHolder bh = cs.convert(
                CONVERTER_NAME,
                getBlobFromPath("test-docs/email/only_html_with_attachments.eml"),
                null);
        assertNotNull(bh);
        Blob result = bh.getBlob();
        assertNotNull(result);
        assertEquals("text/plain", result.getMimeType());
        Blob expected = getTestBlob("test-docs/email/only_html_with_attachments.txt");

        assertTrue(textEquals(expected.getString(), result.getString()));
    }

}
