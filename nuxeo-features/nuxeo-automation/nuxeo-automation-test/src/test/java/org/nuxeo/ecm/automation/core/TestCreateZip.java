/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vincent Dutat
 */
package org.nuxeo.ecm.automation.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.blob.CreateZip;
import org.nuxeo.ecm.automation.core.operations.blob.GetDocumentBlob;
import org.nuxeo.ecm.automation.core.operations.document.GetDocumentChildren;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
public class TestCreateZip {

    @Inject
    CoreSession session;

    @Inject
    AutomationService as;

    @Test
    public void isServiceDeployed() {
        assertNotNull(as);
    }

    @Test
    public void canGetZip() throws Exception {
        DocumentModel ws1 = session.createDocumentModel("Workspace");
        ws1.setPathInfo("/", "ws1");
        ws1 = session.createDocument(ws1);
        DocumentModel doc2 = session.createDocumentModel("File");
        doc2.setPathInfo("/ws1", "doc2");
        doc2.setPropertyValue("file:content", (Serializable) Blobs.createBlob("content doc2"));
        doc2 = session.createDocument(doc2);
        DocumentModel doc = session.createDocumentModel("File");
        doc.setPathInfo("/ws1", "doc1");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("content doc1"));
        doc = session.createDocument(doc);
        session.save();
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(ws1);
            OperationChain chain = new OperationChain("ZipWs");
            chain.add(GetDocumentChildren.ID);
            chain.add(GetDocumentBlob.ID);
            chain.add(CreateZip.ID).set("filename", "zip.zip");
            Blob zipBlob = (Blob) as.run(ctx, chain);

            assertNotNull(zipBlob);
            assertTrue("ZIP blob '" + zipBlob.getFilename() + "' is empty", zipBlob.getLength() > 0);
        }
    }

}
