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
package org.nuxeo.template.jaxrs.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.dublincore")
@Deploy("org.nuxeo.template.manager.api")
@Deploy("org.nuxeo.template.manager")
@Deploy("org.nuxeo.template.manager.jaxrs")
public class TestTemplateSourceTypeBindings {

    @Inject
    protected CoreSession session;

    @Inject
    protected TemplateProcessorService tps;

    protected TemplateSourceDocument createTemplateDoc(String name) throws Exception {

        DocumentModel root = session.getRootDocument();

        // create template
        DocumentModel templateDoc = session.createDocumentModel(root.getPathAsString(), name, "TemplateSource");
        templateDoc.setProperty("dublincore", "title", name);
        File file = FileUtils.getResourceFileFromContext("data/testDoc.odt");
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("testDoc.odt");
        templateDoc.setProperty("file", "content", fileBlob);
        templateDoc = session.createDocument(templateDoc);
        session.save();

        TemplateSourceDocument result = templateDoc.getAdapter(TemplateSourceDocument.class);
        assertNotNull(result);
        return result;
    }

    protected TemplateSourceDocument createWebTemplateDoc(String name) throws Exception {

        DocumentModel root = session.getRootDocument();

        // create template
        DocumentModel templateDoc = session.createDocumentModel(root.getPathAsString(), name, "WebTemplateSource");
        templateDoc.setProperty("dublincore", "title", name);
        templateDoc.setProperty("note", "note", "Template ${doc.title}");
        templateDoc = session.createDocument(templateDoc);
        session.save();

        TemplateSourceDocument result = templateDoc.getAdapter(TemplateSourceDocument.class);
        assertNotNull(result);
        return result;
    }

    @SuppressWarnings("unused")
    @Test
    public void testAvailableTemplates() throws Exception {
        TemplateSourceDocument t1 = createTemplateDoc("t1");
        session.save();

        List<DocumentModel> docs = tps.getAvailableTemplateDocs(session, null);
        assertEquals(1, docs.size());

        docs = tps.getAvailableTemplateDocs(session, "all");
        assertEquals(1, docs.size());

        TemplateSourceDocument t2 = createWebTemplateDoc("t2");
        session.save();
        docs = tps.getAvailableTemplateDocs(session, "all");
        assertEquals(2, docs.size());

    }

}
