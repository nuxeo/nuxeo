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

package org.nuxeo.ecm.platform.url.io;

import javax.inject.Inject;

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

@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.url.core")
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
public class DocumentUrlJsonEnricherTest
        extends AbstractJsonWriterTest.External<DocumentModelJsonWriter, DocumentModel> {

    public DocumentUrlJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    private CoreSession session;

    @Test
    public void test() throws Exception {
        DocumentModel root = session.getDocument(new PathRef("/"));
        // Use the given URL codec name
        JsonAssert json = jsonAssert(root, CtxBuilder.enrichDoc("documentURL").param("URLCodecName", "docid").get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("documentURL").isText();
        json.isEquals(String.format("http://fake-url.nuxeo.com/nxdoc/%s/%s", root.getRepositoryName(), root.getId()));

        // No codec name parameter, as there is no UI installed thus no "notificationDocId" codec contributed
        // documentURL should be null
        json = jsonAssert(root, CtxBuilder.enrichDoc("documentURL").get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("documentURL").isNull();
    }

}
