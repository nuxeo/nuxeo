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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class BreadcrumbJsonEnricherTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public BreadcrumbJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    private DocumentModel document;

    @Inject
    private CoreSession session;

    @Before
    public void setup() {
        document = session.createDocumentModel("/", "level1", "RefDoc");
        document = session.createDocument(document);
        document = session.createDocumentModel("/level1", "level2", "RefDoc");
        document = session.createDocument(document);
        document = session.createDocumentModel("/level1/level2", "level3", "RefDoc");
        document = session.createDocument(document);
    }

    @Test
    public void test() throws Exception {
        JsonAssert json = jsonAssert(document, CtxBuilder.enrichDoc("breadcrumb").get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("breadcrumb").isObject();
        json.has("entity-type").isEquals("documents");
        json = json.has("entries").length(3);
        for (int i = 0; i < 3; i++) {
            JsonAssert doc = json.has(i);
            doc.has("entity-type").isEquals("document");
            doc.has("title").isEquals("level" + (i + 1));
        }
    }

}
