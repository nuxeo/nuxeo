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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.tests.advanced;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestAdvancedXMLZipConversion extends AdvancedXMLZipConverterTest {

    // Test docx2text
    @Test
    public void testDocxConverter() throws Exception {
        doTestTextConverter(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx2text", "advanced/paragraphs.docx");
    }

    // Test pptx2text
    @Test
    public void testPptxConverter() throws Exception {
        doTestTextConverter(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "pptx2text", "advanced/paragraphs.pptx");
    }

    // Test oo2text
    @Test
    public void testOOWriterConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.writer", "oo2text",
                "advanced/paragraphs.odt");
    }

    @Test
    public void testOOImpressConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.impress", "oo2text",
                "advanced/paragraphs.odp");
    }
}
