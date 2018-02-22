/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: TestHelpers.java 26808 2007-11-05 12:00:39Z atchertchian $
 */

package org.nuxeo.ecm.platform.layout.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.NuxeoLayoutIdManagerBean;
import org.nuxeo.ecm.platform.forms.layout.facelets.ValueExpressionHelper;
import org.nuxeo.ecm.platform.ui.web.jsf.MockFacesContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.forms.layout.client.tests:layouts-test-schemas.xml")
public class TestHelpers {

    @Test
    public void testValueExpressionHelper() {
        FieldDefinition fieldDef = new FieldDefinitionImpl("dublincore", "title");
        String expression = ValueExpressionHelper.createExpressionString("document", fieldDef);
        assertEquals("#{document['dublincore']['title']}", expression);
        fieldDef = new FieldDefinitionImpl(null, "dc:title");
        expression = ValueExpressionHelper.createExpressionString("document", fieldDef);
        assertEquals("#{document['dc']['title']}", expression);
        fieldDef = new FieldDefinitionImpl(null, "dublincore:title");
        expression = ValueExpressionHelper.createExpressionString("document", fieldDef);
        assertEquals("#{document['dublincore']['title']}", expression);
        fieldDef = new FieldDefinitionImpl(null, "dc:contributors/0/name");
        expression = ValueExpressionHelper.createExpressionString("document", fieldDef);
        assertEquals("#{document['dc']['contributors'][0]['name']}", expression);
        fieldDef = new FieldDefinitionImpl(null, "test-schema:test-field");
        expression = ValueExpressionHelper.createExpressionString("document", fieldDef);
        assertEquals("#{document['test-schema']['test-field']}", expression);
        fieldDef = new FieldDefinitionImpl(null, "data.ref");
        expression = ValueExpressionHelper.createExpressionString("pageSelection", fieldDef);
        assertEquals("#{pageSelection.data.ref}", expression);
        fieldDef = new FieldDefinitionImpl("data", "ref");
        expression = ValueExpressionHelper.createExpressionString("pageSelection", fieldDef);
        assertEquals("#{pageSelection['data']['ref']}", expression);

        fieldDef = new FieldDefinitionImpl(null, "contextData['request/comment']");
        expression = ValueExpressionHelper.createExpressionString("document", fieldDef);
        assertEquals("#{document.contextData['request/comment']}", expression);

        fieldDef = new FieldDefinitionImpl(null, "data.dc.contributors[fn:length(data.dc.contributors)-1]");
        expression = ValueExpressionHelper.createExpressionString("row", fieldDef);
        assertEquals("#{row.data.dc.contributors[fn:length(data.dc.contributors)-1]}", expression);
    }

    public static String getTestFile(String filePath) {
        return FileUtils.getResourcePathFromContext(filePath);
    }

    protected byte[] getGeneratedInputStream(Document doc) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = null;
        try {
            writer = new XMLWriter(out, format);
            writer.write(doc.getDocument());

        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        byte[] res = out.toByteArray();

        // for debug
        File file = Framework.createTempFile("test", ".xml");
        FileOutputStream fileOut = new FileOutputStream(file);
        fileOut.write(res);
        fileOut.close();

        return res;
    }

    @Test
    public void testLayoutAutomaticGeneration() throws Exception {
        SchemaManager sm = Framework.getService(SchemaManager.class);
        Document doc = LayoutAutomaticGeneration.generateLayoutOutput(sm, "dublincore", false);

        byte[] generated = getGeneratedInputStream(doc);

        InputStream expected = new FileInputStream(getTestFile("layouts-generated-contrib.xml"));

        InputStream generatedStream = new ByteArrayInputStream(generated);

        assertEquals(read(expected).replaceAll("\r?\n", ""), read(generatedStream).replaceAll("\r?\n", ""));
    }

    @Test
    public void testLayoutAutomaticGenerationWithLabel() throws Exception {
        SchemaManager sm = Framework.getService(SchemaManager.class);
        Document doc = LayoutAutomaticGeneration.generateLayoutOutput(sm, "dublincore", true);

        byte[] generated = getGeneratedInputStream(doc);

        InputStream expected = new FileInputStream(getTestFile("layouts-generated-with-labels-contrib.xml"));

        InputStream generatedStream = new ByteArrayInputStream(generated);

        assertEquals(read(expected).replaceAll("\r?\n", ""), read(generatedStream).replaceAll("\r?\n", ""));
    }

    protected String read(InputStream in) throws IOException {
        return IOUtils.toString(in, UTF_8);
    }

    @Test
    public void testGenerateUniqueId() throws Exception {
        MockFacesContext faces = new MockFacesContext();
        faces.mapExpression("#{" + NuxeoLayoutIdManagerBean.NAME + "}", new NuxeoLayoutIdManagerBean());
        String unique_1 = FaceletHandlerHelper.generateUniqueId(faces, "foo");
        assertEquals("foo", unique_1);
        String unique_2 = FaceletHandlerHelper.generateUniqueId(faces, "foo");
        assertEquals("foo_1", unique_2);
        // ask for a name already incremented
        String unique_3 = FaceletHandlerHelper.generateUniqueId(faces, "foo_1");
        assertEquals("foo_2", unique_3);
        // again with several levels
        String unique_4 = FaceletHandlerHelper.generateUniqueId(faces, "foo_1_1");
        assertEquals("foo_3", unique_4);
    }
}
