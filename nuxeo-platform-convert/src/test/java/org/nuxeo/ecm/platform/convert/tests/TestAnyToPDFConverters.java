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

package org.nuxeo.ecm.platform.convert.tests;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.runtime.api.Framework;


public class TestAnyToPDFConverters extends BaseConverterTest {

    private static final Log log = LogFactory.getLog(TestAnyToPDFConverters.class);

    protected void doTestPDFConverter(String srcMT, String fileName)
            throws Exception {

        ConversionService cs = Framework.getLocalService(ConversionService.class);

        String converterName = cs.getConverterName(srcMT, "application/pdf");
        assertEquals("any2pdf", converterName);

        ConverterCheckResult check = cs.isConverterAvailable(converterName);
        assertNotNull(check);
        if (!check.isAvailable()) {
            log.warn("Skipping JOD based converter tests since OOo is not installed");
            log.warn("  converter check output : " + check.getInstallationMessage());
            log.warn("  converter check output : " + check.getErrorMessage());
            return;
        }

        BlobHolder hg = getBlobFromPath("test-docs/" + fileName, srcMT);

        BlobHolder result = cs.convert(converterName, hg, null);
        assertNotNull(result);

        File pdfFile = File.createTempFile("testingPDFConverter", ".pdf");
        result.getBlob().transferTo(pdfFile);
        String text = readPdfText(pdfFile);
        assertTrue(text.contains("Hello"));
    }

    public void testAnyToTextConverter() throws Exception {
        ConversionService cs = Framework.getLocalService(ConversionService.class);
        ConverterCheckResult check = cs.isConverterAvailable("any2pdf");
        assertNotNull(check);
        if (!check.isAvailable()) {
            log.warn("Skipping JOD based converter tests since OOo is not installed");
            log.warn("  converter check output : " + check.getInstallationMessage());
            log.warn("  converter check output : " + check.getErrorMessage());
            return;
        }

        //doTestPDFConverter("text/html",  "hello.html");
        //doTestPDFConverter("text/xml",  "hello.xml");
        doTestPDFConverter("application/vnd.ms-excel", "hello.xls");
        doTestPDFConverter("application/vnd.sun.xml.writer", "hello.sxw");
        doTestPDFConverter("application/vnd.oasis.opendocument.text", "hello.odt");
        doTestPDFConverter("application/vnd.sun.xml.calc", "hello.sxc");
        doTestPDFConverter("application/vnd.oasis.opendocument.spreadsheet", "hello.ods");
        doTestPDFConverter("application/vnd.sun.xml.impress", "hello.sxi");
        doTestPDFConverter("application/vnd.oasis.opendocument.presentation", "hello.odp");
    }

}
