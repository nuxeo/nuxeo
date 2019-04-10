/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.webdav;

import java.io.File;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;


/**
 *
 *
 * @since 5.8
 */
public class WebDavRepoInit implements RepositoryInit{

    @Override
    public void populate(CoreSession session) throws ClientException {
        LogFactory.getLog(WebDavRepoInit.class).trace("enter populate webdav");
        DocumentModel ws = session.createDocumentModel("/", "workspaces",
                "WorkspaceRoot");
        ws.setPropertyValue("dc:title", "Workspaces");
        session.createDocument(ws);
        DocumentModel w = session.createDocumentModel("/workspaces",
                "workspace", "Workspace");
        w.setPropertyValue("dc:title", "Workspace");
        session.createDocument(w);

        createFile(w, "quality.jpg", "image/jpg",session);
        createFile(w, "test.html", "text/html",session);
        createFile(w, "test.txt", "text/plain",session);

        session.save();
    }

    protected void createFile(DocumentModel folder, String name, String mimeType, CoreSession session)
            throws ClientException {
        DocumentModel file = session.createDocumentModel(
                folder.getPathAsString(), name, "File");
        file.setProperty("dublincore", "title", name);
        String testDocsPath = Thread.currentThread().getContextClassLoader().getResource(
                "testdocs").getPath();
        Blob fb = new FileBlob(new File(testDocsPath + "/" + name));
        fb.setMimeType(mimeType);
        fb.setFilename(name);
        file.setProperty("file", "content", fb);
        session.createDocument(file);
    }
}
