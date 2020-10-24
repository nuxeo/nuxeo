/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.core.test;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.RepositoryInit;

/**
 * Default repository initializer that create the default DM doc hierarchy.
 */
public class DefaultRepositoryInit implements RepositoryInit {

    @Override
    public void populate(CoreSession session) {
        DocumentModel doc = session.createDocumentModel("/", "default-domain", "Domain");
        doc.setProperty("dublincore", "title", "Domain");
        session.createDocument(doc);

        doc = session.createDocumentModel("/default-domain/", "workspaces", "WorkspaceRoot");
        doc.setProperty("dublincore", "title", "Workspaces");
        session.createDocument(doc);

        doc = session.createDocumentModel("/default-domain/", "sections", "SectionRoot");
        doc.setProperty("dublincore", "title", "Workspaces");
        session.createDocument(doc);

        doc = session.createDocumentModel("/default-domain/", "templates", "TemplateRoot");
        doc.setProperty("dublincore", "title", "Templates");
        doc.setProperty("dublincore", "description", "Root of workspaces templates");
        session.createDocument(doc);

        doc = session.createDocumentModel("/default-domain/workspaces", "test", "Workspace");
        doc.setProperty("dublincore", "title", "workspace");
        session.createDocument(doc);
    }

}
