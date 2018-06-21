/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata
 */

package org.nuxeo.ecm.platform.rendition.io;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

import com.google.inject.Inject;

@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.rendition.core:test-renditionprovider-contrib.xml")
public class RenditionJsonEnricherTest
    extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public RenditionJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    private CoreSession session;

    @Before
    public void setup() {
        DocumentModel document = session.createDocumentModel("/", "file", "File");
        document.setPropertyValue("dc:title", "TestFile");
        document = session.createDocument(document);
    }

    @Test
    public void test() throws Exception {
        DocumentModel file = session.getDocument(new PathRef("/file"));
        JsonAssert json = jsonAssert(file, CtxBuilder.enrichDoc("renditions").get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("renditions").isArray();
        assertTrue(json.getNode().size() > 0);
    }

}
