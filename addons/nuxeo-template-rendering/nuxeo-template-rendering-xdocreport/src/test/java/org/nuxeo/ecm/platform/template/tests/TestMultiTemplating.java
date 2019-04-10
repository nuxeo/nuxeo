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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.processors.xdocreport.ZipXmlHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.dublincore")
@Deploy("org.nuxeo.template.manager.api")
@Deploy("org.nuxeo.template.manager")
@Deploy("org.nuxeo.template.manager.xdocreport")
public class TestMultiTemplating {

    @Inject
    protected CoreSession session;

    @Inject
    protected TemplateProcessorService tps;

    protected DocumentModel odtTemplateDoc;

    protected DocumentModel ftlTemplateDoc;

    protected DocumentModel testDoc;

    protected Blob getODTTemplateBlob() throws IOException {
        File file = FileUtils.getResourceFileFromContext("data/DocumentsAttributes.odt");
        Blob blob = Blobs.createBlob(file);
        blob.setFilename("DocumentsAttributes.odt");
        return blob;
    }

    protected Blob getFTLTemplateBlob() throws IOException {
        File file = FileUtils.getResourceFileFromContext("data/test.ftl");
        Blob blob = Blobs.createBlob(file);
        blob.setFilename("test.ftl");
        return blob;
    }

    protected void setupTestDocs() throws Exception {

        DocumentModel root = session.getRootDocument();

        // create ODT template
        odtTemplateDoc = session.createDocumentModel(root.getPathAsString(), "odtTemplatedDoc", "TemplateSource");
        odtTemplateDoc.setProperty("dublincore", "title", "ODT Template");
        odtTemplateDoc.setPropertyValue("tmpl:templateName", "odt");
        Blob fileBlob = getODTTemplateBlob();
        odtTemplateDoc.setProperty("file", "content", fileBlob);
        odtTemplateDoc = session.createDocument(odtTemplateDoc);

        session.save();

        // create FTL template
        ftlTemplateDoc = session.createDocumentModel(root.getPathAsString(), "ftlTemplatedDoc", "TemplateSource");
        ftlTemplateDoc.setProperty("dublincore", "title", "FTL Template");
        ftlTemplateDoc.setPropertyValue("tmpl:templateName", "ftl");
        fileBlob = getFTLTemplateBlob();
        ftlTemplateDoc.setProperty("file", "content", fileBlob);
        ftlTemplateDoc = session.createDocument(ftlTemplateDoc);

        session.save();

        // now create simple doc
        testDoc = session.createDocumentModel(root.getPathAsString(), "testDoc", "File");
        testDoc.setProperty("dublincore", "title", "MyTestDoc");
        testDoc.setProperty("dublincore", "description", "some description");

        // set dc:subjects
        List<String> subjects = new ArrayList<String>();
        subjects.add("Subject 1");
        subjects.add("Subject 2");
        subjects.add("Subject 3");
        testDoc.setPropertyValue("dc:subjects", (Serializable) subjects);
        testDoc = session.createDocument(testDoc);
    }

    @Test
    public void testMultiBindings() throws Exception {

        setupTestDocs();

        // bind the doc to the odt template
        testDoc = tps.makeTemplateBasedDocument(testDoc, odtTemplateDoc, true);

        // check association
        TemplateBasedDocument tbd = testDoc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(tbd);
        assertEquals(1, tbd.getTemplateNames().size());
        assertEquals("odt", tbd.getTemplateNames().get(0));

        // check rendition
        Blob rendered = tbd.renderWithTemplate("odt");
        assertNotNull(rendered);

        String xmlContent = ZipXmlHelper.readXMLContent(rendered, ZipXmlHelper.OOO_MAIN_FILE);

        assertTrue(xmlContent.contains("Subject 1"));
        assertTrue(xmlContent.contains("Subject 2"));
        assertTrue(xmlContent.contains("Subject 3"));
        assertTrue(xmlContent.contains("MyTestDoc"));
        assertTrue(xmlContent.contains("Administrator"));

        // bind the doc to FTL template
        testDoc = tbd.setTemplate(ftlTemplateDoc, true);
        assertEquals(2, tbd.getTemplateNames().size());
        assertTrue(tbd.getTemplateNames().contains("odt"));
        assertTrue(tbd.getTemplateNames().contains("ftl"));

        // check Rendition
        Blob renderedHtml = tbd.renderWithTemplate("ftl");
        assertNotNull(renderedHtml);
        String htmlContent = renderedHtml.getString();
        assertTrue(htmlContent.contains(testDoc.getTitle()));
        assertTrue(htmlContent.contains(testDoc.getId()));

        // unbind odt
        testDoc = tps.detachTemplateBasedDocument(testDoc, "odt", true);
        tbd = testDoc.getAdapter(TemplateBasedDocument.class);
        assertEquals(1, tbd.getTemplateNames().size());
        assertEquals("ftl", tbd.getTemplateNames().get(0));

        // unbind ftl
        testDoc = tps.detachTemplateBasedDocument(testDoc, "ftl", true);
        tbd = testDoc.getAdapter(TemplateBasedDocument.class);
        assertNull(tbd);

    }

}
