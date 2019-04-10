/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.webdav;

import java.io.File;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;

/**
 * @since 5.8
 */
public class WebDavRepoInit implements RepositoryInit {

    @Override
    public void populate(CoreSession session) {
        LogFactory.getLog(WebDavRepoInit.class).trace("enter populate webdav");
        DocumentModel ws = session.createDocumentModel("/", "workspaces", "WorkspaceRoot");
        ws.setPropertyValue("dc:title", "Workspaces");
        session.createDocument(ws);
        DocumentModel w = session.createDocumentModel("/workspaces", "workspace", "Workspace");
        w.setPropertyValue("dc:title", "Workspace");
        session.createDocument(w);

        createFile(w, "quality.jpg", "image/jpg", session);
        createFile(w, "test.html", "text/html", session);
        createFile(w, "test.txt", "text/plain", session);

        session.save();
    }

    protected void createFile(DocumentModel folder, String name, String mimeType, CoreSession session)
            {
        DocumentModel file = session.createDocumentModel(folder.getPathAsString(), name, "File");
        file.setProperty("dublincore", "title", name);
        String testDocsPath = Thread.currentThread().getContextClassLoader().getResource("testdocs").getPath();
        Blob fb = new FileBlob(new File(testDocsPath + "/" + name));
        fb.setMimeType(mimeType);
        fb.setFilename(name);
        file.setProperty("file", "content", fb);
        session.createDocument(file);
    }
}
