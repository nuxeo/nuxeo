/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init=RepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.platform.mimetype.api",
        "org.nuxeo.ecm.platform.mimetype.core",
        "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.filemanager.api",
        "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.versioning.api",
        "org.nuxeo.ecm.platform.versioning" })
@LocalDeploy("org.nuxeo.ecm.platform.filemanager.core:ecm-types-test-contrib.xml")
public class TestVersionning {

    protected DocumentModel destWS;

    protected DocumentModel wsRoot;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected FileManager fm;

    private void createTestDocuments() throws Exception {
        wsRoot = coreSession.getDocument(new PathRef(
                "default-domain/workspaces"));

        DocumentModel ws = coreSession.createDocumentModel(
                wsRoot.getPathAsString(), "ws1", "Workspace");
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

        Blob blob = new StringBlob("Something");
        blob.setMimeType("something");
        blob.setFilename("mytest.something");
        DocumentModel doc = fm.createDocumentFromBlob(coreSession, blob,
                destWS.getPathAsString(), true, "mytest.something");
        waitForAsyncCompletion();
        assertEquals("mytest.something", doc.getTitle());
        assertEquals("0.0", doc.getVersionLabel());

        doc = fm.createDocumentFromBlob(coreSession, blob,
                destWS.getPathAsString(), true, "mytest.something");
        waitForAsyncCompletion();
        assertEquals("0.1+", doc.getVersionLabel());

        blob.setFilename("mytest2.something");
        doc = fm.createDocumentFromBlob(coreSession, blob,
                destWS.getPathAsString(), true, "mytest2.something");
        waitForAsyncCompletion();
        assertEquals("0.0", doc.getVersionLabel());

        blob.setFilename("mytxt.txt");
        blob.setMimeType("text/plain");
        doc = fm.createDocumentFromBlob(coreSession, blob,
                destWS.getPathAsString(), true, "mytxt.txt");
        waitForAsyncCompletion();
        assertEquals("Note", doc.getType());
        assertEquals("0.0", doc.getVersionLabel());

        doc = fm.createDocumentFromBlob(coreSession, blob,
                destWS.getPathAsString(), true, "mytxt.txt");
        waitForAsyncCompletion();
        assertEquals("0.1+", doc.getVersionLabel());
    }

}
