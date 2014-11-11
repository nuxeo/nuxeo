/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.types.localconfiguration;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class LocalConfigurationRepositoryInit extends DefaultRepositoryInit {

    @Override
    public void populate(CoreSession session) throws ClientException {
        super.populate(session);

        DocumentModel doc = session.createDocumentModel("/default-domain/workspaces", "workspace",
                "Workspace");
        doc.setProperty("dublincore", "title", "workspace");
        doc = session.createDocument(doc);
        session.saveDocument(doc);

        doc = session.createDocumentModel("/default-domain/workspaces/workspace", "a-folder",
                "Folder");
        doc.setProperty("dublincore", "title", "a folder");
        doc = session.createDocument(doc);
        session.saveDocument(doc);

        doc = session.createDocumentModel("/default-domain/workspaces/workspace", "workspace2",
                "Workspace");
        doc.setProperty("dublincore", "title", "workspace 2");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
    }

}
