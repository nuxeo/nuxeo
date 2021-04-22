/*
 * (C) Copyright 2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * Tests the enricher priority: make sure that an enricher B with the same name as an enricher A and a higher
 * Setup#priority() than A takes precedence over A.
 * <p>
 * Note that enricher deployment order doesn't matter since the MarshallerRegistry keeps the writers in a SortedMap.
 *
 * @since 11.5
 */
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:enrichers-contrib.xml")
@Deploy("org.nuxeo.ecm.core.test.tests:enrichers-override-contrib.xml")
public class JsonEnricherPriorityTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    @Inject
    protected CoreSession session;

    public JsonEnricherPriorityTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Test
    public void testPriorities() throws IOException {
        DocumentModel rootDoc = session.getRootDocument();
        RenderingContext context = CtxBuilder.enrichDoc(OverrideDummyEnricher.NAME).get();
        JsonAssert json = jsonAssert(rootDoc, context);
        json = json.has("contextParameters").isObject();
        json = json.properties(1);
        json = json.has("dummyEnricher").isObject();
        json = json.properties(1);
        json.has("joe").isText().isEquals("doe");
    }

}
