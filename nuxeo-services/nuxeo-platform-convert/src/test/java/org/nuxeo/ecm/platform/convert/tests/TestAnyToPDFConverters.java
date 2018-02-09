/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 *     Florent Guillaume
 *     Thierry Delprat
 *     Ricardo Dias
 */
package org.nuxeo.ecm.platform.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class TestAnyToPDFConverters extends BaseConverterTest {

    protected void doTestPDFConverter(String srcMT, String fileName)
            throws Exception {

        String converterName = cs.getConverterName(srcMT, "application/pdf");
        assertEquals("any2pdf", converterName);

        checkConverterAvailability(converterName);
        checkCommandAvailability("soffice");

        BlobHolder hg = getBlobFromPath("test-docs/" + fileName, srcMT);
        Map<String, Serializable> parameters = new HashMap<>();

        // do the conversion
        BlobHolder result = cs.convert(converterName, hg, parameters);
        assertNotNull(result);

        String text = DocumentUTUtils.readPdfText(result.getBlob().getFile());
        assertTrue(text.contains("Hello") || text.contains("hello"));
    }

    @Test
    public void testAnyToPDFConverter() throws Exception {
        doTestPDFConverter("text/html", "hello.html");
        doTestPDFConverter("text/xml", "hello.xml");
        doTestPDFConverter("application/vnd.ms-excel", "hello.xls");
        doTestPDFConverter("application/vnd.sun.xml.writer", "hello.sxw");
        doTestPDFConverter("application/vnd.oasis.opendocument.text", "hello.odt");
        doTestPDFConverter("application/vnd.sun.xml.calc", "hello.sxc");
        doTestPDFConverter("application/vnd.oasis.opendocument.spreadsheet", "hello.ods");
        doTestPDFConverter("application/vnd.sun.xml.impress", "hello.sxi");
        doTestPDFConverter("application/vnd.oasis.opendocument.presentation", "hello.odp");

        doTestPDFConverter("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "hello.docx");
        doTestPDFConverter("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "hello.xlsx");
        doTestPDFConverter("application/vnd.openxmlformats-officedocument.presentationml.presentation", "hello.pptx");
    }

    protected class ConversionThread extends Thread {

        boolean exception = false;

        boolean terminated = false;

        @Override
        public void run() {
            try {
                testAnyToPDFConverter();
            } catch (Exception e) {
                exception = false;
            } finally {
                terminated = true;
            }
        }
    }

    @Test
    public void testMultiThreadsConverter() throws Exception {

        int t = 0;
        int tMax = 120;
        ConversionThread t1 = new ConversionThread();
        ConversionThread t2 = new ConversionThread();

        t1.start();
        t2.start();

        while (!(t1.terminated && t2.terminated)) {
            Thread.sleep(1000);
            t += 1;
            if (t > tMax) {
                if (!t1.terminated) {
                    t1.interrupt();
                }
                if (!t2.terminated) {
                    t2.interrupt();
                }
                break;
            }
        }

        assertFalse(t1.exception);
        assertFalse(t2.exception);
    }

}
