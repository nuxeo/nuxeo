/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;

/**
 * Default repository initializer that create the default DM doc hierarchy.
 */
public class DefaultRepositoryInit implements RepositoryInit {

    @Override
    public void populate(CoreSession session) throws ClientException {
        DocumentModel doc = session.createDocumentModel("/", "default-domain",
                "Domain");
        doc.setProperty("dublincore", "title", "Default domain");
        doc = session.createDocument(doc);
        session.saveDocument(doc);

        doc = session.createDocumentModel("/default-domain/", "workspaces",
                "WorkspaceRoot");
        doc.setProperty("dublincore", "title", "Workspaces");
        doc = session.createDocument(doc);
        session.saveDocument(doc);

        doc = session.createDocumentModel("/default-domain/", "sections",
                "SectionRoot");
        doc.setProperty("dublincore", "title", "Workspaces");
        doc = session.createDocument(doc);
        session.saveDocument(doc);

        doc = session.createDocumentModel("/default-domain/", "templates",
                "TemplateRoot");
        doc.setProperty("dublincore", "title", "Templates");
        doc.setProperty("dublincore", "description",
                "Root of workspaces templates");
        doc = session.createDocument(doc);
        session.saveDocument(doc);

        doc = session.createDocumentModel("/default-domain/workspaces", "test",
                "Workspace");
        doc.setProperty("dublincore", "title", "workspace");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
    }

}
