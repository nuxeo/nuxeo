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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.html;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class TestHtml2TextTransformer extends AbstractPluginTestCase {

    private static final String TRANSFORMER_NAME = "html2text";

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

    public void testHtml2TextTransformation() throws Exception {
        String html = "<em>this is</em> a     <strong>Test</strong>\n <em>test4</em>\n te4s4t\n 12345";
        String expected = "this is a Test test4 te4s4t 12345";

        List<TransformDocument> results = transformer.transform(null,
                new StringBlob(html));

        Blob result = results.get(0).getBlob();

        assertEquals("text/plain", result.getMimeType());
        assertEquals(expected, result.getString());
        System.out.println("RESULT: " + result.getString());
    }
}
