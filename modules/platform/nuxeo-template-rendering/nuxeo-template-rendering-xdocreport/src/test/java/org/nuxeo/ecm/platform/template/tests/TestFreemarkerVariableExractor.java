/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.template.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.processors.xdocreport.XDocReportProcessor;
import org.nuxeo.template.serializer.service.TemplateSerializerService;

public class TestFreemarkerVariableExractor extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        deployBundle("org.nuxeo.template.manager.api");
        deployBundle("org.nuxeo.template.manager");
    }

    @Test
    public void testDocXParamExtraction() throws Exception {
        XDocReportProcessor processor = new XDocReportProcessor();
        File file = FileUtils.getResourceFileFromContext("data/testDoc.docx");

        List<TemplateInput> inputs = processor.getInitialParametersDefinition(Blobs.createBlob(file));

        String[] expectedVars = new String[] { "StringVar", "DateVar", "Description", "BooleanVar" };

        assertEquals(expectedVars.length, inputs.size());
        for (String expected : expectedVars) {
            boolean found = false;
            for (TemplateInput input : inputs) {
                if (expected.equals(input.getName())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }

        String xmlParams = Framework.getService(TemplateSerializerService.class).serializeXML(inputs);

        for (TemplateInput input : inputs) {
            assertTrue(xmlParams.contains("name=\"" + input.getName() + "\""));
        }

        List<TemplateInput> inputs2 = Framework.getService(TemplateSerializerService.class).deserializeXML(xmlParams);

        assertEquals(inputs.size(), inputs2.size());
        for (TemplateInput input : inputs) {
            boolean found = false;
            for (TemplateInput input2 : inputs2) {
                if (input2.getName().equals(input.getName())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }

    @Test
    public void testODTParamExtraction() throws Exception {
        XDocReportProcessor processor = new XDocReportProcessor();
        File file = FileUtils.getResourceFileFromContext("data/testDoc.odt");

        List<TemplateInput> inputs = processor.getInitialParametersDefinition(Blobs.createBlob(file));

        String[] expectedVars = new String[] { "StringVar", "DateVar", "Description", "BooleanVar" };

        assertEquals(expectedVars.length, inputs.size());
        for (String expected : expectedVars) {
            boolean found = false;
            for (TemplateInput input : inputs) {
                if (expected.equals(input.getName())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }

        String xmlParams = Framework.getService(TemplateSerializerService.class).serializeXML(inputs);

        for (TemplateInput input : inputs) {
            assertTrue(xmlParams.contains("name=\"" + input.getName() + "\""));
        }

        List<TemplateInput> inputs2 = Framework.getService(TemplateSerializerService.class).deserializeXML(xmlParams);

        assertEquals(inputs.size(), inputs2.size());
        for (TemplateInput input : inputs) {
            boolean found = false;
            for (TemplateInput input2 : inputs2) {
                if (input2.getName().equals(input.getName())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }

    @Test
    public void testDocXBrokenParamExtraction() throws Exception {

        pushInlineDeployments("org.nuxeo.template.manager.xdocreport.test:context-extension-contrib.xml");

        XDocReportProcessor processor = new XDocReportProcessor();
        File file = FileUtils.getResourceFileFromContext("data/brokenVariables.docx");

        List<TemplateInput> inputs = processor.getInitialParametersDefinition(Blobs.createBlob(file));

        // only one variable because of broken MERGEFIELD
        String[] expectedVars = new String[] { "func", };

        assertEquals(expectedVars.length, inputs.size());
        for (String expected : expectedVars) {
            boolean found = false;
            for (TemplateInput input : inputs) {
                if (expected.equals(input.getName())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }
}
