/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Ricardo Dias
 */

package org.nuxeo.ecm.platform.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * @since 5.2
 */
public class TestOffice2html extends BaseConverterTest {

    protected void doTestHtmlConverter(String srcMT, String fileName) throws Exception {
        String converterName = cs.getConverterName(srcMT, "text/html");
        assertEquals("office2html", converterName);

        checkConverterAvailability(converterName);
        checkCommandAvailability("soffice");

        BlobHolder hg = getBlobFromPath("test-docs/" + fileName, srcMT);
        Map<String, Serializable> parameters = new HashMap<>();

        BlobHolder result = cs.convert(converterName, hg, parameters);
        assertNotNull(result);

        String html = result.getBlob().getString();
        assertTrue(html.contains("Hello"));
    }

    @Test
    public void testOfficeToHtmlConverter() throws Exception {
        doTestHtmlConverter("application/vnd.ms-excel", "hello.xls");
        doTestHtmlConverter("application/vnd.sun.xml.writer", "hello.sxw");
        doTestHtmlConverter("application/vnd.oasis.opendocument.text", "hello.odt");
        doTestHtmlConverter("application/vnd.sun.xml.calc", "hello.sxc");
        doTestHtmlConverter("application/vnd.oasis.opendocument.spreadsheet", "hello.ods");
    }

}
