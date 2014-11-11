/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: TestHelpers.java 26808 2007-11-05 12:00:39Z atchertchian $
 */

package org.nuxeo.ecm.platform.layout.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.ValueExpressionHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestHelpers extends NXRuntimeTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client.tests",
                "layouts-test-schemas.xml");
    }

    @Test
    public void testValueExpressionHelper() {
        FieldDefinition fieldDef = new FieldDefinitionImpl("dublincore",
                "title");
        String expression = ValueExpressionHelper.createExpressionString(
                "document", fieldDef);
        assertEquals("#{document['dublincore']['title']}", expression);
        fieldDef = new FieldDefinitionImpl(null, "dc:title");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document['dc']['title']}", expression);
        fieldDef = new FieldDefinitionImpl(null, "dublincore:title");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document['dublincore']['title']}", expression);
        fieldDef = new FieldDefinitionImpl(null, "dc:contributors/0/name");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document['dc']['contributors'][0]['name']}", expression);
        fieldDef = new FieldDefinitionImpl(null, "test-schema:test-field");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document['test-schema']['test-field']}", expression);
        fieldDef = new FieldDefinitionImpl(null, "data.ref");
        expression = ValueExpressionHelper.createExpressionString(
                "pageSelection", fieldDef);
        assertEquals("#{pageSelection.data.ref}", expression);
        fieldDef = new FieldDefinitionImpl("data", "ref");
        expression = ValueExpressionHelper.createExpressionString(
                "pageSelection", fieldDef);
        assertEquals("#{pageSelection['data']['ref']}", expression);

        fieldDef = new FieldDefinitionImpl(null,
                "contextData['request/comment']");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document.contextData['request/comment']}", expression);

        fieldDef = new FieldDefinitionImpl(null,
                "data.dc.contributors[fn:length(data.dc.contributors)-1]");
        expression = ValueExpressionHelper.createExpressionString("row",
                fieldDef);
        assertEquals(
                "#{row.data.dc.contributors[fn:length(data.dc.contributors)-1]}",
                expression);
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
        File file = File.createTempFile("test", ".xml");
        FileOutputStream fileOut = new FileOutputStream(file);
        fileOut.write(res);

        return res;
    }

    @Test
    public void testLayoutAutomaticGeneration() throws Exception {
        SchemaManager sm = Framework.getService(SchemaManager.class);
        Document doc = LayoutAutomaticGeneration.generateLayoutOutput(sm,
                "dublincore", false);

        byte[] generated = getGeneratedInputStream(doc);

        InputStream expected = new FileInputStream(
                getTestFile("layouts-generated-contrib.xml"));

        InputStream generatedStream = new ByteArrayInputStream(generated);

        assertEquals(FileUtils.read(expected).replaceAll("\r?\n", ""),
                FileUtils.read(generatedStream).replaceAll("\r?\n", ""));
    }

    @Test
    public void testLayoutAutomaticGenerationWithLabel() throws Exception {
        SchemaManager sm = Framework.getService(SchemaManager.class);
        Document doc = LayoutAutomaticGeneration.generateLayoutOutput(sm,
                "dublincore", true);

        byte[] generated = getGeneratedInputStream(doc);

        InputStream expected = new FileInputStream(
                getTestFile("layouts-generated-with-labels-contrib.xml"));

        InputStream generatedStream = new ByteArrayInputStream(generated);

        assertEquals(FileUtils.read(expected).replaceAll("\r?\n", ""),
                FileUtils.read(generatedStream).replaceAll("\r?\n", ""));
    }

    @Test
    public void testGenerateUniqueId() throws Exception {
        Map<String, Integer> counters = new HashMap<String, Integer>();
        String unique_1 = FaceletHandlerHelper.generateUniqueId("foo", counters);
        assertEquals("foo", unique_1);
        String unique_2 = FaceletHandlerHelper.generateUniqueId("foo", counters);
        assertEquals("foo_1", unique_2);
        // ask for a name already incremented
        String unique_3 = FaceletHandlerHelper.generateUniqueId("foo_1",
                counters);
        assertEquals("foo_2", unique_3);
        // again with several levels
        String unique_4 = FaceletHandlerHelper.generateUniqueId("foo_1_1",
                counters);
        assertEquals("foo_3", unique_4);
    }

}
