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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.descriptors.FieldDescriptor;
import org.nuxeo.ecm.platform.forms.layout.facelets.ValueExpressionHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class TestHelpers extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client.tests",
                "layouts-test-schemas.xml");
    }

    public void testValueExpressionHelper() {
        FieldDefinition fieldDef = new FieldDescriptor("dublincore", "title");
        String expression = ValueExpressionHelper.createExpressionString(
                "document", fieldDef);
        assertEquals("#{document['dublincore']['title']}", expression);
        fieldDef = new FieldDescriptor(null, "dc:title");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document['dc']['title']}", expression);
        fieldDef = new FieldDescriptor(null, "dublincore:title");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document['dublincore']['title']}", expression);
        fieldDef = new FieldDescriptor(null, "dc:contributors/0/name");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document['dc']['contributors'][0]['name']}", expression);
        fieldDef = new FieldDescriptor(null, "test-schema:test-field");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document['test-schema']['test-field']}", expression);
        fieldDef = new FieldDescriptor(null, "data.ref");
        expression = ValueExpressionHelper.createExpressionString(
                "pageSelection", fieldDef);
        assertEquals("#{pageSelection.data.ref}", expression);
        fieldDef = new FieldDescriptor("data", "ref");
        expression = ValueExpressionHelper.createExpressionString(
                "pageSelection", fieldDef);
        assertEquals("#{pageSelection['data']['ref']}", expression);

        fieldDef = new FieldDescriptor(null, "contextData['request/comment']");
        expression = ValueExpressionHelper.createExpressionString("document",
                fieldDef);
        assertEquals("#{document.contextData['request/comment']}", expression);

        fieldDef = new FieldDescriptor(null,
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

}
