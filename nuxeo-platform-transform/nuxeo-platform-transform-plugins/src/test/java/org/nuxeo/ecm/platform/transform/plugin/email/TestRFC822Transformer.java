/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.plugin.email;

import java.io.File;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;

/**
 * @author Anahide Tchertchian
 *
 */
public class TestRFC822Transformer extends AbstractPluginTestCase {

    private static final String TRANSFORMER_NAME = "rfc822totext";

    private Transformer transformer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        transformer = service.getTransformerByName(TRANSFORMER_NAME);
    }

    @Override
    public void tearDown() throws Exception {
        transformer = null;
        super.tearDown();
    }

    private Blob getTestBlob(String filePath) {
        File file = FileUtils.getResourceFileFromContext(filePath);
        return new FileBlob(file);
    }

    public void testTextEmailTransformation() throws Exception {
        List<TransformDocument> results = transformer.transform(null,
                getTestBlob("test-data/email/text.eml"));
        Blob result = results.get(0).getBlob();
        assertEquals("text/plain", result.getMimeType());
        Blob expected = getTestBlob("test-data/email/text.txt");
        assertEquals(expected.getString(), result.getString());
    }

    public void testTextAndHtmlEmailTransformation() throws Exception {
        List<TransformDocument> results = transformer.transform(
                null,
                getTestBlob("test-data/email/text_and_html_with_attachments.eml"));
        Blob result = results.get(0).getBlob();
        assertEquals("text/plain", result.getMimeType());
        Blob expected = getTestBlob("test-data/email/text_and_html_with_attachments.txt");
        assertEquals(expected.getString(), result.getString());
    }

    public void testOnlyHtmlEmailTransformation() throws Exception {
        List<TransformDocument> results = transformer.transform(null,
                getTestBlob("test-data/email/only_html_with_attachments.eml"));
        Blob result = results.get(0).getBlob();
        assertEquals("text/plain", result.getMimeType());
        Blob expected = getTestBlob("test-data/email/only_html_with_attachments.txt");
        assertEquals(expected.getString(), result.getString());
    }

}
