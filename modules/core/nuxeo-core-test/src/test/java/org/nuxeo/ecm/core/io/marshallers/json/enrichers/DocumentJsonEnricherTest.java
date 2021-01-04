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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class DocumentJsonEnricherTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public DocumentJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    private CoreSession session;

    @Test
    public void testNoEnricher() throws Exception {
        DocumentModel root = session.getDocument(new PathRef("/"));
        JsonAssert json = jsonAssert(root, CtxBuilder.get());
        json = json.hasNot("contextParameters");
    }

    @Test
    public void testManyEnrichers() throws Exception {
        DocumentModel root = session.getDocument(new PathRef("/"));
        Map<String, String> params = new HashMap<>();
        params.put("param1", "value1");
        params.put("param2", "value2");
        RenderingContext ctx = CtxBuilder.enrichDoc("contextualParameters", "children", "breadcrumb",
                                                    "permissions")
                                         .param("contextualParameters", params)
                                         .get();
        JsonAssert json = jsonAssert(root, ctx);
        json = json.has("contextParameters");
        json.properties(5);
        json.has("children");
        json.has("breadcrumb");
        json.has("permissions");
        json.has("param1");
        json.has("param2");
    }

}
