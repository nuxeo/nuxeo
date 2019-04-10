/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.InputType;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.processors.xdocreport.ZipXmlHelper;

public class TestXDocReportDOCXProcessing extends SimpleTemplateDocTestCase {

    @Test
    public void testFileUpdateFromParams() throws Exception {

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        List<TemplateInput> params = getTestParams();

        testDoc = adapter.saveParams(TEMPLATE_NAME, params, true);
        session.save();

        Blob newBlob = adapter.renderAndStoreAsAttachment(TEMPLATE_NAME, true);

        String xmlContent = ZipXmlHelper.readXMLContent(newBlob, ZipXmlHelper.DOCX_MAIN_FILE);

        assertTrue(xmlContent.contains("John Smith"));
        assertTrue(xmlContent.contains("some description"));
        assertTrue(xmlContent.contains("The Boolean value is false"));
        assertTrue(xmlContent.contains("r:embed=\"xdocreport_0\""));

    }

    @Test
    public void testWithMissingPicture() throws Exception {

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        // remove picture
        List<Map<String, Serializable>> blobs = new ArrayList<Map<String, Serializable>>();
        testDoc.setPropertyValue("files:files", (Serializable) blobs);
        testDoc = session.saveDocument(testDoc);

        adapter = testDoc.getAdapter(TemplateBasedDocument.class);

        List<TemplateInput> params = getTestParams();

        testDoc = adapter.saveParams(TEMPLATE_NAME, params, true);
        session.save();

        Blob newBlob = adapter.renderAndStoreAsAttachment(TEMPLATE_NAME, true);

        String xmlContent = ZipXmlHelper.readXMLContent(newBlob, ZipXmlHelper.DOCX_MAIN_FILE);

        assertTrue(xmlContent.contains("John Smith"));
        assertTrue(xmlContent.contains("some description"));
        assertTrue(xmlContent.contains("The Boolean value is false"));
        assertTrue(xmlContent.contains("r:embed=\"xdocreport_0\""));

    }

    @Test
    public void testWithMissingPicture2() throws Exception {

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        // remove picture
        testDoc.setPropertyValue("files:files", (Serializable) null);
        testDoc = session.saveDocument(testDoc);

        adapter = testDoc.getAdapter(TemplateBasedDocument.class);

        List<TemplateInput> params = getTestParams();

        testDoc = adapter.saveParams(TEMPLATE_NAME, params, true);
        session.save();

        Blob newBlob = adapter.renderAndStoreAsAttachment(TEMPLATE_NAME, true);

        String xmlContent = ZipXmlHelper.readXMLContent(newBlob, ZipXmlHelper.DOCX_MAIN_FILE);

        assertTrue(xmlContent.contains("John Smith"));
        assertTrue(xmlContent.contains("some description"));
        assertTrue(xmlContent.contains("The Boolean value is false"));
        assertTrue(xmlContent.contains("r:embed=\"xdocreport_0\""));

    }

    @Test
    public void testWithMissingPicture3() throws Exception {

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        adapter = testDoc.getAdapter(TemplateBasedDocument.class);

        List<TemplateInput> params = getTestParams();

        // set wrong input
        TemplateInput input5 = new TemplateInput("picture");
        input5.setType(InputType.PictureProperty);
        input5.setSource("dc:source");
        params.remove(4);
        params.add(input5);

        testDoc = adapter.saveParams(TEMPLATE_NAME, params, true);
        session.save();

        adapter = testDoc.getAdapter(TemplateBasedDocument.class);

        Blob newBlob = adapter.renderAndStoreAsAttachment(TEMPLATE_NAME, true);

        String xmlContent = ZipXmlHelper.readXMLContent(newBlob, ZipXmlHelper.DOCX_MAIN_FILE);

        assertTrue(xmlContent.contains("John Smith"));
        assertTrue(xmlContent.contains("some description"));
        assertTrue(xmlContent.contains("The Boolean value is false"));
        assertTrue(xmlContent.contains("r:embed=\"xdocreport_0\""));

    }

    @Override
    protected Blob getTemplateBlob() throws IOException {
        File file = FileUtils.getResourceFileFromContext("data/testDoc.docx");
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("testDoc.docx");
        return fileBlob;
    }

}
