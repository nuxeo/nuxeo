/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     eugen
 */
package org.nuxeo.ecm.platform.filemanager;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 */
public class RepositoryInit extends DefaultRepositoryInit {

    public static final String PATH_WORKSPACE_ROOT = "/default-domain/workspaces";

    public static final String PATH_WORKSPACE = "/default-domain/workspaces/ws1";

    public static final String PATH_FOLDER = "/default-domain/workspaces/ws1/folder1";

    @Override
    public void populate(CoreSession session) {
        super.populate(session);

        DocumentModel doc = session.createDocumentModel(PATH_WORKSPACE_ROOT, "ws1", "Workspace");
        doc.setProperty("dublincore", "title", "workspace");
        doc = session.createDocument(doc);
        session.saveDocument(doc);

        doc = session.createDocumentModel(PATH_WORKSPACE, "folder1", "Folder");
        doc.setProperty("dublincore", "title", "a folder");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
    }

}
