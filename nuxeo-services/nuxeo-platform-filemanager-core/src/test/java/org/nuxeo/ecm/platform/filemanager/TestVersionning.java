/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = RepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.platform.types.api", "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.filemanager.api", "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.versioning.api", "org.nuxeo.ecm.platform.versioning" })
@LocalDeploy("org.nuxeo.ecm.platform.filemanager.core:ecm-types-test-contrib.xml")
public class TestVersionning {

    protected DocumentModel destWS;

    protected DocumentModel wsRoot;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected FileManager fm;

    private void createTestDocuments() throws Exception {
        wsRoot = coreSession.getDocument(new PathRef("/default-domain/workspaces"));

        DocumentModel ws = coreSession.createDocumentModel(wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = coreSession.createDocument(ws);

        destWS = ws;

        coreSession.save();
        waitForAsyncCompletion();
    }

    protected void waitForAsyncCompletion() {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
    }

    @Test
    public void testVersioning() throws Exception {
        createTestDocuments();

        Blob blob = Blobs.createBlob("Something", "something", null, "mytest.something");
        DocumentModel doc = fm.createDocumentFromBlob(coreSession, blob, destWS.getPathAsString(), true,
                "mytest.something");
        waitForAsyncCompletion();
        assertEquals("mytest.something", doc.getTitle());
        assertEquals("0.0", doc.getVersionLabel());

        // the blob must be updated for the document to be dirty and for the version to be updated
        blob = Blobs.createBlob("Something Else", "something", null, "mytest.something");
        doc = fm.createDocumentFromBlob(coreSession, blob, destWS.getPathAsString(), true, "mytest.something");
        waitForAsyncCompletion();
        assertEquals("0.1+", doc.getVersionLabel());

        blob.setFilename("mytest2.something");
        doc = fm.createDocumentFromBlob(coreSession, blob, destWS.getPathAsString(), true, "mytest2.something");
        waitForAsyncCompletion();
        assertEquals("0.0", doc.getVersionLabel());

        blob.setFilename("mytxt.txt");
        blob.setMimeType("text/plain");
        doc = fm.createDocumentFromBlob(coreSession, blob, destWS.getPathAsString(), true, "mytxt.txt");
        waitForAsyncCompletion();
        assertEquals("Note", doc.getType());
        assertEquals("0.0", doc.getVersionLabel());

        // the blob must be updated for the document to be dirty and for the version to be updated
        blob = Blobs.createBlob("Something Diffirent", "text/plain", null, "mytxt.txt");
        doc = fm.createDocumentFromBlob(coreSession, blob, destWS.getPathAsString(), true, "mytxt.txt");
        waitForAsyncCompletion();
        assertEquals("0.1+", doc.getVersionLabel());
    }

}
