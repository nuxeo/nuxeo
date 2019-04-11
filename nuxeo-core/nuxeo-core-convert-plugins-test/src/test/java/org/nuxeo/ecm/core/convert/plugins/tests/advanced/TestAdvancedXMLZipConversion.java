/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
        doTestTextConverter("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx2text",
                "advanced/paragraphs.docx");
    }

    // Test pptx2text
    @Test
    public void testPptxConverter() throws Exception {
        doTestTextConverter("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx2text",
                "advanced/paragraphs.pptx");
    }

    // Test oo2text
    @Test
    public void testOOWriterConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.writer", "oo2text", "advanced/paragraphs.odt");
    }

    @Test
    public void testOOImpressConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.impress", "oo2text", "advanced/paragraphs.odp");
    }
}
