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
 *
 */

package org.nuxeo.ecm.platform.convert.tests;

import org.junit.Assume;
import org.junit.Test;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.runtime.api.Framework;

import static org.junit.Assert.*;

public class TestOffice2html extends BaseConverterTest {
    protected void doTestHtmlConverter(String srcMT, String fileName) throws Exception {

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        String converterName = cs.getConverterName(srcMT, "text/html");
        assertEquals("office2html", converterName);

        ConverterCheckResult check = cs.isConverterAvailable(converterName);
        assertNotNull(check);
        Assume.assumeTrue(
                String.format("Skipping JOD based converter tests since OOo is not installed:\n"
                        + "- installation message: %s\n- error message: %s", check.getInstallationMessage(),
                        check.getErrorMessage()), check.isAvailable());

        BlobHolder hg = getBlobFromPath("test-docs/" + fileName, srcMT);

        BlobHolder result = cs.convert(converterName, hg, null);
        assertNotNull(result);

        String html = result.getBlob().getString();
        assertTrue(html.contains("Hello"));
    }

    @Test
    public void testOfficeToHtmlConverter() throws Exception {
        ConversionService cs = Framework.getLocalService(ConversionService.class);
        ConverterCheckResult check = cs.isConverterAvailable("office2html");
        assertNotNull(check);
        Assume.assumeTrue(
                String.format("Skipping JOD based converter tests since OOo is not installed:\n"
                        + "- installation message: %s\n- error message: %s", check.getInstallationMessage(),
                        check.getErrorMessage()), check.isAvailable());

        doTestHtmlConverter("application/vnd.ms-excel", "hello.xls");
        doTestHtmlConverter("application/vnd.sun.xml.writer", "hello.sxw");
        doTestHtmlConverter("application/vnd.oasis.opendocument.text", "hello.odt");
        doTestHtmlConverter("application/vnd.sun.xml.calc", "hello.sxc");
        doTestHtmlConverter("application/vnd.oasis.opendocument.spreadsheet", "hello.ods");
    }

}
