/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.platform.rendition.io;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.platform.rendition.service.RenditionFeature;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(RenditionFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
public class PublicationJsonEnricherTest
    extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public PublicationJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    private CoreSession session;

    @Before
    public void setup() {
        DocumentModel document = session.createDocumentModel("/", "file", "File");
        document.setPropertyValue("dc:title", "TestFile");
        document = session.createDocument(document);
        Blob blob = Blobs.createBlob("I am a Blob");
        document.setPropertyValue("file:content", (Serializable) blob);
        document = session.saveDocument(document);
    }

    @Test
    public void test() throws Exception {
        DocumentModel file = session.getDocument(new PathRef("/file"));
        DocumentModel parent = session.getDocument(file.getParentRef());
        JsonAssert json = jsonAssert(file, CtxBuilder.enrichDoc(PublicationJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("publications").isObject();
        json = json.has("resultsCount").isInt();
        assertEquals(0, json.getNode().asInt());

        session.publishDocument(file, parent);
        session.save();
        json = jsonAssert(file, CtxBuilder.enrichDoc(PublicationJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("publications").isObject();
        json = json.has("resultsCount").isInt();
        assertEquals(1, json.getNode().asInt());

        Framework.getService(RenditionService.class).publishRendition(file, parent, "pdf", false);
        session.save();
        json = jsonAssert(file, CtxBuilder.enrichDoc(PublicationJsonEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("publications").isObject();
        json = json.has("resultsCount").isInt();
        assertEquals(2, json.getNode().asInt());
    }

}
