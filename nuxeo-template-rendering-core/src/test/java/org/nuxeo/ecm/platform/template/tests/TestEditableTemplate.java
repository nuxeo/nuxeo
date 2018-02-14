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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;

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
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.dublincore")
@Deploy("org.nuxeo.template.manager.api")
@Deploy("org.nuxeo.template.manager")
public class TestEditableTemplate {

    protected static final String TEMPLATE_NAME = "mytestTemplate";

    @Inject
    protected CoreSession session;

    @Inject
    protected TemplateProcessorService tps;

    protected TemplateBasedDocument setupTestDocs() throws Exception {

        DocumentModel root = session.getRootDocument();

        // create template
        DocumentModel templateDoc = session.createDocumentModel(root.getPathAsString(), "templatedDoc",
                "TemplateSource");
        templateDoc.setProperty("dublincore", "title", "MyTemplate");
        File file = FileUtils.getResourceFileFromContext("data/test.ftl");
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("test.ftl");
        templateDoc.setProperty("file", "content", fileBlob);
        templateDoc.setPropertyValue("tmpl:templateName", TEMPLATE_NAME);
        templateDoc = session.createDocument(templateDoc);

        // now create a template based doc
        DocumentModel testDoc = session.createDocumentModel(root.getPathAsString(), "templatedBasedDoc",
                "TemplateBasedFile");
        testDoc.setProperty("dublincore", "title", "MyTestDoc");
        testDoc.setProperty("dublincore", "description", "some description");

        testDoc = session.createDocument(testDoc);

        // associate doc and template
        TemplateBasedDocument adapter = testDoc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(adapter);

        adapter.setTemplate(templateDoc, true);

        return adapter;
    }

    @Test
    public void testEdiableTemplate() throws Exception {
        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel doc = adapter.getAdaptedDoc();
        TemplateSourceDocument source = adapter.getSourceTemplate(TEMPLATE_NAME);
        assertNotNull(adapter);

        // check that the template has not been copied
        assertNull(adapter.getAdaptedDoc().getPropertyValue("file:content"));

        // check source template
        Blob templateBlob = adapter.getTemplateBlob(TEMPLATE_NAME);
        assertNotNull(templateBlob);
        assertTrue(templateBlob.getString().contains("This is document"));

        // check rendintion
        Blob newBlob = adapter.renderWithTemplate(TEMPLATE_NAME);
        String xmlContent = newBlob.getString();
        assertTrue(xmlContent.contains(doc.getTitle()));
        assertTrue(xmlContent.contains(doc.getId()));

        // detach the doc
        doc = tps.detachTemplateBasedDocument(doc, TEMPLATE_NAME, true);

        // change template config
        DocumentModel sourceDoc = source.getAdaptedDoc();
        sourceDoc.setPropertyValue("tmpl:useAsMainContent", true);
        sourceDoc = session.saveDocument(sourceDoc);
        session.save();

        doc = tps.makeTemplateBasedDocument(doc, sourceDoc, true);
        adapter = doc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(adapter);

        // force refresh
        doc = adapter.initializeFromTemplate(TEMPLATE_NAME, true);
        adapter = doc.getAdapter(TemplateBasedDocument.class);

        Blob localTemplate = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(localTemplate);

        Blob newTemplate = Blobs.createBlob("Hello ${doc.id}");
        newTemplate.setFilename("newTemplate.ftl");

        doc.setPropertyValue("file:content", (Serializable) newTemplate);
        doc = session.saveDocument(doc);
        session.save();

        adapter = doc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(adapter);

        Blob result = adapter.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);

        // System.out.println("Result=\n" + result.getString());

        assertTrue(result.getString().contains("Hello"));
        assertTrue(result.getString().contains(doc.getId()));

    }

}
