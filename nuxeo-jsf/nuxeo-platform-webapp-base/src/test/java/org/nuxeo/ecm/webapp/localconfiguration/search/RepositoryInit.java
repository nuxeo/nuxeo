/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     eugen
 */
package org.nuxeo.ecm.webapp.localconfiguration.search;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * @author <a href="mailto:ei@nuxeo.com">Eugen Ionica</a>
 *
 */
public class RepositoryInit extends DefaultRepositoryInit{

    public static final String PATH_WORKSPACE_ROOT = "/default-domain/workspaces";

    public static final String PATH_WORKSPACE = "/default-domain/workspaces/ws1";

    public static final String PATH_FOLDER = "/default-domain/workspaces/ws1/folder1";

    public void populate(CoreSession session) throws ClientException {
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
