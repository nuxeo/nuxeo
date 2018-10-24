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
 *     Nelson Silva <nsilva@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

import java.io.IOException;
import java.io.Serializable;

@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-dummy-blob-provider.xml")
public class BlobAppLinksJsonEnricherTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    @Inject
    protected BlobManager blobManager;

    @Inject
    private CoreSession session;

    public BlobAppLinksJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Before
    public void setup() throws IOException {
        Blob b = Blobs.createBlob("foo", "video/mp4");
        String key = blobManager.getBlobProvider("dummy").writeBlob(b);
        key = "dummy:" + key;

        DocumentModel doc = session.createDocumentModel("/", "doc", "File");
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = key;
        blobInfo.mimeType = "video/mp4";
        Blob blob = new SimpleManagedBlob(blobInfo);
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(doc);
        session.save();
    }

    @Test
    public void test() throws Exception {
        DocumentModel doc = session.getDocument(new PathRef("/doc"));
        RenderingContext ctx = CtxBuilder.properties("*").enrich("blob", "appLinks").get();
        JsonAssert json = jsonAssert(doc, ctx);
        json = json.has("properties").has("file:content");
        json = json.has("appLinks").isArray().length(1).get(0);
        json.has("appName").isEquals("dummyApp");
        json.has("link").isEquals("dummyLink");
        json.has("icon").isEquals("dummyIcon");
    }
}
