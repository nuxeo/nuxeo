/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.template.manager.api")
@Deploy("org.nuxeo.template.manager")
public class TestAdapters {

    @Inject
    protected CoreSession session;

    @Test
    public void testAdapters() throws Exception {

        // create a template doc
        DocumentModel root = session.getRootDocument();
        DocumentModel template = session.createDocumentModel(root.getPathAsString(), "template", "TemplateSource");
        template.setProperty("dublincore", "title", "MyTemplate");
        File file = FileUtils.getResourceFileFromContext("data/hello.docx");
        assertNotNull(file);
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("hello.docx");
        template.setProperty("file", "content", fileBlob);
        template = session.createDocument(template);

        // test the adapter
        TemplateSourceDocument templateSource = template.getAdapter(TemplateSourceDocument.class);
        assertNotNull(templateSource);

        Blob blob = templateSource.getTemplateBlob();
        assertNotNull(blob);
        assertEquals("hello.docx", blob.getFilename());

        // add fields
        TemplateInput input1 = new TemplateInput("field1", "Value1");
        input1.setDesciption("Some description");

        templateSource.addInput(input1);
        assertNotNull(templateSource.getAdaptedDoc().getPropertyValue("tmpl:templateData"));
        assertEquals(1, templateSource.getParams().size());
        assertNotNull(templateSource.getParamsAsString());
        assertTrue(templateSource.hasInput("field1"));
        assertFalse(templateSource.hasInput("field2"));

        template = templateSource.save();

        session.save();

        // Test template based doc

        DocumentModel testDoc = session.createDocumentModel(root.getPathAsString(), "templatedDoc", "TemplateBasedFile");
        testDoc.setProperty("dublincore", "title", "MyTestDoc");
        testDoc = session.createDocument(testDoc);

        TemplateBasedDocument adapter = testDoc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(adapter);

        // associated to template
        testDoc = adapter.setTemplate(templateSource.getAdaptedDoc(), true);
        List<String> templateNames = adapter.getTemplateNames();
        assertNotNull(templateNames);
        assertEquals(1, templateNames.size());
        assertEquals(templateSource.getName(), templateNames.get(0));

        testDoc = adapter.initializeFromTemplate(templateSource.getName(), true);

        // check fields
        List<TemplateInput> copiedParams = adapter.getParams(templateSource.getName());
        assertNotNull(copiedParams);
        assertEquals(1, copiedParams.size());
        assertEquals("field1", copiedParams.get(0).getName());
        assertEquals("Value1", copiedParams.get(0).getStringValue());

        // check update
        copiedParams.get(0).setStringValue("newValue");
        adapter.saveParams(templateSource.getName(), copiedParams, true);
        copiedParams = adapter.getParams(templateSource.getName());
        assertEquals("newValue", copiedParams.get(0).getStringValue());

        // create a second template doc
        DocumentModel template2 = session.createDocumentModel(root.getPathAsString(), "template2", "TemplateSource");
        template2.setProperty("dublincore", "title", "MyTemplate2");
        file = FileUtils.getResourceFileFromContext("data/testDoc.odt");
        assertNotNull(file);
        fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("hello.odt");
        template2.setProperty("file", "content", fileBlob);
        template2 = session.createDocument(template2);

        // test the adapter
        TemplateSourceDocument templateSource2 = template2.getAdapter(TemplateSourceDocument.class);
        assertNotNull(templateSource2);

        // associated to template
        testDoc = adapter.setTemplate(templateSource2.getAdaptedDoc(), true);
        templateNames = adapter.getTemplateNames();
        assertNotNull(templateNames);
        assertEquals(2, templateNames.size());
        assertTrue(templateNames.contains(templateSource.getName()));
        assertTrue(templateNames.contains(templateSource2.getName()));

    }

}
