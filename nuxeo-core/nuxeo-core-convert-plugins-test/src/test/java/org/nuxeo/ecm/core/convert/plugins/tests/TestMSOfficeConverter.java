/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestMSOfficeConverter extends SimpleConverterTest {

    // Word POI tests fails in surefire
    @Test
    public void testWordConverter() throws Exception {
        doTestTextConverter("application/msword", "msoffice2text", "hello.doc");
    }

    @Test
    public void testWordArabicConverter() throws Exception {
        doTestArabicTextConverter("application/msword", "msoffice2text",
                "wikipedia-internet-ar.doc");
    }

    @Test
    public void testPptConverter() throws Exception {
        doTestTextConverter("application/vnd.ms-powerpoint", "msoffice2text",
                "hello.ppt");
    }

    @Test
    public void testXlsConverter() throws Exception {
        doTestTextConverter("application/vnd.ms-excel", "xl2text", "hello.xls");
    }

    @Test
    public void testDocxConverter() throws Exception {
        doTestTextConverter(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx2text", "hello.docx");
    }

    @Test
    public void testRtfConverter() throws Exception {
        doTestTextConverter(
                "application/rtf",
                "rtf2text", "hello.rtf");
    }

    @Test
    public void testPptxConverter() throws Exception {
        doTestTextConverter(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "pptx2text", "hello.pptx");
    }

    @Test
    public void testXlsxConverter() throws Exception {
        doTestTextConverter(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "xlx2text", "hello.xlsx");
    }

    @Test
    public void testAnyToTextConverterWord() throws Exception {
        doTestAny2TextConverter("application/msword", "msoffice2text",
                "hello.doc");
    }

    @Test
    public void testAnyToTextExcelConverter() throws Exception {
        doTestAny2TextConverter("application/vnd.ms-excel", "xl2text",
                "hello.xls");
    }

    @Test
    public void testAnyToTextDocxConverter() throws Exception {
        doTestAny2TextConverter("application/msword", "docx2text", "hello.docx");
    }

    @Test
    public void testAnyToTextXlsxConverter() throws Exception {
        doTestAny2TextConverter(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "xlx2text", "hello.xlsx");
    }
}
