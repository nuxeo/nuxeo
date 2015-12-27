/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.types.localconfiguration;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class LocalConfigurationRepositoryInit extends DefaultRepositoryInit {

    @Override
    public void populate(CoreSession session) {
        super.populate(session);

        DocumentModel doc = session.createDocumentModel("/default-domain/workspaces", "workspace", "Workspace");
        doc.setProperty("dublincore", "title", "workspace");
        doc = session.createDocument(doc);
        session.saveDocument(doc);

        doc = session.createDocumentModel("/default-domain/workspaces/workspace", "a-folder", "Folder");
        doc.setProperty("dublincore", "title", "a folder");
        doc = session.createDocument(doc);
        session.saveDocument(doc);

        doc = session.createDocumentModel("/default-domain/workspaces/workspace", "workspace2", "Workspace");
        doc.setProperty("dublincore", "title", "workspace 2");
        doc = session.createDocument(doc);
        session.saveDocument(doc);
    }

}
